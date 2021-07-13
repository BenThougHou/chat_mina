package com.yiliao.service.impl;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.tls.tls_sigature.tls_sigature;
import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.MessageEntity;
import com.yiliao.domain.OnLineRes;
import com.yiliao.domain.UserIoSession;
import com.yiliao.evnet.PushMesgEvnet;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.HttpUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.RandomUtill;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SystemConfig;
import com.yiliao.util.tencent.TLSSigAPIv2;

import cn.hutool.json.JSONUtil;
import net.sf.json.JSONObject;

@Service("personalCenterService")
public class PersonalCenterServiceImpl extends ICommServiceImpl implements PersonalCenterService {

	@Autowired
	RedisUtil redisUtil;
	@Override
	public void timerUnseal() {
		try {
			
			List<Map<String,Object>> sqlList = getQuerySqlList("SELECT t_id,t_user_id FROM t_disable WHERE t_state = 0 AND t_end_time <= now()");
			
			sqlList.forEach(s ->{
				//修改用户数据
				this.executeSQL("UPDATE t_disable d JOIN t_user u ON d.t_user_id = u.t_id SET d.t_state = 1,u.t_disable = 0 WHERE u.t_id = ?", s.get("t_user_id"));
				
				 //数据写入到t_home_table 中
			    StringBuffer inSql = new StringBuffer();
			    inSql.append("INSERT INTO t_home_table (t_id, t_cover_img, t_handImg, t_nickName, t_age, t_city, t_vocation, t_autograph, t_online, t_video_gold, t_score ) ");
			    inSql.append("SELECT u.*,s.t_video_gold,AVG(IFNULL(e.t_score,5)) AS t_score ");
			    inSql.append("FROM ( ");
			    inSql.append("SELECT t_id,t_cover_img,t_handImg,t_nickName,t_age,t_city,t_vocation,t_autograph,t_online FROM t_user ");
			    inSql.append(" WHERE  t_role = 1 AND t_disable = 0  AND t_cover_img is not null AND t_sex = 0 AND t_id = ? ");
			    inSql.append(") u LEFT JOIN t_user_evaluation e ON e.t_user_id = u.t_id ");
			    inSql.append("LEFT JOIN t_anchor_setup s ON  s.t_user_id = u.t_id ");
			    inSql.append("GROUP BY u.t_id;");
			    
			    this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql.toString(), s.get("t_user_id"));
				
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取模拟消息
	 */
	@Override
	public String getSimulationMsg(int sex) {
		try {

			String qSql = " SELECT t_centent FROM t_simulation WHERE t_sex != ? AND t_id >= ((SELECT MAX(t_id) FROM t_simulation)-(SELECT MIN(t_id) FROM t_simulation)) * RAND() + (SELECT MIN(t_id) FROM t_simulation)  LIMIT 1 ";

			List<Map<String, Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, sex);

			return sqltoMap.isEmpty() ? "" : sqltoMap.get(0).get("t_centent").toString();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取模拟消息异常!", e);
		}
		return null;
	}

	@Override
	public Map<String, Object> getUserTextSwitch(int userId) {
		try {
			
			Map<String, Object> map = getMap("SELECT u.t_text_switch,u.t_voice_switch,"
					+ "case when u.t_is_vip=0 or u.t_is_svip=0 then 0 else 1 end t_is_vip,"
					+ "u.t_is_svip,"
					+ "u.t_is_not_disturb,"
					+ "(b.t_recharge_money+b.t_profit_money+b.t_share_money) allMopney FROM t_user u "
					+ "left join t_balance b on u.t_id=b.t_user_id"
					+ " WHERE u.t_id = ?", userId);
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<Integer> getVirtualList() {
		List<Integer> findBySQL=new ArrayList<Integer>();
		try {
			List<Map<String,Object>> querySqlList = this.getQuerySqlList("SELECT  t_user_id FROM t_virtual order by rand() limit 30");
			if(!querySqlList.isEmpty()) {
				for (Map<String, Object> map : querySqlList) {
					Integer userId = (Integer) map.get("t_user_id");
					findBySQL.add(userId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			findBySQL=new ArrayList<Integer>();
		}
		return findBySQL;
	}
	@Override
	public List<Map<String,Object>> getVirtualSentenceList(int type,int userId,String number) {
		try {String sql="SELECT t_sentence_id, 0 sentence_sort,t_anchor_id,unix_timestamp(now())*1000 sendTime from t_user_sentence"
				+ " where  FIND_IN_SET("+userId+",t_send_user_list)=0 order by rand() limit "+number;
		logger.info(sql);
			List<Map<String,Object>> querySqlList = this.getQuerySqlList(sql);
			return querySqlList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<Map<String,Object>>();
	}
	
	@Override
	public void setUpBrower(int userId,int sex,int count) {
		List<Map<String,Object>> querySqlList = this.getQuerySqlList("select t_id from t_home_table where t_sex=?", sex);
		
		if(querySqlList.isEmpty()) {
			return;
		}
		
		if(!querySqlList.isEmpty()&&querySqlList.size()<=count) {
			count=querySqlList.size();
		}
		int i=0;
		while(i<count) {
			int rn = RandomUtill.getRandomInt(querySqlList.size());
			Map<String, Object> map = querySqlList.get(rn);
			//加入brower
			// 修改被查看人的浏览次数
			String sql = "UPDATE t_user u SET u.t_browse_sum = (u.t_browse_sum+1) WHERE u.t_id = ?";
			this.getFinalDao().getIEntitySQLDAO().executeSQL(sql, userId);
			// 判断当前人今天是否浏览过了
			sql = "SELECT * FROM t_browse WHERE t_browse_user = ? AND t_cover_browse = ? AND t_create_time BETWEEN ? AND ? ";
			List<Map<String, Object>> browse = this.getQuerySqlList(sql, map.get("t_id"), userId,
					DateUtils.format(new Date()) + " 00:00:00", DateUtils.format(new Date()) + " 23:59:59");
			if (null == browse || browse.isEmpty()) {
				// 保存浏览记录
				sql = "INSERT INTO t_browse (t_browse_user, t_cover_browse, t_create_time) VALUES (?, ?, ?)";
				this.executeSQL(sql,  map.get("t_id"), userId, DateUtils.format(new Date(), DateUtils.FullDatePattern));
				
				String string = redisUtil.get("BROWSE_USERID_"+userId);
				if(string!=null) {
					redisUtil.set("BROWSE_USERID_"+userId, Integer.parseInt(string)+1+"", 60*24*7L);
				}else {
					redisUtil.set("BROWSE_USERID_"+userId, "1", 60*24*7L);
				}
				
			} else {
				sql = "UPDATE t_browse SET  t_create_time=? WHERE t_browse_id=?;";

				this.executeSQL(sql, DateUtils.format(new Date(), DateUtils.FullDatePattern),
						Integer.parseInt(browse.get(0).get("t_browse_id").toString()));
			}
			i++;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void sendTXUserIM(Integer userId,Integer anchUserId,int t_sentence_type,String t_sentence_content,String duration) {
		
		try {
			ArrayList<Map<String,Object>> arrayList = new ArrayList<Map<String,Object>>();
			Map<String,Object>  map=new HashMap<String,Object>();
			Map<String,Object>  msgMap=new HashMap<String,Object>();
			Map<String,Object>  sendMap=new HashMap<String,Object>();
			boolean flag=true;
			switch (t_sentence_type) {
			
			case 0:
				//文字
//				LoginInfo voiceLoginUserMap = UserIoSession.getInstance().getLoginUserMap(userId);
//				if (null==voiceLoginUserMap||voiceLoginUserMap.getT_text_switch()==0) {
//					flag=false;
//				}
				map.put("MsgType", "TIMTextElem");
				msgMap.put("Text", t_sentence_content);
				break;
			case 1:
				//图片 TIMImageElem
//				LoginInfo voiceLoginUserMapImg = UserIoSession.getInstance().getLoginUserMap(userId);
//				if (null==voiceLoginUserMapImg||voiceLoginUserMapImg.getT_text_switch()==0) {
//					flag=false;
//				}
				map.put("MsgType", "TIMCustomElem");
				sendMap.put("type", "picture");
				sendMap.put("fileUrl", t_sentence_content);
				msgMap.put("Desc", "[文本]");
				
				break;
			case 2:
				//语音  TIMSoundElem
//				LoginInfo voiceLoginUserMapVoice = UserIoSession.getInstance().getLoginUserMap(userId);
//				if (null==voiceLoginUserMapVoice||voiceLoginUserMapVoice.getT_text_switch()==0) {
//					flag=false;
//				}
				map.put("MsgType", "TIMCustomElem");
				sendMap.put("type", "voice");
				sendMap.put("fileUrl", t_sentence_content);
				sendMap.put("duration", duration);
				msgMap.put("Desc", "[语音]");
				
				break;
			case 3:
				//视频
				break;
			case 4:
				//视频弹窗 当前只进行虚拟视频拨打 发送消息--->挂断接口
				OnLineRes or = new OnLineRes();
				or.setMid(Mid.anchorLinkUserRes);
				or.setRoomId(1000001);
				or.setConnectUserId(anchUserId);
				or.setSatisfy(-1);
				redisUtil.setTime("VIP_USER_"+anchUserId+"_"+userId,"1",35000l);
				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(userId);
				LoginInfo loginUserMap = UserIoSession.getInstance().getLoginUserMap(userId);
				map.put("MsgType", "TIMCustomElem");
				sendMap.put("call_type", "video");
				sendMap.put("type", "video_unconnect_anchor");
				Map<String, Object> anochorMap = this.getMap("select ROUND(t_video_gold) t_video_gold,ROUND(t_voice_gold) t_voice_gold from t_anchor_setup where  t_user_id=?", anchUserId);
				sendMap.put("video_price",  anochorMap.get("t_video_gold"));
				sendMap.put("voice_price",  anochorMap.get("t_voice_gold"));
				sendMap.put("anchor_id",  anchUserId);
				msgMap.put("Desc", "[视频邀请]");
				if (null != ioSession&&null!=loginUserMap&&loginUserMap.getT_voide_switch()==1) {
					redisUtil.remove("BALANCE_USER_ID_"+userId);
//					log.info("socket 发送："+JSONObject.fromObject(or).toString());
					ioSession.write(JSONObject.fromObject(or));
				}
//				else {
//					flag=false;
//				}
				try {
					
					this.applicationContext.publishEvent(new PushMesgEvnet(new MessageEntity(userId,
							"邀请您进行视频聊天!", 0, new Date(), 6, 0, 0, 0)));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				break;
			case 5:
				//语音弹窗 当前只进行虚拟语音视频拨打 发送消息--->挂断接口
				OnLineRes olr = new OnLineRes();
				olr.setMid(Mid.onLineToVoiceRes);
				olr.setRoomId(1000001);
				olr.setConnectUserId(anchUserId);
				olr.setSatisfy(-1);
				redisUtil.setTime("VIP_USER_"+anchUserId+"_"+userId,"1",35000l);
				IoSession ioSessionVoice = UserIoSession.getInstance().getMapIoSession(userId);
				LoginInfo voiceUserMap = UserIoSession.getInstance().getLoginUserMap(userId);
				map.put("MsgType", "TIMCustomElem");
				sendMap.put("call_type", "voice");
				msgMap.put("Desc", "[语音邀请]");
				sendMap.put("type", "video_unconnect_anchor");
				Map<String, Object> anochorVoiceMap = this.getMap("select ROUND(t_video_gold) t_video_gold,ROUND(t_voice_gold) t_voice_gold from t_anchor_setup where  t_user_id=?", anchUserId);
				sendMap.put("video_price",  anochorVoiceMap.get("t_video_gold"));
				sendMap.put("voice_price",  anochorVoiceMap.get("t_voice_gold"));
				sendMap.put("anchor_id",  anchUserId);
				if (null != ioSessionVoice&&null!=voiceUserMap&&voiceUserMap.getT_voice_switch()==1) {
					redisUtil.remove("BALANCE_USER_ID_"+userId);
//					log.info("socket 发送："+JSONObject.fromObject(or).toString());
					ioSessionVoice.write(JSONObject.fromObject(olr));
				}
//				else {
//					flag=false;
//				}
				try {
					
					this.applicationContext.publishEvent(new PushMesgEvnet(new MessageEntity(userId,
							"邀请您进行视频聊天!", 0, new Date(), 6, 0, 0, 0)));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				break;
			}
			//封装IM发送信息
			if(flag) {
				JSONObject json = new JSONObject();
				json.put("From_Account", anchUserId+10000+"");
				json.put("SyncOtherMachine", 1);
				json.put("To_Account", userId+10000+"");
				json.put("MsgLifeTime", 60*60*24*6);
				json.put("MsgRandom", 1287657);
				json.put("MsgTimeStamp", System.currentTimeMillis()/1000);
//				msgMap.put("Data", sendMap);
				
				map.put("MsgContent", msgMap);
				logger.info("消息:"+JSON.toJSONString(sendMap));
				 
//				msgMap.put("Data", JSON.toJSONString(sendMap));
				String jsonString = JSON.toJSONString(sendMap);
				String substring = jsonString.substring(1);
				logger.info(substring);
				msgMap.put("Data", "serverSend&&"+substring);
				msgMap.put("Ext", "1234");
				msgMap.put("Sound", "12345");
				arrayList.add(map);
				json.put("MsgBody", arrayList);
				String string = redisUtil.get("admin_usersig");
				if(string==null||StringUtils.isBlank(string)) {
					string = generateAdminUserSig();
				}
				logger.info("IM模拟消息请求参数 "+json.toString());
				String jsonObject = HttpUtil.httpClent(SystemConfig.getValue("im_filter_url")
						+ "v4/openim/sendmsg?sdkappid=" + SystemConfig.getValue("im_appid") + "&identifier=administrator&usersig="
						+ string + "&random=99999999&contenttype=json",json.toString());
				logger.info("IM模拟消息返回参数 "+jsonObject.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<Map<String,Object>> getSentenceList(Integer t_sentence_id) {
		try {
			
			List<Map<String,Object>> querySqlList = this.getQuerySqlList("select * from t_send_sentence where t_sentence_id=? order by t_sort ", t_sentence_id);
			return querySqlList;
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 生成管理员的userSig
	 */
	private String generateAdminUserSig() {		
		// 刷新用户的usig
		String str = new TLSSigAPIv2(Long.parseLong(SystemConfig.getValue("im_appid")),SystemConfig.getValue("im_private_key")).genSig("administrator",3600*24*180l);

		redisUtil.set("admin_usersig", str, 1000 * 60 * 60 * 24 * 180l);
		return str;
	}

	@Override
	public boolean getUserChatStatus(Integer userId) {
		try {
			List<Map<String,Object>> querySqlList = this.getQuerySqlList("select t_onLine from t_user where t_id =?", userId);
			if(querySqlList!=null&&!querySqlList.isEmpty()&&querySqlList.get(0).get("t_onLine").toString().equals("0")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public Boolean checkBlackUserInfo(int userId, int coverUserId) {
		try {
			String sqlString="select t_id from t_black_user where t_user_id = ? and t_cover_user_id=?  ";
			List<Map<String,Object>> blackUserMap = this.getQuerySqlList(sqlString, coverUserId,userId);	
			if(blackUserMap!=null&&!blackUserMap.isEmpty()) {
					return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}  
		return false;
	}

	@Override
	public void sendTXUserVoideIM(int userId, int anchUserId, Map<String, Object> msgMap) {
		
		try {
			ArrayList<Map<String,Object>> arrayList = new ArrayList<Map<String,Object>>();
			Map<String,Object>  map=new HashMap<String,Object>();
			map.put("MsgType", "TIMCustomElem");
			//封装IM发送信息
			JSONObject json = new JSONObject();
			json.put("From_Account", userId+10000+"");
			json.put("SyncOtherMachine", 1);
			json.put("To_Account", anchUserId+10000+"");
			json.put("MsgLifeTime", 60*60*24*6);
			json.put("MsgRandom", 1287657);
			json.put("MsgTimeStamp", System.currentTimeMillis()/1000);
			
			String jsonString = JSON.toJSONString(msgMap);
			String substring = jsonString.substring(1);
			logger.info(substring);
			msgMap.put("Data", "serverSend&&"+substring);
			msgMap.put("Desc", msgMap.get("desc").toString());
			msgMap.put("Ext", "1234");
			msgMap.put("Sound", "12345");
			
			map.put("MsgContent", msgMap);
			
			
			
			arrayList.add(map);
			json.put("MsgBody", arrayList);
			String string = redisUtil.get("admin_usersig");
			
			if(string==null||StringUtils.isBlank(string)) {
				string = generateAdminUserSig();
			}
			logger.info("IM模拟消息请求参数 "+json.toString());
			String jsonObject = HttpUtil.httpClent(SystemConfig.getValue("im_filter_url")
					+ "v4/openim/sendmsg?sdkappid=" + SystemConfig.getValue("im_appid") + "&identifier=administrator&usersig="
					+ string + "&random=99999999&contenttype=json",json.toString());
			logger.info("IM模拟消息返回参数 "+jsonObject.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updSendtenceList() {
		try {
			this.executeSQL("update t_user_sentence set t_send_user_list='' ");
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	@Override
	public void updSentenceSendedList(Integer userId,String anoUserId) {
		try {
			this.executeSQL("update t_user_sentence set t_send_user_list=CONCAT(t_send_user_list,',',?) where t_anchor_id=?",userId,anoUserId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public Map<String, Object> getUserGoldSetInfo() {
		try {
			return this.getMap("select t_extract_ratio from t_extract  where t_project_type=5");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
