package com.yiliao.service;

import com.yiliao.domain.MessageEntity;

public interface MessageService {
    /**
     * 发送消息
     * @param entity
     */
    public void  pushMessage(MessageEntity entity);
}
