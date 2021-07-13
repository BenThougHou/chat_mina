package com.yiliao.service;

import java.util.Map;

import com.yiliao.util.MessageUtil;

public interface VideoChatService {
	/**
	 * 获取速配房间号
	 * @param userId
	 * @return
	 */
	MessageUtil getSpeedDatingRoom(int userId);
	/**
	 * 发起视频聊天
	 * @param launchUserId
	 * @param coverLinkUserId
	 * @param roomId
	 * @return
	 */
	public MessageUtil launchVideoChat(int launchUserId,int coverLinkUserId,int roomId,int chatType);
	
	
	/**
	 * 聊天开始计时
	 * @param anthorId
	 * @param userId
	 * @param roomId
	 * @return
	 */
	public MessageUtil videoCharBeginTiming(int anthorId,int userId,int roomId,int chatType);
	
	/**
	 * 挂断链接
	 * @param userId
	 * @param roomId
	 * @return
	 */
	public MessageUtil breakLink(int roomId,int type,int breakUserId);
	/**
	 * 用户申请挂断
	 * @param userId
	 * @return
	 */
	public MessageUtil userHangupLink(int userId);
	
	/**
	 * 主播对用户发起聊天
	 * @param anchorUserId
	 * @param userId
	 * @return
	 */
	public MessageUtil anchorLaunchVideoChat(int anchorUserId,int userId,int roomId,int chatType);
	
	/**
	 * 获取当前用户是否被呼叫
	 * @param userId
	 * @return
	 */
	public Map<String, Object> getUuserCoverCall(int userId);
	
	/**
	 * 获取改房间状态
	 * @param roomId
	 * @return
	 */
	public MessageUtil getRoomState(int roomId);
	
	/**
	 *  用户充值金币
	 * @param userId 用户编号
	 * @param gold  充值金币
	 */
	public void addRoomRate(int userId,int gold);
	
	/**
	 * 用户消费金币
	 * @param userId
	 * @param gold
	 * @return
	 */
	MessageUtil  userBuyGold(int userId,int gold);
	
	/**
	 * 获取用户是否正在聊天中
	 * @param userId
	 * @return
	 */
	MessageUtil getUserIsVideo(int userId);
	int saveCallLog(int userId, int coverUserId, int roomId);
	MessageUtil breakAllVideoUser();

}
