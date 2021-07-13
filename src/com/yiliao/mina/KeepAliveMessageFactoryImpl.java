package com.yiliao.mina;

import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.UserIoSession;

/**
 * mina 心跳包
 * 
 * @author Administrator
 * 
 */
public class KeepAliveMessageFactoryImpl implements KeepAliveMessageFactory {

	Logger logger = LoggerFactory.getLogger(getClass());
	/** 心跳包内容 */
	private static final String HEARTBEATREQUEST = "0x11";// 预设请求内容
	/** 心跳返回类容 */
	private static final String HEARTBEATRESPONSE = "01010";// 预设应答内容（记得客户端在接收到预设请求

	@Override
	public boolean isRequest(IoSession ioSession, Object o) {
		if (o.toString().equals(HEARTBEATREQUEST)&&isUserExistSession(ioSession,o)){
			
			return true;
		}
		return false;
	}
	
	boolean isUserExistSession(IoSession session, Object o) {
			
			Map<Integer, IoSession> mapIoSesson = UserIoSession.getInstance().getMapIoSesson();
			
			for(Map.Entry<Integer, IoSession> m : mapIoSesson.entrySet()) {
				if(m.getValue().getId() == session.getId()) {
//					logger.info("用户编号-{},sessionId-{},请求内容-{}",m.getKey(),m.getValue().getId(),o.toString());
					return true;
				}
			}
			return false;
	}

	@Override
	public boolean isResponse(IoSession session, Object message) {
		if (message.toString().equals(HEARTBEATRESPONSE)&&isUserExistSession(session, message)){
			return true;
		}
		return false;
	}

	@Override
	public Object getRequest(IoSession session) {
		 
		return HEARTBEATREQUEST;
	}

	@Override
	public Object getResponse(IoSession session, Object request) {
		return HEARTBEATRESPONSE;
		// return null;
	}

}
