package com.yiliao.util;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.LoginService;

public class MinaThread extends Thread {

	Logger logger = LoggerFactory.getLogger(getClass());

	int userId;
	IoSession session;

	// 登陆后修改用户状态
	static LoginService loginService = null;
	static {
		loginService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
	}
	
	public MinaThread(int userId, IoSession session) {
		super();
		this.userId = userId;
		this.session = session;
	}

	@Override
	public void run() {
		loginService.socketOnLine(userId,session);
	}

}
