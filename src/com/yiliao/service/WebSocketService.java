package com.yiliao.service;

import com.yiliao.domain.Room;

public interface WebSocketService {
	
	/**
	 * 获取已使用的房间列表
	 */
	void getUseRoomList();
	
	/**
	 * 通知监控服务器 有用户连麦
	 * @param room
	 */
	void singleRoomSend(Room room);
	
	/**通知监控服务器 用户已挂断连接 **/
	void stopRoomSend(int roomId);
	
	/**
	 * 处理违规用户
	 * @param roomId
	 * @param userid
	 */
	void handleIrregularitiesUser(int roomId,int userid);
	
	/**
	 * 处理违规用户 推送警告文字
	 * @param roomId
	 * @param userid
	 */
	void sendWarningToRoom(int roomId,int userid,String content);
	
}
