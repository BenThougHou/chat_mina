package com.yiliao.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.LoginInfo;
import com.yiliao.domain.User;
import com.yiliao.domain.UserIoSession;
import com.yiliao.service.LoginService;
import com.yiliao.service.VideoChatService;
import com.yiliao.timer.SimulationVideoTimer;
//import com.yiliao.timer.RoomTimer;
import com.yiliao.util.DateUtils;
import com.yiliao.util.SpringConfig;

@Service("loginAppService")
public class LoginServiceImpl extends ICommServiceImpl implements LoginService {

	VideoChatService videoService = (VideoChatService) SpringConfig.getInstance().getBean("videoService");

	@Override
	public void socketBreak(int userId) {

		logger.info("当前用户已下线-->{}", userId);

		// 获取当前用户是否存在永久在线权限 如果存在
//		List<Map<String, Object>> userList = this.getFinalDao().getIEntitySQLDAO()
//				.findBySQLTOMap("SELECT * FROM t_user WHERE t_online_setup = 1 AND t_id = ? ", userId);

//		if (userList.isEmpty()) {

			String uSql = "UPDATE t_user SET  t_onLine=? WHERE t_id=?;";
			this.getFinalDao().getIEntitySQLDAO().executeSQL(uSql, User.ONLINE_OFF_LINE, userId);

			// 修改主播表
			int executeSQL = this.executeSQL("UPDATE t_home_table SET t_onLine = ? WHERE t_id = ?;", User.ONLINE_OFF_LINE,
					userId);

			logger.info("更新主播表的执行结果-->{}", executeSQL);

//			List<Map<String, Object>> sqlList = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(
//					"SELECT t_id,t_login_time FROM t_log_time WHERE t_user_id = ? AND t_logout_time IS NULL ;", userId);
//
//			if (!sqlList.isEmpty()) {
//				try {
//
//					long logoutTime = System.currentTimeMillis();
//
//					long time = logoutTime - DateUtils
//							.parse(sqlList.get(0).get("t_login_time").toString(), DateUtils.FullDatePattern).getTime();
//
//					this.getFinalDao().getIEntitySQLDAO().executeSQL(
//							"UPDATE t_log_time SET t_logout_time = ?,t_duration = ? WHERE t_id = ? ;",
//							DateUtils.format(logoutTime, DateUtils.FullDatePattern), (time / 1000),
//							sqlList.get(0).get("t_id"));
//				} catch (ParseException e) {
//					e.printStackTrace();
//				}
//			}
			//清楚虚拟视频
			SimulationVideoTimer.callUser.remove(userId);
			//清除虚拟发信息
			UserIoSession.getInstance().loginUserMap.remove(userId);
			UserIoSession.getInstance().loginGirlAnchorMap.remove(userId);
			UserIoSession.getInstance().loginMaleAnchorMap.remove(userId);
//		}else {
//			try {
//				//清楚虚拟视频
//				Map<String, Object> map = SimulationVideoTimer.callUser.get(userId);
//				if(map!=null) {
//					map.put("sendTime", new ArrayList<Integer>());
//				}
//				LoginInfo loginInfo = UserIoSession.getInstance().loginUserMap.get(userId);
//				//清除虚拟发信息
//				if(loginInfo!=null) {
//					loginInfo.setTimes(new ArrayList<Integer>());
//				}
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//		}
	}

	/**
	 * socket连线
	 */
	@Override
	public void socketOnLine(int userId, IoSession session) {
		try {

			logger.info("当前用户已上线-->{}", userId);
			
			String uSql = "UPDATE t_user u SET u.t_onLine = ? WHERE u.t_id = ? ";
			int resCount = this.executeSQL(uSql,User.ONLINE_IDLE,userId);
			//如果没有任何数据修改
			//那么关闭session
			if(resCount <= 0) {
				session.isClosing();
				return ;
			}
            Thread.sleep(50);
			List<Map<String, Object>> userDatas = getQuerySqlList("SELECT t_id FROM t_user WHERE t_id = ? AND t_role = 1", userId);

			if (null != userDatas && !userDatas.isEmpty()) {
				// 修改home_table
				this.executeSQL("UPDATE t_home_table SET t_onLine = ? WHERE t_id = ? ", User.ONLINE_IDLE, userId);
				Thread.sleep(50);
				/** 用户登陆时间 **/
				this.executeSQL("INSERT INTO t_log_time (t_user_id, t_login_time) VALUES (?,?)", userId,
						DateUtils.format(new Date(), DateUtils.FullDatePattern));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取模拟消息
	 */
	@Override
	public String getSimulationMsg(int sex) {
		try {

			String qSql = " SELECT t_centent FROM t_simulation WHERE t_sex = ? AND t_id >= ((SELECT MAX(t_id) FROM t_simulation)-(SELECT MIN(t_id) FROM t_simulation)) * RAND() + (SELECT MIN(t_id) FROM t_simulation)  LIMIT 1 ";

			List<Map<String, Object>> sqltoMap = this.getFinalDao().getIEntitySQLDAO().findBySQLTOMap(qSql, sex);

			return sqltoMap.isEmpty() ? "" : sqltoMap.get(0).get("t_centent").toString();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取模拟消息异常!", e);
		}
		return null;
	}

	/*
	 * 修改启动状态 (non-Javadoc)
	 * 
	 * @see com.yiliao.service.LoginService#startUpOnLine()
	 */
	@Override
	public void startUpOnLine() {

		String uSql = " UPDATE t_user SET t_onLine = ? WHERE t_id NOT IN (SELECT t_user_id FROM t_virtual) ";

		this.getFinalDao().getIEntitySQLDAO().executeSQL(uSql,User.ONLINE_OFF_LINE);
		// 修改home_table 表中在线状态为离线
		this.executeSQL("UPDATE t_home_table SET t_online = ?  WHERE t_id NOT IN (SELECT t_user_id FROM t_virtual) ",User.ONLINE_OFF_LINE);
	}
}
