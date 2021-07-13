package com.yiliao.timer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.SystemService;
import com.yiliao.util.HttpUtil;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

public class SendTipsMsg {

	private Map<String, Object> spreedTipsMsgMap = null;

	private SystemService systemService = (SystemService) SpringConfig.getInstance().getBean("systemService");

	/**
	 * 推送给速配
	 */
	public void sendSpreedTipsMsg() {

//		System.out.println(DateUtils.format(new Date(), DateUtils.FullDatePattern));
//		System.out.println("给速配推送消息");
		// 如果提示
		try {
			if (null == spreedTipsMsgMap || spreedTipsMsgMap.isEmpty()) {
				spreedTipsMsgMap = systemService.getSpreedTipsMsg();
			}
			
			// 在次判断提示信息是否存在
			if (!spreedTipsMsgMap.isEmpty()) {
				// 获取速配用户列表
				List<Map<String, Object>> speedList = systemService.getSpeedList();
				speedList.forEach(s -> {
					// 获取当前用户的session
					IoSession userIoSession = UserIoSession.getInstance()
							.getMapIoSession(Integer.parseInt(s.get("t_id").toString()));
					if (null != userIoSession) {
						userIoSession.write(JSONObject.fromObject(new HashMap<String, Object>() {
							private static final long serialVersionUID = 1L;
							{
								put("mid", Mid.sendSpreedTipsMsg);
								put("msgContent", spreedTipsMsgMap.get("t_spreed_hint"));
							}
						}));
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		
	}

}
