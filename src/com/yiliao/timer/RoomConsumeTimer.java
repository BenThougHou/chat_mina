package com.yiliao.timer;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

import com.yiliao.domain.User;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.ICommService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

public class RoomConsumeTimer {

	ICommService icService =  (ICommService) SpringConfig.getInstance().getBean("iCommServiceImpl");
	
	public void run () {
		try {
			
			List<Map<String,Object>> sqlList = icService.getQuerySqlList("SELECT t_room_id AS roomId,t_call_user_id AS userId,t_answer_user_id AS anchorId,t_an_vi_gold AS gold,t_create_time AS time FROM t_room_time ");
			sqlList.forEach(s ->{
				if(!VideoTiming.timingUser.containsKey(Integer.parseInt(s.get("roomId").toString()))){
					//获取用户的所有金币
					String goldSql = "SELECT SUM(t_recharge_money+t_profit_money+t_share_money) AS totalGold FROM t_balance WHERE t_user_id = ?";

					Map<String, Object> totalGold = icService.getMap(goldSql,s.get("userId"));
					
					Long time =0L;
					try {
						time = (System.currentTimeMillis() - DateUtils.parse(s.get("time").toString(),DateUtils.FullDatePattern).getTime())/1000;
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
					Map<String, Integer> map = new HashMap<String, Integer>();
					map.put("gold", new BigDecimal(totalGold.get("totalGold").toString()).intValue());
					map.put("deplete", new BigDecimal(sqlList.get(0).get("gold").toString()).intValue());
					map.put("timing", time.intValue());
					map.put("roomId", Integer.parseInt(s.get("roomId").toString())); //房间号
					map.put("anthorId",Integer.parseInt(s.get("anchorId").toString())); //主播编号

					VideoTiming.timingUser.put(Integer.parseInt(s.get("roomId").toString()), map);
				
					// 修改主播状态为忙碌
					icService.executeSQL("UPDATE t_user SET t_onLine = ? WHERE  t_id = ?;", User.ONLINE_BE_BUSY,s.get("anchorId"));
					//修改home_table 表中在线状态为忙碌
					icService.executeSQL("UPDATE t_home_table SET t_onLine = ? WHERE t_id = ? ", User.ONLINE_BE_BUSY, s.get("anchorId"));
					//链接人
					//获取提示信息
					 List<Map<String, Object>> tipsList = icService.getQuerySqlList("SELECT t_video_hint FROM t_system_setup ");
					 //如果后台填写了 提示信息
					 if(!tipsList.isEmpty()) {
						 //链接人session
						 IoSession callUserIoSession = UserIoSession.getInstance().getMapIoSession(Integer.parseInt(s.get("userId").toString()));
						 if(null != callUserIoSession && !callUserIoSession.isClosing()) {
							 //推送 提示信息
							 callUserIoSession.write(JSONObject.fromObject(new HashMap<String,Object>(){{
								 put("mid", Mid.sendVideoTipsMsg);
								 put("msgContent", tipsList.get(0).get("t_video_hint"));
							 }}).toString());
						 }
						 //被链接人 session
						 IoSession coverUserIoSession = UserIoSession.getInstance().getMapIoSession(Integer.parseInt(s.get("anchorId").toString()));
						 if(null != coverUserIoSession && !coverUserIoSession.isClosing()) {
							 coverUserIoSession.write(JSONObject.fromObject(new HashMap<String,Object>(){{
								 put("mid", Mid.sendVideoTipsMsg);
								 put("msgContent", tipsList.get(0).get("t_video_hint"));
							 }}).toString());
						 }
					 }
					
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
