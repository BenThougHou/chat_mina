package com.yiliao.control;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiliao.domain.SentenceSendList;
import com.yiliao.service.VideoChatService;
import com.yiliao.timer.SimulationVideoTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.util.MessageUtil;


/**
 * 视频聊天控制层
 * 
 * @author Administrator
 * 
 */
@Controller
@RequestMapping("app")
public class VideoChatControl {

	@Autowired
	private VideoChatService videoService;

	/**
	 * 获取速配房间号
	 * 
//	 * @param req
	 * @return
	 */
	@RequestMapping(value = { "getSpeedDatingRoom" }, method = { RequestMethod.POST })
	@ResponseBody
	public MessageUtil getSpeedDatingRoom(int userId) {
		// 解密参数

		return videoService.getSpeedDatingRoom(userId);
	}

	/**
	 * 用户对主播发起视频聊天 app调用
	 * 
//	 * @param launchUserId
	 * @param coverLinkUserId
//	 * @param response
	 */
	@RequestMapping("launchVideoChat")
	@ResponseBody
	public MessageUtil launchVideoChat(int userId, int coverLinkUserId, int roomId,int chatType) {

		return this.videoService.launchVideoChat(userId, coverLinkUserId, roomId, chatType);

	}

	/**
	 * 开始计时
	 * 
	 * @param userId   用户编号
	 * @param anthorId 主播编号
	 * @param roomId   房间号
	 * @return
	 */
	@RequestMapping("videoCharBeginTiming")
	@ResponseBody
	public MessageUtil videoCharBeginTiming(int userId, int anthorId, int roomId,int chatType) {

		return this.videoService.videoCharBeginTiming(anthorId, userId, roomId,chatType);

	}

	/**
	 * 断开链接
	 * 
	 * @param userId
	 * @param roomId
//	 * @param response
	 */
	@RequestMapping("breakLink")
	@ResponseBody
	public MessageUtil breakLink(int userId, int breakType,int roomId) {
		// 解密参数
		return this.videoService.breakLink(roomId, breakType, userId);
	}

	/**
	 * 用户挂端链接
	 * 
	 * @param userId
//	 * @param response
	 */
	@RequestMapping("userHangupLink")
	@ResponseBody
	public MessageUtil userHangupLink(int userId) {

		return this.videoService.userHangupLink(userId);

	}

	/**
	 * 主播对用户发起聊天
	 * 
//	 * @param anchorUserId
	 * @param userId
//	 * @param response
	 */
	@RequestMapping("anchorLaunchVideoChat")
	@ResponseBody
	public MessageUtil anchorLaunchVideoChat(int anchorId, int userId, int roomId,int chatType) {

		return this.videoService.anchorLaunchVideoChat(anchorId, userId, roomId,chatType);

	}

	/***
	 * 获取当前用户是否被呼叫
	 * 
	 * @param userId
//	 * @param response
	 */
	@RequestMapping("getUserCoverCall")
	@ResponseBody
	public Map<String, Object> getUuserCoverCall(int userId) {

		System.out.println(111);

		return this.videoService.getUuserCoverCall(userId);
	}

	/**
	 * 获取房间状态
	 * 
//	 * @param req
	 * @return
	 */
	@RequestMapping(value = { "getRoomState" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil getRoomState(int roomId) {

		return videoService.getRoomState(roomId);
	}

	/**
	 * 用户充值金币
	 * 
	 * @param userId
	 * @param gold
	 */
	@RequestMapping(value = { "addRoomRate" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil addRoomRate(int userId, int gold) {

		videoService.addRoomRate(userId, gold);

		return new MessageUtil(1, "调用成功!");
	}
	
	/**
	 * 用户消费金币
	 * @param userId
	 * @param gold
	 * @return
	 */
	@RequestMapping(value= {"userBuyGold"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil userBuyGold(int userId,int gold) {
		
		return videoService.userBuyGold(userId, gold);
	}

	/**
	 * 获取用户是否在聊天
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = { "getUserIsVideo" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil getUserIsVideo(int userId) {

		return videoService.getUserIsVideo(userId);

	}
	
	/**
	 * 加入到模拟呼叫
	 * @param userId
	 */
	@RequestMapping(value= {"addSimulationVideo"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil  addSimulationVideo(int userId,@RequestParam("anchor[]") List<Integer> anchor,int callCount) {
		
		SimulationVideoTimer.callUser.put(userId, new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			{
				put("anchor", anchor);
				put("callCount", callCount);
				put("time", System.currentTimeMillis());
			}
		});
		
	    return new MessageUtil(1, "");
		
	}
	
	/**
	 * 取消呼叫
	 * @param userId
	 */
	@RequestMapping(value = {"delSimulationVideo"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil delSimulationVideo(int userId) {
		//取消模拟呼叫
		SimulationVideoTimer.callUser.remove(userId);
		SentenceSendList.getInstance().sendTenceUserMap.get(userId).clear();
		return new MessageUtil(1, "");
	}
	
	
	
	/**
	 * 获取视频内存状态
	 * @param userId
	 */
	@RequestMapping(value = {"getVideoStatus"},method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil getVideoStatus(int videoUserId,int videoCoverUserId,int roomId,int userId ) {
		//用户
		Map<String, Integer> userIdMap = VideoTiming.timingUser.get(roomId);
		MessageUtil mu = new MessageUtil(1, "");
		mu.setM_object(0);
		if(userIdMap!=null) {
			mu.setM_object(1);
			return mu;
		}
		return mu;
		
	}
	/**
	 *  清除内存 测试方法
	 * @param userId
	 */
	@RequestMapping(value = {"delMemoryVideo"})
	@ResponseBody
	public MessageUtil delMemoryVideo(int roomId) {
		//取消模拟呼叫
		 VideoTiming.timingUser.remove(roomId);
		return new MessageUtil(1, "");
	}	
	
	
	/**
	 *  断掉所有连接
	 * @param userId
	 */
	@RequestMapping(value = {"breakAllVideoUser"})
	@ResponseBody
	public MessageUtil breakAllVideoUser() {
		return videoService.breakAllVideoUser();
	}	
}
