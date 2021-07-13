package com.yiliao.timer;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.SentenceSendList;
import com.yiliao.domain.UserIoSession;
import com.yiliao.mina.res.ActiveMsgRes;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;

/**
 * 
 * @项目工程名
 * @Module ID <(模块)类编号，可以引用系统设计中的类编号> Comments <对此类的描述，可以引用系统设计中的描述>
 * @JDK 版本(version) JDK1.6.45
 * @命名空间 com.yiliao.util
 * @作者(Author) 石德文
 * @创建日期 2016年3月15日 上午9:50:05
 * @修改人
 * @修改时间 <修改日期，格式:YYYY-MM-DD> 修改原因描述：
 * @Version 版本号 V1.0
 * @类名称
 * @描述 (发送模拟消息)
 */

public class SendFictitiousMsgTimer {

	Logger logger = LoggerFactory.getLogger(SendFictitiousMsgTimer.class);

	private PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance()
			.getBean("personalCenterService");
	private RedisUtil redisUtil = (RedisUtil) SpringConfig.getInstance()
			.getBean("redisUtil");
	
	
	
	
	/**
	 * 修改虚拟文字消息历史发送记录
	 * @throws Exception
	 */
	public void updSendtenceList() throws Exception {
		personalCenterService.updSendtenceList();
	}
	
