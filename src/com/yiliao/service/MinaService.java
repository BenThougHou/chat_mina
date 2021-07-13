package com.yiliao.service;

import java.util.Map;

public interface MinaService {

	/**
	 * 下发通知
	 * @param userId
	 * @param userId1
	 * @param mid
	 */
	void notice(Integer userId,Integer userId1,String content);
	
	/**
	 * 登出
	 * @param userId
	 */
	void logout(int userId);
	
	/**
	 * 视频聊天计费
	 */
	void videoTime(int userId,Map<String, Integer> content);
	
	
}
