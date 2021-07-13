package com.yiliao.timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.VideoChatService;
import com.yiliao.util.SpringConfig;

/**
 * 
 * @author Administrator
 * 
 */
public class SocketDelayTimer {

	// 需要计时的用户
	/**
	 * map 中 需要存储的数据 用户ID:Key value:延时时长
	 */
	public static Map<Integer, Integer> socketDelayUser = new ConcurrentHashMap<Integer, Integer>();
	
	//用户挂断之后 储存延时用户记录
	public static Map<Integer, Integer> socketDelayUserLog = new ConcurrentHashMap<Integer, Integer>();
	Logger logger = LoggerFactory.getLogger(getClass());

	VideoChatService videoChatService = (VideoChatService) SpringConfig.getInstance().getBean("videoService");

	final int time = 40;

	/**
	 * 开始计时
	 */
	public void SocketDelayRun() {
		try {
//			logger.info("---进入了socket延迟---");
			socketDelayUser.forEach((k, v) -> {
				logger.info("--正在延迟计时{}秒--",v);
				if (v <= time) {
					// 加入计时
					socketDelayUser.put(k, v + 1);
				} else if (v > time) {
					Map<String, Integer> map = VideoTiming.timingUser.get(k);
					if(null != map && !map.isEmpty()) {
						// 挂断连接
						VideoTiming.clearUser.put(k, VideoTiming.timingUser.get(k));
					} else {
						logger.info("--获取{}的计时信息失败--");
					}
					socketDelayUser.remove(k);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("定时器计时异常!", e);
		}
	}

}
