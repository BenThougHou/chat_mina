package com.yiliao.service;

import com.yiliao.util.MessageUtil;

public interface VIPService {
	/**
	 * 修改所有已到期的VIP用户状态
	 * @return
	 */
	public void updateVIPExpire();
	
	
	/**
	 * 修改虚拟主播状态
	 */
	public void updateVirtualState();
}
