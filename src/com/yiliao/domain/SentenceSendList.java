package com.yiliao.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SentenceSendList {
	
	Logger logger = LoggerFactory.getLogger(getClass());

	private static SentenceSendList instance = new SentenceSendList();
	

	/** 发送虚拟文字消息  */
	public Map<Integer, List<Map<String, Object>>> sendTenceUserMap = new ConcurrentHashMap<Integer, List<Map<String, Object>>>();


	private SentenceSendList() {
	}

	public static SentenceSendList getInstance() {
		return instance;
	}
	
	/**
	 * 存储登陆用户
	 * 
	 * @param key
	 * @param loginInfo
	 */
	public void putSendTenceUserMap(int key, List<Map<String, Object>> loginInfo) {

		this.sendTenceUserMap.put(key, loginInfo);
	}

	/** 获取登陆用户 */
	public List<Map<String, Object>> getSendTenceUserMap(int key) {

		return this.sendTenceUserMap.get(key);
	}
 

	public Map<Integer, List<Map<String, Object>>> getSendTenceUserMap() {
		return sendTenceUserMap;
	}

	public void setSendTenceUserMap(Map<Integer, List<Map<String, Object>>> loginUserMap) {
		this.sendTenceUserMap = loginUserMap;
	}

	
	
	

}
