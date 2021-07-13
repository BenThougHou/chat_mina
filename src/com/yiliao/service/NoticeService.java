package com.yiliao.service;

import com.yiliao.util.MessageUtil;

public interface NoticeService {
	/**
	 * 后台调用通知
	 * @param userId
	 * @param type
	 */
	void sendSocketNotice(int userId);
	
	/**
	 * 发送红包通知
	 * @param userId
	 */
	void sendRedPackegNotice(int userId);
	
	/**
	 * 挂断视频
	 * @param userId
	 */
	void sendNoticeBreakVideo(int userId);
	
	/**
	 * 通知用户违规
	 * @param userId
	 */
	void sendUserViolation(int userId);
	
	/**
	 * 获取所有在线用户
	 * @return
	 */
	MessageUtil getOnLineUser();
	

}
