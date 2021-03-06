package com.yiliao.domain;


public class Room  {

	/**房间号*/
	private int roomId;
	/**发起人*/
	private int launchUserId;
	/**发起人昵称*/
	private String lanuchName;
	/**被链接人*/
	private int coverLinkUserId;
	/**被连接人昵称*/
	private String coverName;
	/**发起人直播码*/
	private String launchUserLiveCode;
	/**被链接人直播码*/
	private String coverLinkUserLiveCode;
	/**第一个用户加入房间的时间*/
	private long  createTime;
	/** 发起人**/
	private int callUserId;
	/** 聊天方式1 3 5:视频 2 4 6:语音 **/
	private int chatType;
	/* 聊天发起方式 1:用户呼叫主播  2:主播呼叫用户*/	
	private int conversationType;
	
	
	
	public int getConversationType() {
		return conversationType;
	}

	public void setConversationType(int conversationType) {
		this.conversationType = conversationType;
	}

	public Room() {
		// TODO Auto-generated constructor stub
	}
	
	public Room(int roomId) {
		super();
		this.roomId = roomId;
	}
	public int getRoomId() {
		return roomId;
	}
	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}
	public int getLaunchUserId() {
		return launchUserId;
	}
	public void setLaunchUserId(int launchUserId) {
		this.launchUserId = launchUserId;
	}
	public int getCoverLinkUserId() {
		return coverLinkUserId;
	}
	public void setCoverLinkUserId(int coverLinkUserId) {
		this.coverLinkUserId = coverLinkUserId;
	}

	public String getLaunchUserLiveCode() {
		return launchUserLiveCode;
	}

	public void setLaunchUserLiveCode(String launchUserLiveCode) {
		this.launchUserLiveCode = launchUserLiveCode;
	}

	public String getCoverLinkUserLiveCode() {
		return coverLinkUserLiveCode;
	}

	public void setCoverLinkUserLiveCode(String coverLinkUserLiveCode) {
		this.coverLinkUserLiveCode = coverLinkUserLiveCode;
	}

	public String getLanuchName() {
		return lanuchName;
	}

	public void setLanuchName(String lanuchName) {
		this.lanuchName = lanuchName;
	}

	public String getCoverName() {
		return coverName;
	}

	public void setCoverName(String coverName) {
		this.coverName = coverName;
	}

	public int getChatType() {
		return chatType;
	}

	public void setChatType(int chatType) {
		this.chatType = chatType;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public int getCallUserId() {
		return callUserId;
	}

	public void setCallUserId(int callUserId) {
		this.callUserId = callUserId;
	}

}
