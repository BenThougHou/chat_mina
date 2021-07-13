package com.yiliao.timer;

import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.UserIoSession;
import com.yiliao.mina.res.ActiveMsgRes;
import com.yiliao.service.PersonalCenterService;
import com.yiliao.util.Mid;
import com.yiliao.util.RandomUtill;
import com.yiliao.util.SpringConfig;

import net.sf.json.JSONObject;


/**
 * 
 * @author Administrator
 * @param <K>
 * 
 */
public class UserTimer{

	Logger logger = LoggerFactory.getLogger(UserTimer.class);
	
	private PersonalCenterService personalCenterService = (PersonalCenterService) SpringConfig.getInstance()
			.getBean("personalCenterService");
  
	
	
	/**
	 *  半小时更新一次浏览人
	 * @param 
	 * @return
	 */
	public  void setUpBrower(){

		// 迭代所有已经登陆的用户
		for (Map.Entry<Integer, LoginInfo> m : UserIoSession.getInstance().loginUserMap.entrySet()) {
					
			// 当用户为女用户时 使用男主播给用户推送
			if (m.getValue().getT_sex() == 0) {
				int rn = RandomUtill.getRandomInt(3);
				personalCenterService.setUpBrower(m.getValue().getUserId(),1,rn);
			} else {
				int rn = RandomUtill.getRandomInt(3);
				personalCenterService.setUpBrower(m.getValue().getUserId(),0,rn);
			}
		}
	}

   
    

	
}
