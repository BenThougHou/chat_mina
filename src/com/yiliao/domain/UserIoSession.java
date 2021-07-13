package com.yiliao.domain;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.LoginService;
import com.yiliao.timer.SocketDelayTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.util.SpringConfig;

public class UserIoSession {
	
	Logger logger = LoggerFactory.getLogger(getClass());

	private static UserIoSession instance = new UserIoSession();
	/** 存储用户所有的iosession*/
	public Map<Integer, IoSession> mapIoSesson = new ConcurrentHashMap<Integer, IoSession>();

	/** 登陆用户 */
	public Map<Integer, LoginInfo> loginUserMap = new ConcurrentHashMap<Integer, LoginInfo>();

	/** 登陆主播 */
	public Map<Integer, LoginInfo> loginGirlAnchorMap = new ConcurrentHashMap<Integer, LoginInfo>();
	/** 登陆男主播 */
	public Map<Integer, LoginInfo> loginMaleAnchorMap = new ConcurrentHashMap<Integer, LoginInfo>();

	private UserIoSession() {
	}

	public static UserIoSession getInstance() {
		return instance;
	}

	// 装载用户的连接池
	public void putMapIoSesson(int key, IoSession value) {
		this.mapIoSesson.put(key, value);
	}

	/**
	 * 获取用户链接池
	 * @param key
	 * @return
	 */
	public IoSession getMapIoSession(int key) {
		return this.mapIoSesson.get(key);
	}
	
	LoginService  loginAppService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
	/**
	 * 删除用户链接
	 * @param ioSession
	 */
	public void delMapIoSession(IoSession ioSession){
		
		int userId = 0;
		Iterator<Map.Entry<Integer, IoSession>> it = mapIoSesson.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, IoSession> entry = it.next();
			if (entry.getValue().getId() == ioSession.getId()) {
				userId = entry.getKey();
				it.remove();// 使用迭代器的remove()方法删除元素
				logger.info("当前用户已关闭客户端-->{},session->{}", userId, ioSession);
				loginAppService.socketBreak(userId);

				List<Integer> byUserResRoom = VideoTiming.getByUserResRoom(entry.getKey());
				
				if(null != byUserResRoom && !byUserResRoom.isEmpty()) {
					
					byUserResRoom.forEach(s -> {
						Room room = VideoTiming.getRoom(s);
						
						// 如果该用户是在聊天计时 那么加入到延迟挂断任务
						Map<String, Integer> map = VideoTiming.timingUser.get(room.getRoomId());
						
						if (null != map && !map.isEmpty()) {
							// 加入到计时任务中
							SocketDelayTimer.socketDelayUser.put(room.getRoomId(), 0);
							SocketDelayTimer.socketDelayUserLog.put(entry.getKey(), room.getRoomId());
						}else {
							logger.info("无法获取计时数据!");
						}
					});
				}
			}
		}
	}

	/**
	 * 存储登陆主播
	 * 
	 * @param key
	 * @param loginInfo
	 */
	public void putLoginMaleAnchorMap(int key, LoginInfo loginInfo) {

		this.loginGirlAnchorMap.put(key, loginInfo);
	}

	/** 获取登陆主播 */
	public LoginInfo getLoginMaleAnchorMap(int key) {

		return this.loginGirlAnchorMap.get(key);
	}

	/**
	 * 存储登陆主播
	 * @param key
	 * @param loginInfo
	 */
	public void putLoginGirlAnchorMap(int key, LoginInfo loginInfo) {

		this.loginGirlAnchorMap.put(key, loginInfo);
	}

	/** 获取登陆主播 */
	public LoginInfo getLoginGirlAnchorMap(int key) {

		return this.loginGirlAnchorMap.get(key);
	}
	
	
	/**
	 * 存储登陆用户
	 * 
	 * @param key
	 * @param loginInfo
	 */
	public void putLoginUserMap(int key, LoginInfo loginInfo) {

		this.loginUserMap.put(key, loginInfo);
	}

	/** 获取登陆用户 */
	public LoginInfo getLoginUserMap(int key) {

		return this.loginUserMap.get(key);
	}
 

	public Map<Integer, LoginInfo> getLoginUserMap() {
		return loginUserMap;
	}

	public void setLoginUserMap(Map<Integer, LoginInfo> loginUserMap) {
		this.loginUserMap = loginUserMap;
	}
	

	public Map<Integer, IoSession> getMapIoSesson() {
		return mapIoSesson;
	}

	public void setMapIoSesson(Map<Integer, IoSession> mapIoSesson) {
		this.mapIoSesson = mapIoSesson;
	}

	public Map<Integer, LoginInfo> getLoginGirlAnchorMap() {
		return loginGirlAnchorMap;
	}

	public Map<Integer, LoginInfo> getLoginMaleAnchorMap() {
		return loginMaleAnchorMap;
	}
	
	

}
