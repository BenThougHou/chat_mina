package com.yiliao.service.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yiliao.domain.MessageEntity;
import com.yiliao.domain.OnLineRes;
import com.yiliao.domain.Room;
import com.yiliao.domain.User;
import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushLinkUser;
import com.yiliao.evnet.PushMesgEvnet;
import com.yiliao.service.GoldComputeService;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.service.VideoChatService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.MidRes;
import com.yiliao.util.PoolThread;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.StopThread;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

/**
 * 视频聊天
 * 
 * @author Administrator
 */
@Service("videoService")
public class VideoChatServiceImpl extends ICommServiceImpl implements VideoChatService {


	private GoldComputeService goldComputeService = (GoldComputeService) SpringConfig.getInstance()
			.getBean("goldComputeService");
	private PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance()
			.getBean("personalCenterService");
	@Autowired
	RedisUtil redisUtil;

	@Override
	public MessageUtil getSpeedDatingRoom(int userId) {
		try {

//			if (RoomTimer.freeRooms.size() == 0) {
//				return new MessageUtil(0, "房间暂未生产.");
//			}
//			// 给用户分配房间号
			int roomId = this.getFinalDao().getIEntitySQLDAO().saveData("insert into t_room_time_log "
					+ "(t_anchor_id,t_create_time) values (?,now())",userId);
			Room rm = new Room(roomId);
			rm.setCreateTime(System.currentTimeMillis());
//			// 删除可用当前可用房间
//			RoomTimer.freeRooms.remove(0);
//
			RoomTimer.useRooms.put(rm.getRoomId(), rm);

			return new MessageUtil(1, roomId);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取速配房间号异常!", userId);
		}
		return null;
	}
	/**
	 * 用户发起聊天
	 */
	@Override
	public MessageUtil launchVideoChat(int launchUserId, int coverLinkUserId,int roomId ,int chatType) {
		MessageUtil mu = null;
		try {
			HashMap<String,Object> hashMap = new HashMap<String, Object>();
			logger.info("---进入了用户呼叫主播流程---");
			// 销毁用户所有存在的房间信息 并把房间返回房间池
			userHangupLink(launchUserId);

			if (launchUserId == coverLinkUserId) {
				return new MessageUtil(-7, "不能和自己进行聊天.");
			}
			
//			if(UserIoSession.getInstance().getMapIoSession(launchUserId)==null||!UserIoSession.getInstance().getMapIoSession(launchUserId).isActive()) {
//				return new MessageUtil(-10, "登录状态异常,请重新登录.");
//			};
			// 获取用户的所有金币
			String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql,
					launchUserId);

			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold,t_voice_gold FROM t_anchor_setup WHERE t_user_id = ?";

			List<Map<String, Object>> sqlList = this.getQuerySqlList(videoGoldSql, coverLinkUserId);

			if (sqlList.isEmpty()) {

				return new MessageUtil(-6, "主播资料未完善!无法发起聊天.");
			}

			//获取用户余额
			Map<String, Object> map = getMap("SELECT t_fail_gold  FROM t_system_setup");
			
			logger.info("系统设置的金币->{}",map.toString());
			logger.info("用户的金币数->{}",totalGold.toString());
			logger.info("比对结果->{}",new BigDecimal(map.get("t_fail_gold").toString()).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) <= 0);
			// 判断用户的金币是否满足聊天计时
			String content="";
			int mid=0;
			if(chatType==1||chatType==3||chatType==5) {
				
				if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
						.compareTo(new BigDecimal(sqlList.get(0).get("t_video_gold").toString())) < 0 || 
						new BigDecimal(map.get("t_fail_gold").toString()).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0) {
					if(!checkSvipUser(launchUserId,  coverLinkUserId)) {
						return new MessageUtil(-4, "余额不足!请充值.");
					}
				}
				content="邀请您进行视频聊天!";
				mid=Mid.onLineRes;
				
			}else if(chatType==2||chatType==4||chatType==6) {
				if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
						.compareTo(new BigDecimal(sqlList.get(0).get("t_voice_gold").toString())) < 0 || 
						new BigDecimal(map.get("t_fail_gold").toString()).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0) {
					if(!checkSvipUser(launchUserId,  coverLinkUserId)) {
						return new MessageUtil(-4, "余额不足!请充值.");
					}
				}
				content="邀请您进行语音聊天!";
				mid=Mid.onLineToVoiceRes;
			}
			
			
			

			// 验证发起人和被链接人是否是同性别
