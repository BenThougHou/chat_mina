package com.yiliao.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yiliao.service.SystemService;
import com.yiliao.util.RedisUtil;

import net.sf.json.JSONObject;


@Service("systemService")
public class SystemServiceImpl extends ICommServiceImpl implements SystemService {
	
	@Autowired
	RedisUtil redisUtil;

	@Override
	public Map<String, Object> getSpreedTipsMsg() {
		try {
			//获取速配提示消息
			List<Map<String, Object>> spreedTipsMsgList = this.getQuerySqlList("SELECT t_spreed_hint FROM t_system_setup");
			
			if(!spreedTipsMsgList.isEmpty()) {
				return spreedTipsMsgList.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取缓存中的速配用户
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String,Object>> getSpeedList() {
		
		Set<String> keys = redisUtil.redisTemplate.keys("speed_user_*");
		
		List<Map<String,Object>> lmap = new ArrayList<Map<String,Object>>();
		
		for (String key : keys) {
			Map<String,Object> map = JSONObject.fromObject(redisUtil.get(key));
			lmap.add(map);
		}
		
		return lmap;
	}
	
	
	

}
