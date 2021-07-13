package com.yiliao.service;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author Administrator
 * 个人中心
 */
public interface PersonalCenterService {
	
	
	/**
	 * 定时器解封
	 */
	public void timerUnseal();
	
	/**
	 * 随机获取一条模拟消息
	 * @return
	 */
	String getSimulationMsg(int sex);
	
	/**
	 *  半小时更新一次浏览人
	 */
	public void setUpBrower(int userId,int sex,int count);

	
	/**
	 *  获取虚拟主播
	 */
	public List<Integer>  getVirtualList();
	Map<String, Object> getUserTextSwitch(int userId);
	

	/**
	 * 发送虚拟消息
	 * @param userId 用户Id 
	 * @param anchUserId 主播Id
	 * @param t_sentence_type 消息类型
	 * @param t_sentence_content 消息内容
	 */
	public void sendTXUserIM(Integer userId,Integer anchUserId,int t_sentence_type,String t_sentence_content,String duration);
	
	/**
	 * 获取用户聊天状态 若在聊则不进行虚拟主播推送 
	 * 
	 * */	
	public boolean getUserChatStatus(Integer key);

	/**
	 * 黑名单查询
	 * @param userId 查询人
	 * @param coverUserId 被拉入黑名单人
	 * @return
	 */
	public Boolean checkBlackUserInfo(int userId, int coverUserId);

	public List<Map<String,Object>> getVirtualSentenceList(int type,int userId,String number);

	public List<Map<String,Object>> getSentenceList(Integer t_sentence_id);

	/**
	 * @param launchUserId 用户
	 * @param coverLinkUserId  主播
	 * @param msgMap 信息
	 */
	public void sendTXUserVoideIM(int launchUserId, int coverLinkUserId, Map<String, Object> msgMap);

	public void updSendtenceList();

	public void updSentenceSendedList(Integer userId,String anoUserId);

	public Map<String, Object> getUserGoldSetInfo(); 
	
	
}