	/**
	 * @方法名 delSmsCode
	 * @说明 (给用户推送模拟消息)
	 * @param 参数 设定文件
	 * @return void 返回类型
	 * @作者 石德文
	 * @throws 异常
	 */
	public void sendFictitiousMsg() throws Exception {
		logger.info("--开始执行发送模拟消息方法--");
		// 迭代所有已经登陆的用户
		for (Entry<Integer, List<Map<String, Object>>> m : SentenceSendList.getInstance().sendTenceUserMap.entrySet()) {
			// 语术 主播列表
			List<Map<String,Object>> sentenceList =m.getValue();
			//遍历所有语术Id
			if(sentenceList.size()>0) {
				 ListIterator<Map<String, Object>> listIterator = sentenceList.listIterator();
				 while (listIterator.hasNext()) {
					 Map<String, Object> sentenceMap = listIterator.next();
					Integer t_sentence_id = (Integer)sentenceMap.get("t_sentence_id");
					//1.查询语术列表
					List<Map<String,Object>> sendSentenceList = personalCenterService.getSentenceList(t_sentence_id);
					if(sendSentenceList!=null && !sendSentenceList.isEmpty()) {
						Integer sentence_sort = Integer.parseInt(sentenceMap.get("sentence_sort").toString());
						
						if(sendSentenceList.size()<sentence_sort+1) {
							listIterator.remove();
							continue;
						}
						
						Map<String, Object> map = sendSentenceList.get(sentence_sort);
						//获取当前待发送语术  
						if(map!=null&&Integer.parseInt(map.get("t_sentence_time").toString()) * 1000 
								+ Long.parseLong(sentenceMap.get("sendTime").toString()) 
								<= System
								.currentTimeMillis()) {
							
							//判断当前用户开关
							LoginInfo loginInfo = UserIoSession.getInstance().loginUserMap.get(m.getKey());
							if(loginInfo==null) {
								listIterator.remove();
								continue;
							}else {
								int t_sentence_type = Integer.parseInt(map.get("t_sentence_type").toString());
								if(t_sentence_type==4) {
									//视频开关   为0  代表没有打开视频开关  
									if(loginInfo.getT_voide_switch()==0){
										listIterator.remove();
										continue;
									}
								}else if(t_sentence_type==5) {
									//语音视频   为0  代表没有打开视频开关  
									if(loginInfo.getT_voice_switch()==0){
										listIterator.remove();
										continue;
									}
								}else {
									if(loginInfo.getT_text_switch()==0){
										listIterator.remove();
										continue;
									}
								}
							}
							
							
							//发送消息
							personalCenterService.sendTXUserIM(m.getKey(),Integer.parseInt(sentenceMap.get("t_anchor_id").toString())
									,Integer.parseInt(map.get("t_sentence_type").toString())
									,map.get("t_sentence_content").toString(),map.get("t_sentence_param").toString()); 
							//发送消息完成之后 下标+1
							sentenceMap.put("sentence_sort", sentence_sort+1);
							sentenceMap.put("sendTime", System.currentTimeMillis());
							if(sentence_sort==0) {
								//第一次发送的时候加入已发送队列
								personalCenterService.updSentenceSendedList(m.getKey(),sentenceMap.get("t_anchor_id").toString());
							} 
						}
					}else {
						listIterator.remove();
					}
				}
			}
			
			
//			if (!m.getValue().getTimes().isEmpty()) {
//				// 上次发送的时间加上间隔分钟数 小雨或者等于了当前时间 准备给用户推送消息
//				if ((m.getValue().getTimes().get(0) * 1000 + m.getValue().getLoginTime()) <= System
//						.currentTimeMillis() && m.getValue().getT_text_switch() == 1) {
//					logger.info("当前模拟消息用户->{}",JSONObject.fromObject(m).toString());
//					// 当用户为女用户时 使用男主播给用户推送
//					if (m.getValue().getT_sex() == 0) {
//						logger.info("--进入了女用户模拟消息--");
//						logger.info("当前模拟消息用户->{}",JSONObject.fromObject(m).toString());
//						// 循环虚拟主播
//						//虚拟主播推送开关 0 关闭 1打开
//						String t_false_send_text_switch = redisUtil.get("t_false_send_text_switch");
//						//真实主播推送开关 0 关闭 1打开
//						String t_real_send_text_switch = redisUtil.get("t_real_send_text_switch");
//						
//						List<Integer> anchor = m.getValue().getAnchor();
//						int userId = m.getValue().getUserId();
//							//是VIP
//							int count =0;
//							if(t_real_send_text_switch!=null&&t_real_send_text_switch.equals("1")) {
//								for (Map.Entry<Integer, LoginInfo> anch : UserIoSession.getInstance().loginMaleAnchorMap.entrySet()) {
//									logger.info("--进入了男用户模拟消息-- 有女用户在线");
//									if (!m.getValue().getAnchor().contains(anch.getValue().getUserId())) {
//										//黑名单查询 若返回true 则取消当前发送
//										//黑名单判断 true 被拉入黑名单
//										boolean flag=personalCenterService.checkBlackUserInfo(anch.getValue().getUserId(),userId);
//										if(flag) {
//											m.getValue().getAnchor().add(anch.getValue().getUserId());
//											return  ;
//										}
//										logger.info("--进入了男用户模拟消息-- 女主播开始发消息");
//										Integer anchUserId=anch.getValue().getUserId();
//										personalCenterService.sendTXUserIM(userId,anchUserId,0,0);
//										m.getValue().getAnchor().add(anch.getValue().getUserId());
//										count++;
//										return ;
//									}
//								}
//								if(count==0) {
//									if(m.getValue().getT_is_vip()!=0&&!anchor.isEmpty()) {
//										logger.info("--进入了男用户模拟消息-- 女主播不在 开始虚拟消息");
//										Integer anchUserId=anchor.get(0);
//										boolean flag=personalCenterService.checkBlackUserInfo(anchUserId,userId);
//										if(flag) {
//											m.getValue().getAnchor().remove(0);
//											return  ;
//										}
//										// 把当前主播加载的已经给用户发送过消息的主播里面
//										personalCenterService.sendTXUserIM(userId,anchUserId,0,1);
//										m.getValue().getAnchor().remove(0);
//									}
//								}
//							}
//						if(t_false_send_text_switch!=null&&t_false_send_text_switch.equals("1")) {
//							logger.info("--进入了男用户模拟消息-- 当前用户不不不是VIP");
//							//不是VIP
//							if(m.getValue().getT_is_vip()!=0&&count==0&&!anchor.isEmpty()) {
//								logger.info("--进入了男用户模拟消息-- 当前用户不不不是VIP  开始发虚拟消息");
//								Integer anchUserId=anchor.get(0);
//								boolean flag=personalCenterService.checkBlackUserInfo(anchUserId,userId);
//								if(flag) {
//									m.getValue().getAnchor().remove(0);
//									return  ;
//								}
//								// 把当前主播加载的已经给用户发送过消息的主播里面
//								personalCenterService.sendTXUserIM(userId,anchUserId,0,1);
//								m.getValue().getAnchor().remove(0);
//							}
//						}
//					m.getValue().getTimes().remove(0);
//					m.getValue().setLoginTime(System.currentTimeMillis());
//				} else {
//						logger.info("--进入了男用户模拟消息--");
//						logger.info("当前模拟消息用户->{}",JSONObject.fromObject(m).toString());
//						// 循环虚拟主播
//						//虚拟主播推送开关 0 关闭 1打开
//						String t_false_send_text_switch = redisUtil.get("t_false_send_text_switch");
//						//真实主播推送开关 0 关闭 1打开
//						String t_real_send_text_switch = redisUtil.get("t_real_send_text_switch");
//						
//						List<Integer> anchor = m.getValue().getAnchor();
//						int userId = m.getValue().getUserId();
//							//是VIP
//							int count =0;
//							if(t_real_send_text_switch!=null&&t_real_send_text_switch.equals("1")) {
//								for (Map.Entry<Integer, LoginInfo> anch : UserIoSession.getInstance().loginGirlAnchorMap.entrySet()) {
//									logger.info("--进入了男用户模拟消息-- 有女用户在线");
//									if (!m.getValue().getAnchor().contains(anch.getValue().getUserId())) {
//										logger.info("--进入了男用户模拟消息-- 女主播开始发消息");
//										Integer anchUserId=anch.getValue().getUserId();
//										//黑名单判断 true 被拉入黑名单
//										boolean flag=personalCenterService.checkBlackUserInfo(anchUserId,userId);
//										if(flag) {
//											m.getValue().getAnchor().add(anchUserId);
//											return  ;
//										}
//										personalCenterService.sendTXUserIM(userId,anchUserId,1,0);
//										m.getValue().getAnchor().add(anch.getValue().getUserId());
//										count++;
//										return ;
//									}else {
//										logger.info("--进入了男用户模拟消息-- 有女用户在线");
//									}
//								}
//								if(count==0) {
//									if(m.getValue().getT_is_vip()!=0&&!anchor.isEmpty()) {
//										logger.info("--进入了男用户模拟消息-- 女主播不在 开始虚拟消息");
//										Integer anchUserId=anchor.get(0);
//										boolean flag=personalCenterService.checkBlackUserInfo(anchUserId,userId);
//										if(flag) {
//											m.getValue().getAnchor().remove(0);
//											return  ;
//										}
//										// 把当前主播加载的已经给用户发送过消息的主播里面
//										personalCenterService.sendTXUserIM(userId,anchUserId,1,1);
//										m.getValue().getAnchor().remove(0);
//										count++;
//									}
//								}
//							}
//						if(t_false_send_text_switch!=null&&t_false_send_text_switch.equals("1")) {
//							logger.info("--进入了男用户模拟消息-- 当前用户不不不是VIP");
//							//不是VIP
//							if(m.getValue().getT_is_vip()!=0&&count==0&&!anchor.isEmpty()) {
//								logger.info("--进入了男用户模拟消息-- 当前用户不不不是VIP  开始发虚拟消息");
//								Integer anchUserId=anchor.get(0);
//								boolean flag=personalCenterService.checkBlackUserInfo(anchUserId,userId);
//								if(flag) {
//									m.getValue().getAnchor().remove(0);
//									return  ;
//								}
//								// 把当前主播加载的已经给用户发送过消息的主播里面
//								personalCenterService.sendTXUserIM(userId,anchUserId,1,1);
//								m.getValue().getAnchor().remove(0);
//							}
//						}
//					m.getValue().getTimes().remove(0);
//					m.getValue().setLoginTime(System.currentTimeMillis());
//				}
//					m.getValue().setLoginTime(System.currentTimeMillis());
//				}
//			}
		}
	}

}
