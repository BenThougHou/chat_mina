package com.yiliao.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LoginInfo {

	/** 用户编号 */
	private int userId;
	/** 用户角色 */
	private int t_role;
	/** 用户是否VIP */
	private int t_is_vip;
	
	/** 用户是否超级VIP */
	private int t_is_svip;
	
	/** 登陆人性别*/
	private int t_sex;
	/** 获取用户是否开启文本消息开关  0.关闭 1.打开**/
	private int t_text_switch;
	
	/** 获取用户是否开启视频文本消息开关  0.关闭 1.打开**/
	private int t_voide_switch;
	
	/** 获取用户是否开启语音文本消息开关  0.关闭 1.打开**/
	private int t_voice_switch;
	/** 上一次发送消息时间*/
	private long loginTime;

	public int getT_voide_switch() {
		return t_voide_switch;
	}


	public void setT_voide_switch(int t_voide_switch) {
		this.t_voide_switch = t_voide_switch;
	}


	public int getT_voice_switch() {
		return t_voice_switch;
	}


	public int getT_is_svip() {
		return t_is_svip;
	}


	public void setT_is_svip(int t_is_svip) {
		this.t_is_svip = t_is_svip;
	}


	public void setT_voice_switch(int t_voice_switch) {
		this.t_voice_switch = t_voice_switch;
	}

	/** 发送时间集合 */
	private List<Integer> times;
	/** 用于存储已经推送过的主播 */
	private List<Integer> anchor = Collections
			.synchronizedList(new ArrayList<Integer>());

	

	public int getUserId() {
		return userId;
	}


	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getT_role() {
		return t_role;
	}

	public void setT_role(int t_role) {
		this.t_role = t_role;
	}

	public int getT_is_vip() {
		return t_is_vip;
	}

	public void setT_is_vip(int t_is_vip) {
		this.t_is_vip = t_is_vip;
	}

	public List<Integer> getTimes() {
		return times;
	}

	public void setTimes(List<Integer> times) {
		this.times = times;
	}

	public List<Integer> getAnchor() {
		return anchor;
	}

	public void setAnchor(List<Integer> anchor) {
		this.anchor = anchor;
	}

	public long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(long loginTime) {
		this.loginTime = loginTime;
	}

	public int getT_sex() {
		return t_sex;
	}

	public void setT_sex(int t_sex) {
		this.t_sex = t_sex;
	}

	public int getT_text_switch() {
		return t_text_switch;
	}

	public void setT_text_switch(int t_text_switch) {
		this.t_text_switch = t_text_switch;
	}

	
}
