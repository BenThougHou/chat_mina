package com.yiliao.service.impl;

import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yiliao.domain.DynamicRes;
import com.yiliao.domain.GetOutOfLineRes;
import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.NewRedPacketRes;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.NoticeService;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.MidRes;
import com.yiliao.util.RedisUtil;

import net.sf.json.JSONObject;

@Service("noticeService")
public class NoticeServiceImpl extends ICommServiceImpl implements NoticeService {
	
	@Autowired
	private RedisUtil redis;
	/**
	 * 发送通知
	 */
	@Override
	public void sendSocketNotice(int userId) {
		try {

			logger.info("---动态进行推送---");
			IoSession session = UserIoSession.getInstance().getMapIoSession(userId);
			if (null != session) {
				// socket推送
				NewRedPacketRes newRedP = new NewRedPacketRes();
				newRedP.setMid(Mid.noticeNewRedPacketRes);
				session.write(JSONObject.fromObject(newRedP).toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void sendRedPackegNotice(int userId) {
		try {

			logger.info("---红包推送---");
			IoSession session = UserIoSession.getInstance().getMapIoSession(userId);
			if (null != session) {
				DynamicRes dr = new DynamicRes();
				dr.setMid(Mid.dynamicRes);
				session.write(JSONObject.fromObject(dr).toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void sendNoticeBreakVideo(int userId) {
		try {
			
			// 推送挂断呼叫
			IoSession session = UserIoSession.getInstance()
					.getMapIoSession(userId);
			
			String string = redis.get("BALANCE_USER_ID_"+userId);
			
			if (null != session&&string==null) {
				MidRes mid = new MidRes();
				mid.setMid(Mid.brokenLineRes);

				session.write(JSONObject.fromObject(mid));
			}
			redis.remove("BALANCE_USER_ID_"+userId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendUserViolation(int userId) {
		try {
			
			// socket推送
			IoSession launSession = UserIoSession.getInstance().getMapIoSession(userId);
			if (null != launSession) {
				GetOutOfLineRes gof = new GetOutOfLineRes();
				gof.setMid(Mid.getOutOfLineRes);
//				gof.setMessage(message);
				launSession.write(JSONObject.fromObject(gof).toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 获取在线用户
	 */
	@Override
	public MessageUtil getOnLineUser() {
		
		Map<Integer, LoginInfo> userMap = UserIoSession.getInstance().loginUserMap;
		
		return new  MessageUtil(1, userMap);
	}
	
	
	
	
	
	
	
	
	

}
