package com.yiliao.domain;

import com.yiliao.util.MidRes;

public class OnLineRes extends MidRes {
 
//	public OnLineRes(Integer mid) {
//		super(mid);
//	}

	private static final long serialVersionUID = 1L;
	/** 房间号 */
	private int roomId;
	/** 链接人用户编号 */
	private int connectUserId;
	/** 是否住够进行视频聊天 -1:不能进行聊天  1:可以进行聊天 */
	private int satisfy;

	public OnLineRes() {
		
	}
	
	public OnLineRes(Integer mid, int roomId, int connectUserId, int satisfy) {
		super(mid);
		this.roomId = roomId;
		this.connectUserId = connectUserId;
		this.satisfy = satisfy;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getConnectUserId() {
		return connectUserId;
	}

	public void setConnectUserId(int connectUserId) {
		this.connectUserId = connectUserId;
	}


	public int getSatisfy() {
		return satisfy;
	}


	public void setSatisfy(int satisfy) {
		this.satisfy = satisfy;
	}

}
