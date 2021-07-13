package com.yiliao.util;

public class Mid {

	/** 监控服务器登陆 */
	public static final int superviseLogReq = 10001;
	/** 子服务器登陆响应 */
	public static final int superviseLogRes = 10002;
	/** 通知监控服务器 用户连线 */
	public static final int supervise_connection = 10003;
	/** 用户已经断开链接 */
	public static final int supervise_destroy = 10004;
	/** 登陆 */
	public static final int userLoginReq = 30001;
	/** 响应 */
	public static final int userLoginRes = 30002;
	/** 发送模拟消息 */
	public static final int sendFictitiousMsgRes = 30003;
	/** 通知连线 */
	public static final int onLineRes = 30004;
	/** 通知用户挂断视频消息 */
	public static final int brokenLineRes = 30005;
	/** 通知用户违规 */
	public static final int getOutOfLineRes = 30006;
	/** 给移动端推送有新的红包 */
	public static final int noticeNewRedPacketRes = 30007;
	/** 通知用户-主播和他连线 */
	public static final int anchorLinkUserRes = 30008;
	/*** 通知用户 ---有新的评论 ***/
	public static final int dynamicRes = 30009;
	/** 用的余额不足 **/
	public static final int notSufficientFunds = 30010;
	/** 通知用户-有新的动态消息 **/
	public static final int dynamicNewRes = 30011;
	/** 接通视频的时候 下发提示信息 **/
	public static final int sendVideoTipsMsg = 30012;
	/** 主播开启速配的时候 下发提示信息 **/
	public static final int sendSpreedTipsMsg = 30013;
	/** 推送房间总人数给用户 **/
	public static final int sendBigUserCountMsg = 30014;
	
	/** 通知语音用户--->主播连线 */
	public static final int onLineToVoiceRes = 30015;
	
	/** 通知语音 主播-->用户语音连线 */
	public static final int anchorLinkUserToVoiceRes = 30016;
	
	/** 全局礼物推送 */
	public static final int allGiftSendSocket = 30017;
	
	/** 通知用户挂断语音消息 */
	public static final int brokenVoiceLineRes = 30018;
	
	/** 通知用户挂断一键拨打视频消息 */
	public static final int brokenVIPLineRes = 30019;
	
	/** 通知用户挂断一键拨打语音消息 */
	public static final int brokenUserVoiceLineRes = 30020;
	
	
	/** 文字推送警告用户 */
	public static final int sendWarningToRoom = 30021;
}
