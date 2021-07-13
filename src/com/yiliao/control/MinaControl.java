package com.yiliao.control;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiliao.service.MinaService;

import net.sf.json.JSONObject;

/**
 * 
 * @author Administrator
 *
 */
@RequestMapping(value="app")
@Controller
public class MinaControl {

	@Autowired
	private MinaService  minaService;
	
	
	@RequestMapping(value="notice",method = RequestMethod.POST)
	@ResponseBody
	public int notice(Integer userId,Integer userId1,String content) throws Exception {
		
		this.minaService.notice(userId, userId1, URLDecoder.decode(content, "utf-8"));
		
		return 200;
	}
	
	/**
	 * 用户登出
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "logout",method = RequestMethod.POST)
	@ResponseBody
	public int logout(int userId) {
		
		this.minaService.logout(userId);
		
		return 200;
	}
	
	
}
