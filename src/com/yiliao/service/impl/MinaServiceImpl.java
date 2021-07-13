package com.yiliao.service.impl;

import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.UserIoSession;
import com.yiliao.service.MinaService;
import com.yiliao.timer.VideoTiming;

@Service
public class MinaServiceImpl extends ICommServiceImpl implements MinaService {

	/**
	 * 推送给移动端
	 */
	@Override
	public void notice(Integer userId, Integer userId1, String content) {
		 try {
			
			 logger.info("userId->{}",userId);
			 logger.info("userId1->{}",userId1);
			 
			 if (null != userId && 0 != userId) {

					IoSession session = UserIoSession.getInstance().getMapIoSession(userId);

					if (null != session) {
						session.write(content);
					}
				}

				if (null != userId1 && 0 != userId1) {
					IoSession session = UserIoSession.getInstance().getMapIoSession(userId1);

					if (null != session) {
						session.write(content);
					}
				}
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void logout(int userId) {
		
		// 清空登陆状态信息
		UserIoSession.getInstance().loginUserMap.remove(userId);
		UserIoSession.getInstance().loginGirlAnchorMap.remove(userId);
		UserIoSession.getInstance().loginMaleAnchorMap.remove(userId);
	}

	@Override
	public void videoTime(int userId,Map<String, Integer> map) {
		 
		VideoTiming.timingUser.put(userId, map);
		
	}
	
	
	

}