//			if (sameSex(launchUserId, coverLinkUserId)) {
//				return new MessageUtil(-5, "同性别无法进行聊天!");
//			}
			// 查询当前用户呼叫的主播是否是虚拟主播
			String qSql = " SELECT * FROM t_virtual WHERE t_user_id = ? ";

			List<Map<String, Object>> virList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql,
					coverLinkUserId);
			// 如果被呼叫的用户不是虚拟主播 那么走正常流程 否则让用户直接呼叫
			if (null == virList || virList.isEmpty()) {

				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(coverLinkUserId);

				// 获取主播是否正在连线中
				if (RoomTimer.getUserIsBusy(coverLinkUserId)|| VideoTiming.getUserExist(coverLinkUserId)) {

					return new MessageUtil(-2, "您拨打的用户正忙,请稍后再拨.");
				}

				// 获取主播是否正在大房间直播中
				List<Map<String, Object>> list = this
						.getQuerySqlList("SELECT t_is_debut FROM t_big_room_man WHERE t_user_id = ? ", coverLinkUserId);

				if (null != list && !list.isEmpty() && "1".equals(list.get(0).get("t_is_debut").toString())) {
					return new MessageUtil(-2, "您拨打的用户正忙,请稍后再拨.");
				}

				// 判断主播是否设置为勿扰
//				String sql = "SELECT * FROM t_user WHERE t_id = ? AND t_is_not_disturb = 1";
//				List<Map<String, Object>> users = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql,
//						coverLinkUserId);
//
//				if (null != users && !users.isEmpty()) {
//					return new MessageUtil(-3, "Sorry,对方设置了勿扰.");
//				}
				// 把链接人
				// 记录用户的链接信息
				saveCallLog(launchUserId, coverLinkUserId, roomId);
				//将通话记录表主键ID作为唯一房间号
//				roomId=callLogId;
				Room room = new Room(roomId);
				this.executeSQL("update t_room_time_log set t_chatType=?,t_call_time=now(),t_conversation_type=? where t_id=?", chatType,1,roomId);
				room.setLaunchUserId(launchUserId);
				room.setCoverLinkUserId(coverLinkUserId);
				room.setCallUserId(launchUserId);
				room.setConversationType(1);
				// 写入到通话记录中

				room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr") + room.getLaunchUserId() + "/" + roomId);
				room.setCoverLinkUserLiveCode(
						SystemConfig.getValue("play_addr") + room.getCoverLinkUserId() + "/" + roomId);
				room.setChatType(chatType);
				RoomTimer.useRooms.put(roomId, room);
				//
				OnLineRes or = new OnLineRes();
				or.setMid(mid);
				or.setRoomId(roomId);
				or.setConnectUserId(launchUserId);
				// 給被呼叫方推送 連接信息
				if (null != ioSession) {
					 JSONObject fromObject = JSONObject.fromObject(or);
					 fromObject.put("chatType", chatType);
					ioSession.write(fromObject.toString());
				}

				// 获取链接人的昵称
				Map<String, Object> userMap = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ?", launchUserId);
				
				//开始呼叫，设置用户 主播 忙碌状态 t_state
				this.executeSQL("update t_user set t_onLine = ? where t_id=? and t_onLine = 0",User.ONLINE_BE_BUSY, launchUserId);
				this.executeSQL("update t_home_table set t_onLine = ? where t_id=? and  t_onLine = 0",User.ONLINE_BE_BUSY, coverLinkUserId);
				this.executeSQL("update t_user set t_onLine = ? where t_id=? and  t_onLine = 0",User.ONLINE_BE_BUSY, coverLinkUserId);
				//用户呼叫主播 加入缓存 由此判断 VIP用户接听
//				redisUtil.setTime("VIP_USER_"+launchUserId+"_"+coverLinkUserId, "1",35000l);
				redisUtil.remove("VIP_USER_"+coverLinkUserId+"_"+launchUserId);
				this.applicationContext.publishEvent(new PushMesgEvnet(new MessageEntity(coverLinkUserId,
						userMap.get("t_nickName") + content, 0, new Date(), 6, roomId, launchUserId, 0)));
			} else {

				// 查询当前虚拟主播是否在忙碌或者离线状态
				qSql = " SELECT t_onLine FROM t_user WHERE t_id = ? ";

				Map<String, Object> viaMap = this.getMap(qSql,coverLinkUserId);
				
				if ("1".equals(viaMap.get("t_onLine").toString())) {
					return new MessageUtil(-2, "您拨打的用户正忙,请稍后再拨.");
				} else if ("2".equals(viaMap.get("t_onLine").toString())) {
					return new MessageUtil(-1, "对方不在线!");
				}
				saveCallLog(launchUserId, coverLinkUserId, roomId);
				//将通话记录表主键ID作为唯一房间号
				Room room = new Room(roomId);
				this.executeSQL("update t_room_time_log set t_chatType=?,t_call_time=now(),t_conversation_type=? where t_id=?", chatType,1,roomId);
				room.setLaunchUserId(launchUserId);
				room.setCoverLinkUserId(coverLinkUserId);
				room.setCallUserId(launchUserId);
				room.setConversationType(1);
				// 写入到通话记录中
				room.setChatType(chatType);
				RoomTimer.useRooms.put(roomId, room);
			}
