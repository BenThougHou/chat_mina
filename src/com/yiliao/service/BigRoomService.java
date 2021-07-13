package com.yiliao.service;

import java.util.Map;

import com.yiliao.util.MessageUtil;

public interface BigRoomService {
	
	/**
	 * 用户加入直播房间
	 * @param userId
	 * @param anchorId
	 * @return
	 */
	MessageUtil userMixBigRoom(int userId,int anchorId);
	
	/**
	 * 用户退出房间
	 * @param userId
	 * @return
	 */
	MessageUtil userQuitBigRoom(int userId);
	
	/**
	 * 给用户下发当前房间中的总人数
	 * @param roomId
	 */
	void sendBigUserCount(Map<String, Object> map);
	/**
	 * 关闭直播
	 * @param userId
	 * @return
	 */
	MessageUtil closeLiveTelecast(int userId,int type);
	 
}
