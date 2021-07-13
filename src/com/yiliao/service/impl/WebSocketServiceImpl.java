package com.yiliao.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.Room;
import com.yiliao.domain.UserIoSession;
import com.yiliao.mina.res.GetOutOfLineRes;
import com.yiliao.service.MinaService;
import com.yiliao.service.VideoChatService;
import com.yiliao.service.WebSocketService;
import com.yiliao.timer.RoomTimer;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;
import com.yiliao.websocket.WebSocketIoHandler;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 客户端监控获取的数据
 * @author Administrator
 *
 */
@Service("webSocketService")
public class WebSocketServiceImpl extends ICommServiceImpl implements
		WebSocketService {

	//聊天service
	VideoChatService videoChatService =  (VideoChatService) SpringConfig.getInstance().getBean("videoService");
	
	private Map<Integer, IoSession> del = new HashMap<Integer, IoSession>();
	
	List<String> arr = new ArrayList<String>();
	
	String qSql = "SELECT t_nickName,t_phone FROM t_user WHERE t_id = ? ";
	String suSql = "SELECT  * FROM t_room_time  ";
	
	@Override
	public void getUseRoomList() {
		//如果监控服务器未登陆 将不在推送数据
		if(WebSocketIoHandler.sessionMap.isEmpty()){
			return ;
		}
		List<Map<String, Object>> querySqlList = this.getQuerySqlList(suSql);
		if(null!=querySqlList&&!querySqlList.isEmpty()) {
			for (Map<String, Object> map : querySqlList) {
				Room room=RoomTimer.useRooms.get(map.get("t_room_id"));
					if(room.getCoverLinkUserId() !=0 && room.getLaunchUserId() !=0){
						
						//得到发起人 和 被链接人的昵称
						Map<String, Object> lanMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql, room.getLaunchUserId());
						
						room.setLanuchName(getNickName(lanMap));
						
						Map<String, Object> covMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql, room.getCoverLinkUserId());
						
						room.setCoverName(getNickName(covMap));
						
						IoSession session = getIosession();
						 
						JSONObject fromObject = JSONObject.fromObject(room);
						fromObject.put("mid", 10003);
						
						del.put(room.getRoomId(), session);
						//发送到前端服务器
						WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
					}
			}
		}
		//遍历正在使用的房间
		
	} 
	
	
	/**返回用户昵称*/
	private String getNickName(Map<String, Object> map){
		if(null == map.get("t_nickName")){
			return "聊友:"+map.get("t_phone").toString().substring(map.get("t_phone").toString().length()-4);
		}else{
			return map.get("t_nickName").toString();
		}
	}



	/**
	 * 产生新的房间推送
	 */
	@Override
	public  void  singleRoomSend(Room room) {
		try {
			//如果监控服务器未登陆 将不在推送数据
			if(WebSocketIoHandler.sessionMap.isEmpty()){
				return ;
			}
			Thread.sleep(1000);
			//得到发起人 和 被链接人的昵称
//			logger.info("当前发起人-->{}",room.getLaunchUserId());
			if(null != room && room.getLaunchUserId() !=0 && room.getCoverLinkUserId() !=0){
				
				Map<String, Object> lanMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql, room.getLaunchUserId());
				
				room.setLanuchName(getNickName(lanMap));
				
				Map<String, Object> covMap = this.getFinalDao().getIEntitySQLDAO().findBySQLUniqueResultToMap(qSql, room.getCoverLinkUserId());
				
				room.setCoverName(getNickName(covMap));
				
				IoSession session = getIosession();
				
				del.put(room.getRoomId(), session);
				
				JSONObject fromObject = JSONObject.fromObject(room);
				
				fromObject.put("mid", 10003);
				
				logger.info("已发送到监控服务器->{}",fromObject.toString());
				//发送到前端服务器
				WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 挂断房间推送
	 */
	@Override
	public void stopRoomSend(int roomId) {
		try {
			//如果监控服务器未登陆 将不在推送数据
			
			if(WebSocketIoHandler.sessionMap.isEmpty()){
				return ;
			}
			
			//根据房间好取得session
			IoSession session = del.get(roomId);
			
			if(null != session){
				JSONObject fromObject = new JSONObject();
				fromObject.put("roomId", roomId);
				fromObject.put("mid", 10004);
				
				logger.info("{}房间关闭,推送给监控平台!",roomId);
				//发送到前端服务器
				WebSocketIoHandler.sendMessage(session, JSONArray.fromObject(fromObject).toString());
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	/**
	 * 获取Map中的 未发送的地址
	 * @return
	 */
	public IoSession getIosession(){
		
		for(Map.Entry<String, IoSession> m : WebSocketIoHandler.sessionMap.entrySet()){
			if(!arr.contains(m.getKey())){
				logger.info("获取的当前登陆[{}]",m.getKey());
				arr.add(m.getKey());
				return m.getValue();
			}
		}
		//如果以上设置都未返回 那么下面进行初始化操作
		
		for(Map.Entry<String, IoSession> m : WebSocketIoHandler.sessionMap.entrySet()){
			 arr.clear();
			 logger.info("获取的当前登陆[{}]",m.getKey());
			 arr.add(m.getKey());
			 return m.getValue();
		}
		return null;
	}

 

	/**
	 * 处理违规用户
	 */
	@Override
	public void handleIrregularitiesUser(int roomId, int userid) {
		try {
			//调用挂断直播设置
			
			videoChatService.breakLink(roomId, 3, userid);
			//调用用户的处理
			handleIllegalityUser(Integer.valueOf(userid));
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("监管平台处理违规用户异常!", e);
		}
	}
	
	
	/***
	 * 处理用户违规
	 * @param userId
	 */
	private void handleIllegalityUser(Integer userId){
		try {
			
			//统计用户的封号次数
			String banSql = "SELECT  t_hours FROM t_banned_setup WHERE t_count = (SELECT count(t_id)+1 FROM t_disable WHERE t_user_id = ?)";
			
			List<Map<String, Object>> banHours = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(banSql, userId);
			//判断用户的封号时间 如果为0 标示永久封号
			if(!banHours.isEmpty() && !"0".equals(banHours.get(0).get("t_hours"))){
				//设置封号时间
				String inSql = "INSERT INTO t_disable (t_user_id, t_disable_time, t_start_time, t_end_time, t_state) VALUES (?,?,?,?,0) ";
				double parseDoublIe = Double .parseDouble(banHours.get(0).get("t_hours").toString());
				Integer time = new Double(parseDoublIe).intValue();
				int bennedTime = new Double(parseDoublIe).intValue()*60;
				this.getFinalDao().getIEntitySQLDAO().executeSQL(inSql, userId,bennedTime,
						DateUtils.format(new Date(), DateUtils.FullDatePattern),
						DateUtils.format(DateUtils.afterHours(DateUtils.nowCal(), time), DateUtils.FullDatePattern)
				);
			}else {
				HashMap<String,Object> hashMap = new HashMap<String, Object>();
				hashMap.put("t_hours", "0");
				banHours.add(hashMap);
			} 
			
			//修改用户数据
			String upSql = "UPDATE t_user SET t_disable = ? WHERE t_id = ? ";
			
			this.getFinalDao().getIEntitySQLDAO().executeSQL(upSql, "0".equals(banHours.get(0).get("t_hours").toString())?2:1,userId);
			
			//给用发送激光推送
			//消息内容
			String message = "您因违反平台相关禁止内容规定."+("0".equals(banHours.get(0).get("t_hours").toString())?"且违反次数较多,将被进行永久封号,如有异议请联系相关平台客服.":
				"本次将封号"+banHours.get(0).get("t_hours")+"小时");
			
			
			GetOutOfLineRes gof= new GetOutOfLineRes();
			gof.setMid(Mid.getOutOfLineRes);
			gof.setMessage(message);
			
			//给用户发送推送
			MinaService minaService = (MinaService) SpringConfig.getInstance().getBean("minaServiceImpl");
			
			minaService.notice(userId, null, JSONObject.fromObject(gof).toString());
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void sendWarningToRoom(int roomId, int userId,String content) {
		try {
			 IoSession userIdIoSession = UserIoSession.getInstance().mapIoSesson.get(userId);
			JSONObject fromObject =new JSONObject();
			fromObject.put("mid",Mid.sendWarningToRoom);
			fromObject.put("roomId",roomId);
			fromObject.put("content",content);
			if (null != userIdIoSession) {
				userIdIoSession.write(fromObject.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	 }
}
