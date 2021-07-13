package com.yiliao.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.yiliao.domain.MessageEntity;
import com.yiliao.service.MessageService;
import com.yiliao.util.DateUtils;
import com.yiliao.util.MessageUtil;

/**
 * 消息服务层
 * 
 * @author Administrator
 * 
 */
@Service("messageService")
public class MessageServiceImpl extends ICommServiceImpl implements
		MessageService {
	/*
	 * 发送消息
	 * (non-Javadoc)
	 * @see com.yiliao.service.MessageService#pushMessage(com.yiliao.domain.MessageEntity)
	 */
	@Override
	public void pushMessage(MessageEntity entity) {
		try {
			
			//存储消息
			String messageSql = "INSERT INTO t_message (t_user_id, t_message_content, t_create_time, t_is_see) VALUES (?, ?, ?, ?);";
			
			this.executeSQL(messageSql, 
					entity.getT_user_id(),
					entity.getT_message_content(),
					DateUtils.format(entity.getT_create_time(), DateUtils.FullDatePattern),
					entity.getT_is_see());
			
//			this.applicationContext.publishEvent(new PushMesgEvnet(entity));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