//			hashMap.put("roomId", roomId);
			mu = new MessageUtil(1, "正在链接.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("发起视频聊天异常!当前链接人【{}】,被链接人【{}】", launchUserId, coverLinkUserId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}
	
	/**
	 * 开始计时
	 */
	@Override
	public MessageUtil videoCharBeginTiming(int anthorId, int userId, int roomId,int chatType) {
		MessageUtil mu = null;
		try {

			logger.info("[{}]房间开始计时,anthorId->{},userId->{}", roomId, anthorId, userId);
			
			String string = redisUtil.get("videoCharBeginTiming_"+roomId);
			
			if(StringUtils.isNotBlank(string)) {
				return new MessageUtil(1, "挂断成功!");
			}else {
				redisUtil.setTime("videoCharBeginTiming_"+roomId, "breakLink_"+roomId,3000l);
			}
			
			
			// 获取用户是否正在计时当中
			if (VideoTiming.timingUser.containsKey(roomId)) {
				// 打印当前计时信息
				logger.info("当前用户的计时信息为-->{}", JSONObject.fromObject(VideoTiming.timingUser.get(roomId)).toString());
				// 判断当前用户的计时信息 是否和最新需要计时的信息一致
				Map<String, Integer> map = VideoTiming.timingUser.get(roomId);
				// 判断当前计时中的主播和房间号是否一样
				// 如果一致,表示重复调用计时 不用执行了
				if (map!=null ) {
					return new MessageUtil(1, "当前用户正在计时中.");
				} else { // 否则 调用挂断链接 程序继续执行
					return new MessageUtil(-5, "当前用户正在计时中.");
//					this.breakLink(map.get("roomId"), 6, userId);
				}
			}
			
			// 获取用户的所有金币
			String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql,
					userId);
			
			//获取用户余额
			Map<String, Object> sysmap = getMap("SELECT t_fail_gold  FROM t_system_setup");
			String depleteGole="";
			if (null == totalGold.get("totalGold")	|| new BigDecimal(0).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0) {
				logger.info("{}用户余额为0或者负数", userId);
				if(!checkSvipUser(userId,  anthorId)) {
					return new MessageUtil(-1, "余额不足!请充值.");
				}
			} 
			
			// 判断房间是否还存在
			Room room = RoomTimer.useRooms.get(roomId);
			
//			if (null == room) {
//				room = VideoTiming.getRoom(roomId);
//			}

			{
				// 判断用户余额是否住够聊天
				// 获取被链接人每分钟需要消耗多少金币
				String videoGoldSql = "SELECT t_video_gold,t_voice_gold FROM t_anchor_setup WHERE t_user_id = ?";
				List<Map<String, Object>> sqlList = this.getQuerySqlList(videoGoldSql, anthorId);
				depleteGole=sqlList.get(0).get("t_video_gold").toString();
				if(chatType==1||chatType==3||chatType==5) {
					if (new BigDecimal(sqlList.get(0).get("t_video_gold").toString())
							.compareTo(new BigDecimal(totalGold.get("totalGold").toString())) == 1 
							|| new BigDecimal(sysmap.get("t_fail_gold").toString()).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0 ) {
						logger.info("用户余额-->{}", totalGold.get("totalGold").toString());
						if(!checkSvipUser(userId,  anthorId)||room.getConversationType()==2) {
							return new MessageUtil(-1, "余额不足!请充值.");
						}
					}
				}else if(chatType==2||chatType==4||chatType==6) {
					if (new BigDecimal(sqlList.get(0).get("t_voice_gold").toString())
							.compareTo(new BigDecimal(totalGold.get("totalGold").toString())) == 1 
							|| new BigDecimal(sysmap.get("t_fail_gold").toString()).compareTo(new BigDecimal(totalGold.get("totalGold").toString())) >= 0 ) {
						logger.info("用户余额-->{}", totalGold.get("totalGold").toString());
						if(!checkSvipUser(userId,  anthorId)||room.getConversationType()==2) {
							return new MessageUtil(-1, "余额不足!请充值.");
						}
					}
					depleteGole=sqlList.get(0).get("t_voice_gold").toString();
				}
			}
			if(UserIoSession.getInstance().getMapIoSession(anthorId)==null||UserIoSession.getInstance().getMapIoSession(userId)==null) {
				return new MessageUtil(-5, "对方用户已下线");
			};
			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold,t_voice_gold FROM t_anchor_setup WHERE t_user_id = ?";

			List<Map<String, Object>> sqlList = this.getQuerySqlList(videoGoldSql, anthorId);

			if (sqlList.isEmpty()) {
				return new MessageUtil(-2, "主播资料未完善!");
			}

			if (null == room) {
				return new MessageUtil(-5, "对方已挂断");
			}
			
			
			// 判断当前房间是否已经有人了
			if (0 != room.getLaunchUserId() && room.getLaunchUserId() != userId) {
				return new MessageUtil(-4, "啊！被别人抢走了,下次手速要快哦.");
			} else if (room.getLaunchUserId() == 0) {
				// 用户加入速配房间
				room.setLaunchUserId(userId);
				room.setCallUserId(userId);
				logger.info("房间中的主播{}",room.getCoverLinkUserId());
				room.setCoverLinkUserId(anthorId);
//				room.setCoverName(coverName);
				// 写入到通话记录中
				saveCallLog(userId, anthorId, roomId);

				room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr") + room.getLaunchUserId() + "/" + roomId);
				room.setCoverLinkUserLiveCode(SystemConfig.getValue("play_addr") + room.getCoverLinkUserId() + "/" + roomId);
			}
			
			logger.info("当前主播-->{}",anthorId);
			// 房间已销毁
			if (room.getCoverLinkUserId() != anthorId || room.getLaunchUserId() != userId) {
				logger.info("{}房间已经销毁.{}当前主播，{}用户", roomId,room.getCoverLinkUserId());
				return new MessageUtil(-3, "对方挂断视频请求.");
			}

			logger.info("{}用户余额{}", userId, new BigDecimal(totalGold.get("totalGold").toString()).intValue());
			logger.info("{}主播每分钟视频收费{},语音收费{}", anthorId,
					new BigDecimal(sqlList.get(0).get("t_video_gold").toString()).intValue(),new BigDecimal(sqlList.get(0).get("t_voice_gold").toString()).intValue());
		
			// 加入到计时器中
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put("gold", new BigDecimal(totalGold.get("totalGold").toString()).intValue());
			map.put("deplete", new BigDecimal(depleteGole).intValue());
			map.put("timing", 0);
			map.put("roomId", roomId); // 房间号
			map.put("userId", userId); // 主播编号
			map.put("anthorId", anthorId); // 主播编号
			map.put("chatType", chatType);
			if(room.getConversationType()==0) {
				room.setConversationType(1);
			}
			map.put("conversationType", room.getConversationType());
			logger.info("{}房间开始计时,anthorId->{},userId->{},chatType->{},加入到计时系统开始", roomId, anthorId, userId,chatType);

			VideoTiming.timingUser.put(roomId, map);

			logger.info("{}房间开始计时,anthorId->{},userId->{},加入到计时系统完成", roomId, anthorId, userId);

			// 链接信息写入到数据库中
			this.executeSQL(
					"INSERT INTO t_room_time (t_room_id, t_call_user_id, t_answer_user_id,t_an_vi_gold,t_chatType, t_create_time) VALUES (?, ?, ?,?, ?, ?);",
					roomId, userId, anthorId, map.get("deplete"),chatType,
					DateUtils.format(new Date(), DateUtils.FullDatePattern));

			// 修改主播状态为忙碌
			this.executeSQL("UPDATE t_user SET t_onLine = ? WHERE  t_id = ?;", User.ONLINE_BE_BUSY,anthorId);
			// 修改home_table 表中在线状态为忙碌
			this.executeSQL("UPDATE t_home_table SET t_onLine = ?  WHERE t_id = ? ", User.ONLINE_BE_BUSY , anthorId);
			
			//接通之后需要销毁同时呼叫的其他用户
			this.executeSQL("update t_room_time_log set t_anchor_gold=?,t_answer_time=now(),t_user_id=? where t_id=?", depleteGole,userId,roomId);
			 
			/*********** 监控程序 ***********/
			if (null != room) {
				// 异步通知
				this.applicationContext.publishEvent(new PushLinkUser(RoomTimer.useRooms.get(roomId)));
			}
			/***************************/

			/********* 推送视频提示语 ***********/
			// 链接人
			// 获取提示信息
			List<Map<String, Object>> tipsList = this.getQuerySqlList("SELECT t_video_hint FROM t_system_setup ");
			// 如果后台填写了 提示信息
			if (!tipsList.isEmpty()) {
				// 链接人session
				IoSession callUserIoSession = UserIoSession.getInstance().getMapIoSession(room.getLaunchUserId());
				if (null != callUserIoSession && !callUserIoSession.isClosing()) {
					// 推送 提示信息
					callUserIoSession.write(JSONObject.fromObject(new HashMap<String, Object>() {
						private static final long serialVersionUID = 1L;
						{
							put("mid", Mid.sendVideoTipsMsg);
							put("msgContent", tipsList.get(0).get("t_video_hint"));
						}
					}).toString());
				}
				// 被链接人 session
				IoSession coverUserIoSession = UserIoSession.getInstance().getMapIoSession(room.getCoverLinkUserId());
				if (null != coverUserIoSession && !coverUserIoSession.isClosing()) {
					coverUserIoSession.write(JSONObject.fromObject(new HashMap<String, Object>() {
						private static final long serialVersionUID = 1L;
						{
							put("mid", Mid.sendVideoTipsMsg);
							put("msgContent", tipsList.get(0).get("t_video_hint"));
						}
					}).toString());
				}
			}

			/******************************/

			mu = new MessageUtil(1, "开始计时");

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("计时异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/*
	 * 用户挂断链接(non-Javadoc)
	 * 
	 * @see com.yiliao.service.VideoChatService#breakLink(int, int)
	 */
	@Override
	public MessageUtil breakLink(int roomId, int type, int breakUserId) {
		MessageUtil mu = null;
		try {
			logger.info(
					"用户{}主动挂断->{},开始结算.,挂断类型->{},1.用户主动挂断链接,"
							+ "2.用户断开链接或者发起呼叫断开链接,3.用户违规,4.socket断开链接,5.计费时间不足挂断链接,"
							+ "6.发起新的计时(上次挂断失败),"
							+ "7.自动呼叫30秒倒计时挂断,"
							+ "8.后台统一挂断",
					breakUserId, roomId, type);
			/* 获取房间信息 */
			//使用redis控制用户频繁调用问题 
			String string = redisUtil.get("breakLink_"+roomId);
			
			if(StringUtils.isNotBlank(string)) {
				return new MessageUtil(1, "挂断成功!");
			}else {
				redisUtil.setTime("breakLink_"+roomId, "breakLink_"+roomId,3000l);
			}

			Room room = RoomTimer.useRooms.get(roomId);

			if (null == room || 0 == room.getLaunchUserId() && 0 == room.getCoverLinkUserId()) {
				room = VideoTiming.getRoom(roomId);
			}
			int payType=WalletDetail.CHANGE_CATEGORY_VIDEO;
			//通话分钟数
			int time=0;
			//本次通话消费金币
			int t_room_gold=0;
			
			if (null != room && 0 != room.getLaunchUserId() && 0 != room.getCoverLinkUserId()) {
				int chatType=room.getChatType();
				//1:用户呼叫主播 2:主播呼叫用户
				int conversationType = room.getConversationType();
				logger.info("当前房间号-->{}-getLaunchUserId->{}-getCoverLinkUserId->{}-chatType->{}",roomId,room.getLaunchUserId(),room.getCoverLinkUserId(),chatType);
				
				int mid_userId=Mid.brokenLineRes;
				int mid_ano=Mid.brokenLineRes;
				//1:视频 2:语音
				int IMType=1;
				if(chatType==2||chatType==4||chatType==6) {
					mid_userId=Mid.brokenVoiceLineRes;
					mid_ano=Mid.brokenVoiceLineRes;
					payType=WalletDetail.CHANGE_CATEGORY_VOICE;
					IMType=2;
				}
//				int chatType =1;
				// 1:视频  2:语音 
//				主播呼用户     视频时：无资费
//				chatType：3 视频 4：语音
//				用户呼主播
//				chatType：5 视频 6：语音
//				1.不是本人主动挂断 ！= 主播 140 用户 229
//				2.breakUserId!=room.getCoverLinkUserId()&&chatType==3&&(null == map|| map.isEmpty()||type==7
				Map<String, Integer> map = VideoTiming.timingUser.get(roomId);
				// 
				if(chatType==3&&!(breakUserId==room.getCoverLinkUserId()&&type!=7)&&(null == map|| map.isEmpty())) {
					//主播等于主播 != 7  或者 内存不为空 或者  type 
					mid_ano=Mid.brokenVIPLineRes;
				}//							140
				else if(chatType==4&&!(breakUserId==room.getCoverLinkUserId()&&type!=7)&&(null == map|| map.isEmpty())) {
					mid_ano=Mid.brokenUserVoiceLineRes;
				}
				else if(chatType==5&&!(breakUserId==room.getLaunchUserId()&&type!=7)&&(null == map|| map.isEmpty())) {
					mid_userId=Mid.brokenVIPLineRes;
				}//							 140
				else if(chatType==6&&!(breakUserId==room.getLaunchUserId()&&type!=7)&&(null == map|| map.isEmpty())) {
					mid_userId=Mid.brokenUserVoiceLineRes;
				}
				
				logger.info("获取内存中的的计费信息-{}", map);
				if (null != map && !map.isEmpty()) {
					// 根据房间号 删除数据
					this.executeSQL("DELETE FROM t_room_time WHERE t_room_id = ?", roomId);
					// 清理要挂断信息的key
					if (!VideoTiming.arr.isEmpty() && VideoTiming.arr.indexOf(roomId) >= 0)
						VideoTiming.arr.remove(VideoTiming.arr.indexOf(roomId));

					// 删除正在计时的用户数据
					VideoTiming.timingUser.remove(roomId);

					// 计算本次链接链接分钟数
					time = map.get("timing") % 60 == 0 ? map.get("timing") / 60 : map.get("timing") / 60 + 1;
					int svipTime=time;
					// TODO: 2020/4/15 1.判断呼叫用户是否是svip 2.判断主播是否还有免费呼叫次数 3.减去免费分钟数

					
					// 呼叫方式是视频 & 聊天发起方式是用户呼叫主播
					String sql="select t_is_vip,t_is_svip from t_user where t_id=?";
					Map<String, Object> userMap = this.getMap(sql,room.getLaunchUserId());
					try {
						if(userMap.get("t_is_svip").toString().equals("0")&&room.getConversationType()==1) {
							// 用户是svip && 主播的接听免费次数 > 0
							String free_svip = redisUtil.get("free_svip_number_"+room.getCoverLinkUserId());
							
							//用户设置5次
							 Map<String, String> free_user_svip_map = redisUtil.hmget("free_user_svip_number_"+room.getLaunchUserId());
							 
							 String free_user_svip ="0";
							 String free_user_to_anchor ="";
							if(StringUtils.isBlank(free_svip)) {
								free_svip="0";
							}
							if(free_user_svip_map!=null
									&&StringUtils.isNotBlank(free_user_svip_map.get("free_user_svip"))
									&&StringUtils.isNotBlank(free_user_svip_map.get("free_user_to_anchor"))) {
								free_user_svip=free_user_svip_map.get("free_user_svip");
								free_user_to_anchor=free_user_svip_map.get("free_user_to_anchor");
							}
							
							int free_svip_number= Integer.parseInt(free_svip);
							int free_user_svip_number= Integer.parseInt(free_user_svip);
							if (free_svip_number > 0&&free_user_svip_number<5&&!free_user_to_anchor.contains(room.getCoverLinkUserId()+"")){
								redisUtil.setTime("free_svip_number_"+room.getCoverLinkUserId(),free_svip_number - 1+"", DateUtils.millis(1));
								
								//设置用户的key
								HashMap<String,String> hashMap = new HashMap<String,String>();
								free_user_to_anchor= free_user_to_anchor+","+room.getCoverLinkUserId();
								hashMap.put("free_user_svip", free_user_svip_number + 1+"");
								hashMap.put("free_user_to_anchor", free_user_to_anchor);
								redisUtil.hmset("free_user_svip_number_"+room.getLaunchUserId(),hashMap );
								redisUtil.expireMillTime("free_user_svip_number_"+room.getLaunchUserId(),DateUtils.millis(1));
								
								// 扣除免费的分钟数
								logger.info("SVIP用户{},免费1分钟之后时间{},用户redis对象{}",room.getLaunchUserId(),svipTime,hashMap.toString());
								svipTime-=1;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					// 计算本次链接一共消耗了多少金币
					int gold = map.get("deplete") * svipTime;
					t_room_gold=gold;
					logger.info("[{}]房间挂断链接,当前计时分钟数->{},消耗金币->{}", roomId, time, gold);
					// 如果当前消费金币数大于了用户的余额
					// 那么减少1分钟的计费
					if (gold > map.get("gold")) {
						gold = (map.get("timing") / 60) * map.get("deplete");
						time=(map.get("timing") / 60);
					}
					// 保存消费记录
					int orderId = this.saveOrder(room.getLaunchUserId(), room.getCoverLinkUserId(), 0,
							payType, new BigDecimal(gold), roomId, time);
					// 扣除用户金币
					if (goldComputeService.userConsume(room.getLaunchUserId(), payType,
							new BigDecimal(gold), orderId)) {
						// 分配用户的消费的金币
						goldComputeService.distribution(new BigDecimal(gold), room.getLaunchUserId(),
								room.getCoverLinkUserId(), payType, orderId);
					} else {
						logger.info("--当前用户{}在房间号{}中和{}聊天{}秒扣费异常,其中用户余额{}，主播每分钟消耗{}--", room.getLaunchUserId(), roomId,
								room.getCoverLinkUserId(), map.get("timing"), map.get("gold"), map.get("deplete"));
					}

					logger.info("当前房间信息-->{}", JSONObject.fromObject(room).toString());
					// 把通话时间写入到通话记录中
//					if (room.getCallUserId() == room.getLaunchUserId()) {
						updateCallLog( roomId, time);
//					} else {
//						updateCallLog(room.getCoverLinkUserId(), room.getLaunchUserId(), roomId, time);
//					}
					receptionRate(room.getCoverLinkUserId(), 1);
					
					//有计费 发送语音或者视频
					Map<String,Object>  msgMap=new HashMap<String,Object>();
					if(IMType==1) {
						//视频
						msgMap.put("call_type", "video");
						msgMap.put("desc", "[视频邀请]");
					}else {
						//语音
						msgMap.put("call_type", "voice");
						msgMap.put("desc", "[语音邀请]");
					}
					msgMap.put("type", "video_connect");
					msgMap.put("video_time", time);
					int sendIMUser=0;
					int sendedIMUser=0;
					if(conversationType==1) {
						//用户呼叫主播
						sendIMUser=room.getLaunchUserId();
						sendedIMUser=room.getCoverLinkUserId();
					}else if (conversationType==2) {
						sendIMUser=room.getCoverLinkUserId();
						sendedIMUser=room.getLaunchUserId();
					}
					personalCenterService.sendTXUserVoideIM(sendIMUser,sendedIMUser,msgMap);
					//数据汇总到表中
					new PoolThread(PoolThread.data_query_video_chat, orderId).start();
				} else { // 数据库中读取数据
					List<Map<String, Object>> sqlList = this
							.getQuerySqlList("SELECT * FROM t_room_time WHERE t_room_id = ?", roomId);
					if (sqlList.isEmpty()) {
						logger.info("---{}房间无法获取任何信息--", roomId);
						// 把通话时间写入到通话记录中
//						if (room.getCallUserId() == room.getLaunchUserId()) {
							updateCallLog( roomId, 0);
//						} else {
//							updateCallLog(room.getCoverLinkUserId(), room.getLaunchUserId(), roomId, 0);
//						}
						mu = new MessageUtil(1, "房间已销毁!");
						//无计费 发送语音或者视频
						Map<String,Object>  msgMap=new HashMap<String,Object>();
						if(IMType==1) {
							//视频
							msgMap.put("call_type", "video");
							msgMap.put("desc", "[视频邀请]");
						}else {
							//语音
							msgMap.put("call_type", "voice");
							msgMap.put("desc", "[语音邀请]");
						}
						
						int sendIMUser=0;
						int sendedIMUser=0;
						logger.info("房间信息{}"+room);
						if(conversationType==1) {
							//用户呼叫主播
							msgMap.put("type", "video_unconnect_user");
							sendIMUser=room.getLaunchUserId();
							sendedIMUser=room.getCoverLinkUserId();
						}else if(conversationType==2) {
							//主播呼叫用户
							sendIMUser=room.getCoverLinkUserId();
							sendedIMUser=room.getLaunchUserId();
							msgMap.put("type", "video_unconnect_anchor");
							Map<String, Object> anochorMap = this.getMap("select ROUND(t_video_gold) t_video_gold,ROUND(t_voice_gold) t_voice_gold from t_anchor_setup where  t_user_id=?", room.getCoverLinkUserId());
							msgMap.put("video_price",  anochorMap.get("t_video_gold"));
							msgMap.put("voice_price",  anochorMap.get("t_voice_gold"));
							msgMap.put("anchor_id",  room.getCoverLinkUserId());
						}
						personalCenterService.sendTXUserVoideIM(sendIMUser,sendedIMUser,msgMap);
						
					} else {
						// 根据房间号 删除数据
						this.executeSQL("DELETE FROM t_room_time WHERE t_room_id = ?", roomId);
						
						BigDecimal _video_gold = new BigDecimal(sqlList.get(0).get("t_an_vi_gold").toString());

						int userId = Integer.parseInt(sqlList.get(0).get("t_call_user_id").toString());
						int anchorId = Integer.parseInt(sqlList.get(0).get("t_answer_user_id").toString());

						// 开始时间
						long begin_time = DateUtils
								.parse(sqlList.get(0).get("t_create_time").toString(), DateUtils.FullDatePattern)
								.getTime();
						// 得到时间差
						Long time_difference = (System.currentTimeMillis() - begin_time) / 1000;
						// 计算本次链接链接分钟数
						 time = time_difference.intValue() % 60 == 0 ? time_difference.intValue() / 60
								: time_difference.intValue() / 60 + 1;
						// 计算本次链接一共消耗了多少金币
						BigDecimal gold = _video_gold.multiply(new BigDecimal(time)).setScale(2, BigDecimal.ROUND_DOWN);
						t_room_gold=gold.intValue();
						logger.info("[{}]房间查询sql挂断链接,当前计时分钟数->{},消耗金币->{}", sqlList.get(0).get("t_room_id"), time,
								gold);
						// 获取用户的真实余额
						String sql = "SELECT u.t_id,u.t_sex,u.t_nickName,u.t_role,u.t_referee,b.t_recharge_money,b.t_profit_money,b.t_share_money FROM t_user u LEFT JOIN t_balance b ON b.t_user_id = u.t_id WHERE u.t_id = ?";

						List<Map<String, Object>> data = getQuerySqlList(sql, userId);

						BigDecimal totalGold = new BigDecimal(data.get(0).get("t_recharge_money").toString())
								.add(new BigDecimal(data.get(0).get("t_profit_money").toString()))
								.add(new BigDecimal(data.get(0).get("t_share_money").toString()));

						if (totalGold.compareTo(gold) < 0) {
							gold = _video_gold.multiply(new BigDecimal(time_difference.intValue() / 60)).setScale(2,
									BigDecimal.ROUND_DOWN);
							// 如果用户的金币还是小于聊天分钟数
							// 那么扣除用户的所有金币
							if (totalGold.compareTo(gold) < 0) {
								gold = totalGold;
							}
						}
						// 保存消费记录
						int orderId = this.saveOrder(userId, anchorId, 0, payType, gold,
								roomId, time);
						// 扣除用户金币
						goldComputeService.userConsume(userId, payType, gold, orderId);
						// 分配用户的消费的金币
						goldComputeService.distribution(gold, userId, anchorId, payType,
								orderId);

						// 把通话时间写入到通话记录中
//						if (userId == Integer.parseInt(sqlList.get(0).get("t_call_user_id").toString())) {
							updateCallLog( roomId, time);
//						} else {
//							updateCallLog(anchorId, userId, roomId, time);
//						}
						receptionRate(anchorId, 1);
						//有计费 发送语音或者视频
						Map<String,Object>  msgMap=new HashMap<String,Object>();
						if(IMType==1) {
							//视频
							msgMap.put("call_type", "video");
							msgMap.put("desc", "[视频邀请]");
						}else {
							//语音l
							msgMap.put("call_type", "voice");
							msgMap.put("desc", "[语音邀请]");
						}
						int sendIMUser=0;
						int sendedIMUser=0;
						if(conversationType==1) {
							//用户呼叫主播
							sendIMUser=room.getLaunchUserId();
							sendedIMUser=room.getCoverLinkUserId();
						}else if(conversationType==2) {
							sendIMUser=room.getCoverLinkUserId();
							sendedIMUser=room.getLaunchUserId();
						}
						
						msgMap.put("type", "video_connect");
						msgMap.put("video_time", time);
						personalCenterService.sendTXUserVoideIM(sendIMUser,sendedIMUser,msgMap);
						//数据汇总到表中
						new PoolThread(PoolThread.data_query_video_chat, orderId).start();
					}
				}
				//通知监控服务器 用户已断开连接
				new  StopThread(roomId).start();
				// 在正在使用的房间列表中删除数据
				RoomTimer.useRooms.remove(roomId);
				this.executeSQL("update t_user set t_onLine = ? where t_id=? and  t_onLine != 2",User.ONLINE_IDLE, room.getCoverLinkUserId());
				this.executeSQL("update t_user set t_onLine = ?  where t_id=? and  t_onLine != 2",User.ONLINE_IDLE, room.getLaunchUserId());
				this.executeSQL("update t_home_table set t_onLine = ? where t_id=? and  t_onLine != 2",User.ONLINE_IDLE,  room.getCoverLinkUserId());
				
				this.executeSQL("update t_room_time_log set t_room_time=?,t_room_gold=?,t_break_type=?,t_end_time=now() where t_id=?", time,t_room_gold,type,roomId);
				
				// 给链接用户推送挂断信息
				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(room.getLaunchUserId());
				logger.info("用户{}的session->{}->{}", room.getLaunchUserId(),ioSession,mid_userId);
				if (null != ioSession) {
					JSONObject fromObject = JSONObject.fromObject(new MidRes(mid_userId));
					fromObject.put("breakUserId", breakUserId);
					fromObject.put("roomId", roomId);
					ioSession.write(fromObject.toString());
				}
				// 给被链接人推送挂断信息
				IoSession coverIoSession = UserIoSession.getInstance().getMapIoSession(room.getCoverLinkUserId());
				logger.info("主播{}的session->{}->{}", room.getCoverLinkUserId(),coverIoSession,mid_ano);
				if (null != coverIoSession) {
					JSONObject fromObject = JSONObject.fromObject(new MidRes(mid_ano));
					fromObject.put("breakUserId", breakUserId);
					fromObject.put("roomId", roomId);
					coverIoSession.write(fromObject.toString());
				}
			}
			
			mu = new MessageUtil(1, "挂断成功!");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("挂断链接异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 用户编号
	 * 
	 * @param userId
	 * @param type   1.接通2.未接听
	 */
	private void receptionRate(int userId, int type) {

		// 查询该主播是否存在数据
		String querySql = "SELECT * FROM t_reception_rate WHERE t_user_id = ?";
		List<Map<String, Object>> dataList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(querySql, userId);
		// 新增数据
		if (null == dataList || dataList.isEmpty()) {

			String inSql = "INSERT t_reception_rate (t_user_id, t_count, t_reception_count, t_refuse_count, t_reception) VALUES (?, ?, ?, ?, ?);";

			this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId, 1, type == 1 ? 1 : 0, type == 1 ? 0 : 1,
					type == 1 ? 100 : 0);
		} else {

			int count = Integer.parseInt(dataList.get(0).get("t_count").toString()) + 1;

			int t_reception_count = Integer.parseInt(dataList.get(0).get("t_reception_count").toString())
					+ (type == 1 ? 1 : 0);

			int t_refuse_count = Integer.parseInt(dataList.get(0).get("t_refuse_count").toString())
					+ (type == 1 ? 0 : 1);

			// 修改数据
			String upSql = "UPDATE t_reception_rate SET t_count=?, t_reception_count=?, t_refuse_count=?, t_reception=? WHERE t_id=?;";

			this.executeSQL(upSql, count, t_reception_count, t_refuse_count,
					calculationPercent(count, t_reception_count),
					Integer.parseInt(dataList.get(0).get("t_id").toString()));
		}

	}

	/**
	 * 计算百分比
	 * 
	 * @param total
	 * @param number
	 * @return
	 */
	public String calculationPercent(int total, int number) {

		NumberFormat numberFormat = NumberFormat.getInstance();

		// 设置精确到小数点后2位
		numberFormat.setMaximumFractionDigits(2);

		return numberFormat.format((float) number / (float) total * 100);
	}

	/**
	 * 存储订单记录
	 * 
	 * @param consume       消费者
	 * @param cover_consume 被消费者
	 * @param consume_score 消费资源数据编号
	 * @param consume_type  消费类型
	 * @param amount        消费金额
	 */
	private int saveOrder(int consume, int cover_consume, int consume_score, int consume_type, BigDecimal amount,
			int roomId, int logTime) {

		String sql = "INSERT INTO t_order (t_consume, t_cover_consume, t_consume_type, t_consume_score, t_amount, t_create_time,t_room_id,t_log_time) VALUES (?, ?, ?, ?, ?, ?,?,?)";

		return this.getFinalDao().getIEntitySQLDAO().saveData(sql, consume, cover_consume, consume_type, consume_score,
				amount, DateUtils.format(new Date(), DateUtils.FullDatePattern), roomId, logTime);
	}

	/**
	 * 用户调用挂断
	 */
	@Override
	public MessageUtil userHangupLink(int userId) {
		MessageUtil mu = null;
		try {

			List<Integer> rooms = RoomTimer.getUserIdReturnRoomId(userId);

			if (!rooms.isEmpty()) {
				rooms.forEach(s -> {
					 this.breakLink(s, 2, userId);
				});
			}

			rooms = VideoTiming.getByUserResRoom(userId);
			if (!rooms.isEmpty()) {
				rooms.forEach(s -> {
					// 挂断链接
					 this.breakLink(s, 2, userId);
				});
			}
			mu = new MessageUtil(1, "");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户挂断链接异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 主播对用户发起聊天
	 */
	@Override
	public MessageUtil anchorLaunchVideoChat(int anchorUserId, int userId,int roomId ,int chatType) {
		MessageUtil mu = null;
			// 清理主播所在的所有房间 且把房间返回房间池
		try {
			HashMap<String,Object> hashMap = new HashMap<String, Object>();
			userHangupLink(anchorUserId);
			
			if (anchorUserId == userId) {
				return new MessageUtil(-7, "不能和自己进行聊天.");
			}
//			if(UserIoSession.getInstance().getMapIoSession(anchorUserId)==null||!UserIoSession.getInstance().getMapIoSession(anchorUserId).isActive()) {
//				return new MessageUtil(-10, "登录状态异常,请重新登录.");
//			};
			IoSession ioSession = UserIoSession.getInstance().getMapIoSession(userId);
			// 获取被链接人是否正在连线中
			if (RoomTimer.getUserIsBusy(userId) || VideoTiming.getUserExist(userId)) {
				// 写入到通话记录中
//				saveCallLog(anchorUserId, userId, roomId);

				return new MessageUtil(-2, "您拨打的用户正忙,请稍后再拨.");
			}
			// 判断对方是否设置了勿扰
//			String sql = "SELECT * FROM t_user WHERE t_id = ? AND t_is_not_disturb = 1";
//			List<Map<String, Object>> users = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(sql, userId);
//
//			if (null != users && !users.isEmpty()) {
//				return new MessageUtil(-3, "Sorry,对方设置了勿扰.");
//			}

			// 获取被链接人的余额是否足够进行视频聊天
			// 获取用户的所有金币
			String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

			Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(goldSql,
					userId);

			// 获取被链接人每分钟需要消耗多少金币
			String videoGoldSql = "SELECT t_video_gold,t_voice_gold FROM t_anchor_setup WHERE t_user_id = ?";

			List<Map<String, Object>> sqlList = this.getQuerySqlList(videoGoldSql, anchorUserId);

			if (sqlList.isEmpty()) {
				return new MessageUtil(-6, "您的资料未完善!无法进行呼叫.");
			}

			// 把链接人
			// 记录用户的链接信息
			saveCallLog(anchorUserId, userId, roomId);
			this.executeSQL("update t_room_time_log set t_chatType=?,t_call_time=now(),t_conversation_type=? where t_id=?", chatType,2,roomId);
			Room room = new Room(roomId);

			room.setLaunchUserId(userId);
			room.setCoverLinkUserId(anchorUserId);
			room.setCallUserId(anchorUserId);

			// 写入到通话记录中
			room.setConversationType(2);
			room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr") + room.getLaunchUserId() + "/" + roomId);
			room.setCoverLinkUserLiveCode(
					SystemConfig.getValue("play_addr") + room.getCoverLinkUserId() + "/" + roomId);
			
			// 默认足够支付聊天
			int satisfy = 1;
			String content="";
			int mid = 0;
			// 判断用户的金币是否满足聊天计时
			if(chatType==1||chatType==3||chatType==5) {
				if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
						.compareTo(new BigDecimal(sqlList.get(0).get("t_video_gold").toString())) < 0) {
					satisfy = -1;
				}
				content="邀请您进行视频聊天!";
				mid=Mid.anchorLinkUserRes;
			}else if(chatType==2||chatType==4||chatType==6) {
				if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
						.compareTo(new BigDecimal(sqlList.get(0).get("t_voice_gold").toString())) < 0) {
					satisfy = -1;
				}
				content="邀请您进行语音聊天!";
				mid=Mid.anchorLinkUserToVoiceRes;
			}
			room.setChatType(chatType);
			RoomTimer.useRooms.put(roomId, room);

			logger.info("当前链接人->{},当前被链接人->{},当前房间号->{},对方session->{}", anchorUserId, userId, roomId, ioSession);
			/** 激光推送 **/
			// 获取链接人的昵称
			Map<String, Object> userMap = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ?", anchorUserId);

			this.applicationContext.publishEvent(new PushMesgEvnet(new MessageEntity(userId,
					userMap.get("t_nickName") + content, 0, new Date(), 6, roomId, anchorUserId, satisfy)));
			/** socket推送 **/
			if (ioSession != null) {
				JSONObject fromObject = JSONObject
				.fromObject(new OnLineRes(mid, roomId, anchorUserId, satisfy));
				fromObject.put("chatType", chatType);
				ioSession.write(fromObject.toString());
			}
			//开始呼叫，设置用户 主播 忙碌状态 t_state
			this.executeSQL("update t_user set t_onLine = ? where t_id=? and  t_onLine != 2", User.ONLINE_BE_BUSY,anchorUserId);
			this.executeSQL("update t_home_table set t_onLine = ? where t_id=? and  t_onLine != 2",User.ONLINE_BE_BUSY, anchorUserId);
			this.executeSQL("update t_user set t_onLine = ? where t_id=? and  t_onLine != 2",User.ONLINE_BE_BUSY, userId);
			//用户呼叫主播 加入缓存 由此判断 VIP用户接听
			redisUtil.setTime("VIP_USER_"+anchorUserId+"_"+userId, "1",35000l);
			//设置之后 用户充值 不会挂断当前链接
			redisUtil.setTime("BALANCE_USER_ID_"+userId, "1", 30*1000l);
			mu = new MessageUtil(1, "正在链接.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("主播对用户发起聊天异常!", e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return mu;
	}

	/**
	 * 新增通话记录
	 * 
	 * @param userId
	 * @param coverUserId
	 */
	public int saveCallLog(int userId, int coverUserId, int roomId) {
		// 当对方未接听 数据写入到通话记录中
		return this.getFinalDao().getIEntitySQLDAO().saveData(
				"INSERT INTO t_call_log (t_callout_user, t_answer_user,t_room_id, t_create_time) VALUES (?,?,?,?);",
				userId, coverUserId, roomId, DateUtils.format(new Date(), DateUtils.FullDatePattern));
	}

	/**
	 * 修改通话记录
	 * 
	 * @param userId
	 * @param coverUserId
	 * @param time
	 */

	private void updateCallLog(int roomId, int time) {

//		logger.info("userId->{},coverUserId->{},roomId->{},time->{}", userId, coverUserId, roomId,time);
		this.executeSQL("UPDATE t_call_log SET t_call_time = ?,t_state =1 WHERE t_room_id = ? ", time, roomId);
//		List<Map<String, Object>> sqlList = getQuerySqlList(
//				"SELECT t_id FROM t_call_log WHERE t_callout_user = ? AND t_answer_user = ? AND t_room_id = ? AND t_state = 0 order by t_id desc ;",
//				userId, coverUserId, roomId);
//		if (!sqlList.isEmpty()) {
//			this.executeSQL("UPDATE t_call_log SET t_call_time = ?,t_state =1 WHERE t_id = ? ", time, sqlList.get(0).get("t_id"));
//		} else {
//			logger.info("{}连线{}主播在{}房间聊天,没有聊天记录.", userId, coverUserId, roomId);
//		}

	}

	@Override
	public Map<String, Object> getUuserCoverCall(int userId) {
		MessageUtil mu = null;
		try {

			// 房间号集合
			List<Integer> rooms = RoomTimer.getUserIdReturnRoomId(userId);

			if (!rooms.isEmpty()) {
				Room room = RoomTimer.useRooms.get(rooms.get(0));

				// 获取用户角色
				Map<String, Object> map = this.getMap("SELECT t_role FROM t_user WHERE  t_id = ? ", userId);

				int satisfy = 1;

				if (Integer.parseInt(map.get("t_role").toString()) == 0) {
					// 获取被链接人的余额是否足够进行视频聊天
					// 获取用户的所有金币
					String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

					Map<String, Object> totalGold = this.getFinalDao().getIEntitySQLDAO()
							.findBySQLUniqueResultToMap(goldSql, userId);

					// 获取被链接人每分钟需要消耗多少金币
					String videoGoldSql = "SELECT t_video_gold FROM t_anchor_setup WHERE t_user_id = ?";

					Map<String, Object> videoGold = this.getFinalDao().getIEntitySQLDAO()
							.findBySQLUniqueResultToMap(videoGoldSql, room.getCoverLinkUserId());
					// 判断用户的金币是否满足聊天计时
					if (null == totalGold.get("totalGold") || new BigDecimal(totalGold.get("totalGold").toString())
							.compareTo(new BigDecimal(videoGold.get("t_video_gold").toString())) < 0) {
						satisfy = -1;
					}
				}

				Map<String, Object> rmap = new HashMap<String, Object>();
				rmap.put("connectUserId", room.getLaunchUserId());
				rmap.put("coverLinkUserId", room.getCoverLinkUserId());
				rmap.put("roomId", room.getRoomId());
				rmap.put("satisfy", satisfy);
				return  rmap;
			}
			return new HashMap<String, Object>();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("{}获取是否被呼叫异常!", userId, e);
			mu = new MessageUtil(0, "程序异常!");
		}
		return new HashMap<String, Object>();
	}

	@Override
	public MessageUtil getRoomState(int roomId) {
		try {

			logger.info("--房间{}获取房间状态--");

			Room room = RoomTimer.useRooms.get(roomId);

			if (null == room || 0 == room.getLaunchUserId() && 0 == room.getCoverLinkUserId()) {
				room = VideoTiming.getRoom(roomId);
			}
			// 获取计费信息
			Map<String, Integer> map = VideoTiming.timingUser.get(roomId);

			if (null != map && !map.isEmpty()) {
				return new MessageUtil(1, "正在计时!");
			}

			// 获取数据库中的记录表 判断是否是内存中计时失败
			List<Map<String, Object>> sqlList = this.getQuerySqlList("SELECT t_id FROM t_room_time WHERE t_room_id = ?",
					roomId);

			if (null != sqlList && !sqlList.isEmpty()) {
				return new MessageUtil(1, "正在计时!");
			}

			return new MessageUtil(-1, "房间异常!");

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public void addRoomRate(int userId, int gold) {
		try {
			
//			Map<String, Integer> map = VideoTiming.timingUser.get(userId);
			Map<String, Integer> map =VideoTiming.getUserMoneyMap(userId);
			// 如果用户正在进行视频聊天中
			if (null != map && !map.isEmpty()) {
				// 更新视频聊天中的总金币数
				map.put("gold", map.get("gold") + gold);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	@Override
	public MessageUtil userBuyGold(int userId, int gold) {
		try {
			
//			Map<String, Integer> map = VideoTiming.timingUser.get(userId);
			Map<String, Integer> map =VideoTiming.getUserMoneyMap(userId);
			// 如果用户正在进行视频聊天中
			if (null != map && !map.isEmpty()) {
				// 更新视频聊天中的总金币数
				int bal = map.get("gold") - gold ;
				logger.info("扣除后剩余金币-->{}",bal);
				map.put("gold", bal);
				return new MessageUtil(1, "扣除成功!");
			}
			return new MessageUtil(-1, "减币失败!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new MessageUtil(-1, "减币失败!");
	}

	/**
	 * 获取用户是否正在聊天
	 */
	@Override
	public MessageUtil getUserIsVideo(int userId) {
		
//		Map<String, Integer> map = VideoTiming.timingUser.get(userId);
		Map<String, Integer> map =VideoTiming.getUserMoneyMap(userId);
		return new MessageUtil(1, map);
	}
	
	
	
	public boolean checkSvipUser(int userId,int anchorId) {
		// 呼叫方式是视频 & 聊天发起方式是用户呼叫主播
		String sql="select t_is_vip,t_is_svip from t_user where t_id=?";
		Map<String, Object> userMap = this.getMap(sql,userId);
		try {
			if(userMap.get("t_is_svip").toString().equals("0")) {
				// 用户是svip && 主播的接听免费次数 > 0
				String free_svip = redisUtil.get("free_svip_number_"+anchorId);
				
				//用户设置5次
				 Map<String, String> free_user_svip_map = redisUtil.hmget("free_user_svip_number_"+userId);
				 
				 String free_user_svip ="0";
				 String free_user_to_anchor ="";
				if(StringUtils.isBlank(free_svip)) {
					free_svip="0";
				}
				if(free_user_svip_map!=null
						&&StringUtils.isNotBlank(free_user_svip_map.get("free_user_svip"))
						&&StringUtils.isNotBlank(free_user_svip_map.get("free_user_to_anchor"))) {
					free_user_svip=free_user_svip_map.get("free_user_svip");
					free_user_to_anchor=free_user_svip_map.get("free_user_to_anchor");
				}
				
				int free_svip_number= Integer.parseInt(free_svip);
				int free_user_svip_number= Integer.parseInt(free_user_svip);
				if (free_svip_number > 0&&free_user_svip_number<5&&!free_user_to_anchor.contains(anchorId+"")){
					return true;
//					redisUtil.setTime("free_svip_number_"+anchorId,free_svip_number - 1+"", DateUtils.millis(1));
//					//设置用户的key
//					HashMap<String,String> hashMap = new HashMap<String,String>();
//					free_user_to_anchor= free_user_to_anchor+","+anchorId;
//					hashMap.put("free_user_svip", free_user_svip_number + 1+"");
//					hashMap.put("free_user_to_anchor", free_user_to_anchor);
//					redisUtil.hmset("free_user_svip_number_"+userId,hashMap );
//					redisUtil.expireMillTime("free_user_svip_number_"+userId,DateUtils.millis(1));
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public MessageUtil breakAllVideoUser() {
		// TODO Auto-generated method stub
		 VideoTiming.timingUser.forEach((k,v) ->{
			 this.breakLink(k, 8, v.get("userId"));
		 });
		return new MessageUtil(1,"挂断成功");
	}
	
	
	
}
