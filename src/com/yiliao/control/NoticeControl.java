package com.yiliao.control;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.OnLineRes;
import com.yiliao.domain.Room;
import com.yiliao.domain.SentenceSendList;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.NoticeService;
import com.yiliao.service.ProhibitService;
import com.yiliao.service.WebSocketService;
import com.yiliao.timer.SimulationVideoTimer;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SystemConfig;

import net.sf.json.JSONObject;

/**
 * 动态控制层
 * 
 * @author Administrator
 *
 */
@Controller
@RequestMapping("app")
public class NoticeControl {

	@Autowired
	private NoticeService noticeService;
	
	@Autowired
	private WebSocketService webSocketService;
	@Autowired
	private ProhibitService  prohibitService;

	/**
	 * 发送通知
	 * 
	 * @param userId
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	@RequestMapping(value = { "sendSocketNotice" }, method = { RequestMethod.POST })
	@ResponseBody
	public MessageUtil sendSocketNotice(int userId) {

		this.noticeService.sendSocketNotice(userId);
		
		return new MessageUtil(1,"");
	}

	/**
	 * 发送红包通知
	 * 
	 * @param userId
	 */
	@RequestMapping(value = { "sendRedPackegNotice" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil sendRedPackegNotice(int userId) {

		noticeService.sendRedPackegNotice(userId);
		
		return new MessageUtil(1,"");
	}
	
	/**
	 * 通知用户端 挂断模拟视频
	 * @param userId
	 */
	@RequestMapping(value= {"sendNoticeBreakVideo"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil sendNoticeBreakVideo(int userId) {
		
		noticeService.sendNoticeBreakVideo(userId);
		
		return new MessageUtil(1,"");
	}

	
	/**
	 * 用户违规
	 * @param userId
	 */
	@RequestMapping(value= {"sendUserViolation"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil sendUserViolation(int userId) {
		
		noticeService.sendUserViolation(userId);
		
		return new MessageUtil(1,"");
	}
	
	/**
	 * 获取实时在线用户
	 * @return
	 */
	@RequestMapping(value= {"getOnLineUser"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil getOnLineUser() {
		
		return noticeService.getOnLineUser();
	}
	
	
	/**
	 * 大房间监控
	 * @param userId
	 * @return
	 */
	@RequestMapping(value= {"sendWebSocket"})
	@ResponseBody
	public MessageUtil sendWebSocket(int userId) {
		
		Room room = new Room((userId+10000)*100);
		room.setLaunchUserId(userId);
		room.setLaunchUserLiveCode(SystemConfig.getValue("play_addr") + userId + "/" + room.getRoomId());
		
		webSocketService.singleRoomSend(room);
		
		return new MessageUtil(1, "");
	}
	
	/**
	 * 	礼物 充值 会员全站推送
	 * @param content
	 * @return
	 */
	@RequestMapping(value= {"sendSocketMsg"})
	@ResponseBody
	public MessageUtil sendSocketMsg(String t_nickName,String t_handImg,String t_cover_nickName,
			String t_cover_handImg,String sendType,String t_gift_still_url,String mid,
			String recharge) {
		try {
		Map<Integer, IoSession> mapIoSesson = UserIoSession.getInstance().mapIoSesson;
		Set<Integer> keySet = mapIoSesson.keySet();
		JSONObject fromObject =new JSONObject();
		fromObject.put("t_nickName",t_nickName);
		fromObject.put("t_handImg",t_handImg);
		fromObject.put("t_cover_nickName",t_cover_nickName);
		fromObject.put("t_cover_handImg",t_cover_handImg);
		fromObject.put("sendType",sendType);
		fromObject.put("t_gift_still_url",t_gift_still_url);
		fromObject.put("mid",mid);
		fromObject.put("recharge",recharge);
		String string = fromObject.toString().replace("\\r\\n", "");
		string = string.replace("\\n", "");
		for (Integer key : keySet) {
			IoSession ioSession = mapIoSesson.get(key);
			if (null != ioSession) {
				ioSession.write(string);
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
 return new MessageUtil(1, "");
 }
	/**
	 * 	用户设置视频 语音 文字开关 同步修改米娜登录参数
	 * @param content
	 * @return
	 */
	@RequestMapping(value= {"setUpLoginUserSwitch"})
	@ResponseBody
	public MessageUtil setUpLoginUserSwitch(int userId,int chatType,int witchType) {
		try {
			// chatType  1:视频开关  2:语音  3：文本开关
			LoginInfo voiceLoginUserMap = UserIoSession.getInstance().getLoginUserMap(userId);
		    if(chatType==1) {
				Map<String, Object> map = SimulationVideoTimer.callUser.get(userId);
		    	if(map!=null) {
		    		map.put("t_is_not_disturb", witchType+"");
		    	}
		    	if (null!=voiceLoginUserMap) {
		    		voiceLoginUserMap.setT_voide_switch(witchType);
		    	}
			} else if(chatType==2) {
				if (null!=voiceLoginUserMap) {
					voiceLoginUserMap.setT_voice_switch(witchType);
				}
			}else if(chatType==3) {
				if (null!=voiceLoginUserMap) {
					voiceLoginUserMap.setT_text_switch(witchType);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	return new MessageUtil(1, "");
 }
	
	
	/**
	 * 	封号处理
	 * @param content
	 * @return
	 */
	@RequestMapping(value= {"handleIllegalityUser"})
	@ResponseBody
	public MessageUtil handleIllegalityUser(int userId,String imgUrl) {
		try {
			prohibitService.handleIllegalityUser(userId, imgUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	return new MessageUtil(1, "");
 }
		
}
