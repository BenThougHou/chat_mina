package com.yiliao.timer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.OnLineRes;
import com.yiliao.domain.Room;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.service.VideoChatService;
import com.yiliao.service.impl.VideoChatServiceImpl;
import com.yiliao.util.Mid;
import com.yiliao.util.RedisUtil;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

public class SimulationVideoTimer {

	/**
	 * 需要清理的用户Map key :需要呼叫的用户对象 V: 存储虚拟主播消息和次数消息
	 */
	public static Map<Integer, Map<String, Object>> callUser = new ConcurrentHashMap<Integer, Map<String, Object>>();
	
	private RedisUtil redisUtil = (RedisUtil) SpringConfig.getInstance()
			.getBean("redisUtil");
	private PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance()
			.getBean("personalCenterService");
	private VideoChatService videoChatService = (VideoChatService) SpringConfig.getInstance()
			.getBean("videoService");
	
	public void run() {

		LoggerFactory.getLogger(getClass()).info("开始循环发送模拟呼叫用户..");
		LoggerFactory.getLogger(getClass()).info(" 虚拟主播对象 "+callUser.toString());
		Iterator<Map.Entry<Integer, Map<String, Object>>> it = callUser.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Map<String, Object>> entry = it.next();
			if(entry.getValue().get("t_is_not_disturb").toString().equals("0")) {
				it.remove();
				continue;
			}
			//获取用户聊天状态 若在聊则不进行虚拟主播推送 
			boolean userChatStatus = personalCenterService.getUserChatStatus(entry.getKey());
			if(!userChatStatus) {
				it.remove();
				continue;
			}
			// 获取推送次数
			int callCount = Integer.parseInt(entry.getValue().get("callCount").toString());
			
			List<Integer> sendTime = (List<Integer>) entry.getValue().get("sendTime");
			
			List<Integer> arr = (List<Integer>) entry.getValue().get("anchor");
			if(sendTime.isEmpty()||arr.isEmpty()) {
				it.remove();
				return;
			}
			
			// 获取当前时间戳减去上次推送的时间戳得到差
			Long time = System.currentTimeMillis() - Long.valueOf(entry.getValue().get("time").toString());

			System.out.println("时间差为->" + time);

			System.out.println("当前推送次数-->" + (callCount+1));

			System.out.println("应该推送的时间差为-->" + (callCount+1)  * 1000);
			
			// 如果已经发送了2次那么清理掉已经发送的模拟呼叫
			if (callCount > 30) {
				it.remove();
				continue;
				// 当前时间减去上次发送的时间 必须大于或者等于 次数*60*1000
			} else if (time >= sendTime.get(0) * 1000) {
				if(sendTime.isEmpty()) {
					it.remove();
					continue;
			}

				System.out.println("--开始发起模拟消息--当前第"+(callCount+1)+"次");
				
				// 给用户发起模拟视频消息
				OnLineRes or = new OnLineRes();
				or.setMid(Mid.anchorLinkUserRes);
				or.setRoomId(1000001);
				if(arr.isEmpty()) {
					System.out.println("----暂无主播---");
					it.remove();
					continue;
				}
				or.setConnectUserId(arr.get(0));
				or.setSatisfy(-1);
				redisUtil.setTime("VIP_USER_"+arr.get(0)+"_"+entry.getKey(),"1",35000l);
				IoSession ioSession = UserIoSession.getInstance().getMapIoSession(entry.getKey());

				if (null != ioSession) {
					redisUtil.remove("BALANCE_USER_ID_"+entry.getKey());
//					log.info("socket 发送："+JSONObject.fromObject(or).toString());
					ioSession.write(JSONObject.fromObject(or));
				}
				
				// 清理掉第一个主播
				arr.remove(0);
				sendTime.remove(0);
				entry.getValue().put("anchor", arr);
				// 发起次数加1
				entry.getValue().put("callCount", callCount + 1);
				// 更新发起时间
				entry.getValue().put("time", System.currentTimeMillis());

			}

		}

	}

}
