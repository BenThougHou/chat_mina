package com.yiliao.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiliao.service.BigRoomService;
import com.yiliao.util.MessageUtil;

import net.sf.json.JSONObject;

@Controller
@RequestMapping("app")
public class BigRoomControl {

	@Autowired
	private BigRoomService bigRoomService;

	/**
	 * 用户加入房间
	 * @param userId
	 * @param anchorId
	 * @return
	 */
	@RequestMapping(value = { "userMixBigRoom" }, method = RequestMethod.POST)
	@ResponseBody
	JSONObject userMixBigRoom(int userId, int anchorId) {

		return JSONObject.fromObject(this.bigRoomService.userMixBigRoom(userId, anchorId));
	}

	/**
	 * 关闭直播间
	 * @param req
	 * @return
	 */
	@RequestMapping(value = { "closeLiveTelecast" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil closeLiveTelecast(int userId) {
		 
		return this.bigRoomService.closeLiveTelecast(userId, 1);
	}
	/**
	 * 用户退出房间
	 * 
	 * @param req
	 * @return
	 */
	@RequestMapping(value = { "userQuitBigRoom" }, method = RequestMethod.POST)
	@ResponseBody
	public MessageUtil userQuitBigRoom(int userId) {

		return this.bigRoomService.userQuitBigRoom(userId);
	}

}
