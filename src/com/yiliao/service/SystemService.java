package com.yiliao.service;

import java.util.List;
import java.util.Map;

public interface SystemService {

	/**
	 * 获取速配提示消息
	 * @return
	 */
	Map<String, Object>  getSpreedTipsMsg();
	
	/**
	 * 获取速配列表
	 * @return
	 */
	List<Map<String,Object>> getSpeedList();
		
	
}
