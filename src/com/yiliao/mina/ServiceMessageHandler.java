package com.yiliao.mina;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.Room;
import com.yiliao.domain.SentenceSendList;
import com.yiliao.domain.UserIoSession;
import com.yiliao.mina.req.UserLoginReq;
import com.yiliao.mina.res.UserLoginRes;
import com.yiliao.service.LoginService;
import com.yiliao.timer.SimulationVideoTimer;
import com.yiliao.timer.SocketDelayTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.Mid;
import com.yiliao.util.MinaThread;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

public class ServiceMessageHandler extends MinaServerHanlder {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	private RedisUtil redisUtil = (RedisUtil) SpringConfig.getInstance()
			.getBean("redisUtil");
	com.yiliao.service.PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance().getBean("personalCenterService");

	public void messageHandler(IoSession session, Object message) {

		logger.info("当前登陆数据-->sessionID{}-->接受数据{}",session.getId(), message.toString());
		
		JSONObject mes = JSONObject.fromObject(message);
		
		// 用户登陆
		if (Mid.userLoginReq == mes.getInt("mid")) {

			UserLoginReq ulq = (UserLoginReq) JSONObject.toBean(mes,UserLoginReq.class);
			
			logger.info("当前时间-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			
			IoSession userSession = UserIoSession.getInstance().getMapIoSession(ulq.getUserId());
			//判断用户的iosession 是否存在
			if(null != userSession && userSession.getId() != session.getId()) {
				logger.info("--原session存在 关闭session--");
				userSession.isClosing();
			}
			
			logger.info("当前时间-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			// 存储iosession
			UserIoSession.getInstance().putMapIoSesson(ulq.getUserId(), session);
			// 存储登陆用户消息
			LoginInfo li = new LoginInfo();
			
			li.setT_role(ulq.getT_role());
			li.setUserId(ulq.getUserId());
			li.setT_sex(ulq.getT_sex());
			li.setLoginTime(System.currentTimeMillis());
			li.setTimes(this.randomCommonList(1, 3, 30));
			Map<String, Object> userInfo = personalCenterService.getUserTextSwitch(ulq.getUserId());
			
			ulq.setT_is_vip(Integer.parseInt(userInfo.get("t_is_vip").toString()));
			li.setT_is_vip(Integer.parseInt(userInfo.get("t_is_vip").toString()));
			li.setT_is_svip(Integer.parseInt(userInfo.get("t_is_svip").toString())); 
			
			li.setT_text_switch(Integer.parseInt(userInfo.get("t_text_switch").toString()));
			li.setT_voice_switch(Integer.parseInt(userInfo.get("t_voice_switch").toString()));
			li.setT_voide_switch(Integer.parseInt(userInfo.get("t_is_not_disturb").toString()));
			
			// 构建登陆成功消息
			UserLoginRes urs = new UserLoginRes();
			urs.setMid(Mid.userLoginRes);
			urs.setState(1);
			urs.setMessage("登陆成功!");

			session.write(JSONObject.fromObject(urs).toString());
			
			//虚拟主播语术推送开关 0 关闭 1打开
			String t_false_send_text_switch = redisUtil.get("t_false_send_text_switch");
			String t_false_send_text_switch_count = redisUtil.get("t_false_send_text_switch_count");
			
//			虚拟视频弹窗
			if(t_false_send_text_switch!=null&&t_false_send_text_switch.equals("1")
					&&ulq.getT_sex()==1&&ulq.getT_role()==0
							&&li.getT_text_switch()==1
					&&t_false_send_text_switch_count!=null&&Integer.parseInt(t_false_send_text_switch_count)>0) {
				
				String t_false_send_video_switch = redisUtil.get("t_false_send_video_switch");
				boolean flag=false;
				if(t_false_send_video_switch!=null&&t_false_send_video_switch.equals("0")&&ulq.getT_is_vip()==1) {
					//0： 余额不足的用户   1：非VIP用户
					flag=true;
					logger.info("非VIP");
				} else if (t_false_send_video_switch!=null&&t_false_send_video_switch.equals("1")) {
					//余额不足
					//查询最低设置视频每分钟金钱
					Map<String, Object> goldSetInfo = personalCenterService.getUserGoldSetInfo();
					if(goldSetInfo!=null) {
						String[] split = goldSetInfo.get("t_extract_ratio").toString().split(",");
						int compareTo = new BigDecimal(userInfo.get("allMopney").toString()).compareTo(new BigDecimal(split[0]));
						logger.info("用户{}，金钱{}，比较值{}",ulq.getUserId(),userInfo.get("allMopney").toString(),compareTo);
						if( compareTo < 0 ) {
							flag=true;
						}
					}
				}
				if(flag) {
					//语术
					List<Map<String, Object>> sendTenceUserMap = SentenceSendList.getInstance().getSendTenceUserMap(li.getUserId());
					if(sendTenceUserMap==null||sendTenceUserMap.isEmpty()) {
						List<Map<String,Object>> sentenceList = personalCenterService.getVirtualSentenceList(0,ulq.getUserId(),t_false_send_text_switch_count);
						if(sentenceList!=null&&!sentenceList.isEmpty()) {
							SentenceSendList.getInstance().putSendTenceUserMap(li.getUserId(), sentenceList);
							logger.info("用户虚拟IM消息添加成功"+li.getUserId());
						}
					}
					String t_all_false_send_video_switch = redisUtil.get("t_all_false_send_video_switch");
					if(t_all_false_send_video_switch!=null&&t_all_false_send_video_switch.equals("1")
							&&ulq.getT_sex()==1&&ulq.getT_role()==0
									&&li.getT_voide_switch()==1) {
						//虚拟视频主播
						List<Integer> virtualList = personalCenterService.getVirtualList();
						HashMap<String,Object> hashMap = new HashMap<String, Object>();
						hashMap.put("anchor", virtualList);
//						呼叫次数
						hashMap.put("callCount", 0);
//						呼叫时间
						hashMap.put("time", 0);
						hashMap.put("t_is_not_disturb", userInfo.get("t_is_not_disturb").toString());
						hashMap.put("vip", ulq.getT_is_vip());
//						发送间隔时间
						hashMap.put("sendTime", this.randomCommonList(1, 3, 30));
						SimulationVideoTimer.callUser.put(ulq.getUserId(), hashMap);
					}
				}
			}
			
			
			
			
			
			if (li.getT_role() == 0) {
				UserIoSession.getInstance().putLoginUserMap(li.getUserId(), li);
			} else {
				if (li.getT_sex() == 0) {
					UserIoSession.getInstance().putLoginGirlAnchorMap(li.getUserId(), li);
				} else {
					UserIoSession.getInstance().putLoginMaleAnchorMap(li.getUserId(), li);
				}
			}

			// 获取该用户是否在延迟挂断任务中 如果在 取消任务 判断主播的房间号
			if (SocketDelayTimer.socketDelayUserLog.containsKey(li.getUserId())) {
				Integer integer = SocketDelayTimer.socketDelayUserLog.get(li.getUserId());
				SocketDelayTimer.socketDelayUserLog.remove(li.getUserId());
				SocketDelayTimer.socketDelayUser.remove(integer);
			}
			logger.info("当前完成登陆时间-->{}",DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
			//处理用户数据
			new MinaThread(li.getUserId(), session).start();
			
		} else if(Mid.superviseLogReq == mes.getInt("mid")){ //子服务器登陆
			
		    JSONObject json = new JSONObject();
		    json.put("mid", Mid.superviseLogRes);
		    json.put("msg", "hole,欢迎你登陆【监控服务器】.");
			logger.info("---子服务器监控服务器已登陆---");
			session.write(json);
		}

	}

	/**
	 * 随机指定范围内N个不重复的数 最简单最基本的方法
	 * 
	 * @param min
	 *            指定范围最小值
	 * @param max
	 *            指定范围最大值
	 * @param n
	 *            随机数个数
	 */
	public List<Integer> randomCommon(int min, int max, int n) {
		if (n > (max - min + 1) || max < min) {
			return null;
		}
		List<Integer> arr = new ArrayList<Integer>();
		arr.add(2);
		int count = 0;
		while (count < n) {
			int num = (int) (Math.random() * (max - min)) + min;
			if (!arr.contains(num)) {
				arr.add(num);
				count++;
			}
		}
		return arr;
	}

	public static void main(String[] args) {

		ServiceMessageHandler smh = new ServiceMessageHandler();

		System.out.println(smh.randomCommon(1, 10, 4));
	}
	public List<Integer> randomCommonList(int min, int max, int n) {
//		if (n > (max - min + 1) || max < min) {
//			return null;
//		}
		List<Integer> arr = new ArrayList<Integer>();
		int count = 0;
		while (count < n) {
			int num = (int) (Math.random() * (max*60 - min*60)) + min*60;
			arr.add(num);
			count++;
		}
		return arr;
	}
	/**
	 * 清理掉线数据
	 * 
	 * @param session
	 */
	public void exceptionCaught(IoSession session) {

		// 循环迭代数据
		for (Entry<Integer, IoSession> m : UserIoSession.getInstance().getMapIoSesson().entrySet()) {
			if (session.getId() == m.getValue().getId()) {
				UserIoSession.getInstance().getMapIoSesson().remove(m.getKey());
				logger.info("用户异常掉线清理用户session-->userId->{},sesison->{}", m.getKey(), session);
				LoginService loginAppService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
				loginAppService.socketBreak(m.getKey());

				List<Integer> byUserResRoom = VideoTiming.getByUserResRoom(m.getKey());

				if (null != byUserResRoom && !byUserResRoom.isEmpty()) {

					byUserResRoom.forEach(s -> {
						Room room = VideoTiming.getRoom(s);

						// 如果该用户是在聊天计时 那么加入到延迟挂断任务
						Map<String, Integer> map = VideoTiming.timingUser.get(room.getRoomId());

						if (null != map && !map.isEmpty()) {
							// 加入到计时任务中
							SocketDelayTimer.socketDelayUser.put(room.getRoomId(), 0);
							SocketDelayTimer.socketDelayUserLog.put(m.getKey(), room.getRoomId());
						} else {
							logger.info("无法获取计时数据!");
						}
					});
				}

			}
		}
	}
}
