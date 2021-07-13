package com.yiliao.service;

import org.apache.mina.core.session.IoSession;

public interface LoginService {
	 
	/**
	 * socket 断开  修改用户状态
	 * @param userId
	 */
	public void socketBreak(int userId);
 
	/**
	 * socket 链接
	 * @param userId
	 */
	public void socketOnLine(int userId,IoSession session);
	
	/**
	 * 随机获取一条模拟消息
	 * @return
	 */
	String getSimulationMsg(int sex);
	
	/**
	 * 启动修改用户状态
	 */
	public void startUpOnLine();

}
