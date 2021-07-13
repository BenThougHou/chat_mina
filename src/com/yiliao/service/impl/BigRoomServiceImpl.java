package com.yiliao.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Service;

import com.yiliao.domain.UserIoSession;
import com.yiliao.domain.WalletDetail;
import com.yiliao.evnet.PushBigUserCount;
import com.yiliao.service.BigRoomService;
import com.yiliao.service.ProhibitService;
import com.yiliao.util.MessageUtil;
import com.yiliao.util.Mid;
import com.yiliao.util.SpringConfig;
import com.yiliao.util.StopThread;

import net.sf.json.JSONObject;

@Service(value = "bigRoomService")
public class BigRoomServiceImpl extends ICommServiceImpl implements BigRoomService {
	/**
	 * 用户加入房间
	 */
	@Override
	public MessageUtil userMixBigRoom(int userId, int anchorId) {
		try {
			// 获取当前用户是否在大房间中
			List<Map<String, Object>> sqlList = this.getQuerySqlList("SELECT * FROM t_big_room_viewer WHERE t_user_id = ? ", userId);

			if (null != sqlList && !sqlList.isEmpty()) {
				// 删除原有的房间
				this.executeSQL("DELETE FROM t_big_room_viewer WHERE t_user_id = ?; ", userId);
			}

			// 获取主播的房间信息
			List<Map<String, Object>> bigRoom = this.getQuerySqlList(
					"SELECT * FROM t_big_room_man WHERE t_user_id = ? AND t_room_id > 0 AND t_is_debut = 1 ;",
					anchorId);

			// 获取当前主播的头像昵称关注数
			Map<String, Object> map = this.getMap(
					"SELECT u.t_handImg,u.t_nickName,COUNT(f.t_id) AS followNumber FROM t_user u LEFT JOIN t_follow f ON f.t_cover_follow = u.t_id WHERE u.t_id = ?",
					anchorId);
			// 获取当前用户是否关注
			List<Map<String, Object>> isFollow = this.getQuerySqlList(
					"SELECT * FROM t_follow WHERE t_cover_follow = ? AND t_follow_id = ?;", anchorId, userId);

			if (null == isFollow || isFollow.isEmpty()) {
				map.put("isFollow", 0);
			} else {
				map.put("isFollow", 1);
			}

			//设置主播是否开播
			map.put("isDebut", bigRoom.isEmpty()?0:1);
			
			// 如果用户和主播ID 不形同 那么加入到房间中
			if (userId != anchorId) {
				// 把用户写入到数据库中
				this.executeSQL("INSERT INTO t_big_room_viewer (t_big_room_id, t_user_id) VALUES ( ?, ?);",	(anchorId+10000)*100, userId);
			}

			// 获取消费最高的5个用户
			StringBuffer body = new StringBuffer();
			body.append("SELECT u.t_id,ifnull(u.t_handImg,'') t_handImg,SUM(o.t_amount) AS total ");
			body.append("FROM t_order o LEFT JOIN t_user u ON u.t_id = o.t_consume ");
			body.append("WHERE o.t_cover_consume = ? AND t_consume_type IN (");
			body.append(WalletDetail.CHANGE_CATEGORY_TEXT).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PRIVATE_VIDEO).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_PHONE).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_WEIXIN).append(",");
			body.append(WalletDetail.CHANGE_CATEGORY_RED_PACKET).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_GIFT).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_PHOTO).append(",");
			body.append(WalletDetail.CHANGE_CATEGOR_DYNAMIC_VIDEO);
			body.append(") ");
			body.append("GROUP BY o.t_consume");

			// 分页获取数据
			List<Map<String, Object>> dataList = this
					.getQuerySqlList("SELECT * FROM (" + body + ") aa ORDER BY aa.total DESC LIMIT 5;", anchorId);
			
//			dataList.forEach(s ->{ if(null == s.get("t_handImg")) s.put("t_handImg", "");});
			
			map.put("devoteList", dataList);

			// 获取当前主播有多少人在房间中
			Map<String, Object> viewerM = this.getMap("SELECT COUNT(t_id) AS viewer FROM t_big_room_viewer WHERE t_big_room_id = ?",
					(anchorId+10000)*100);

			map.put("viewer", viewerM.get("viewer"));

			/********* 推送视频提示语 ***********/
			// 链接人
			// 获取提示信息
			List<Map<String, Object>> tipsList = this.getQuerySqlList("SELECT t_video_hint FROM t_system_setup ");
			// 如果后台填写了 提示信息
			if (!tipsList.isEmpty()) {
				// 链接人session
				map.put("warning", tipsList.get(0).get("t_video_hint"));
			}

			/******************************/

			// 获取加入房间的用户昵称
			Map<String, Object> userN = this.getMap("SELECT t_nickName FROM t_user WHERE t_id = ? ", userId);
				// 异步推送
			this.applicationContext.publishEvent(new PushBigUserCount(new HashMap<String, Object>() {
				private static final long serialVersionUID = 1L;
				{
					put("roomId", (anchorId+10000)*100);
					put("userName", userN.get("t_nickName"));
				}
			}));

			System.out.println(JSONObject.fromObject(map).toString());
			
			return  new MessageUtil(1,JSONObject.fromObject(map));
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}

	@Override
	public MessageUtil userQuitBigRoom(int userId) {
		try {
			// 删除原有的房间
			this.executeSQL("DELETE FROM t_big_room_viewer WHERE t_user_id = ?; ", userId);

			return new MessageUtil(1, "已退出!");
		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}



	public int goldFiles(int gold) {
		if (gold <= 500) {
			return 1;
		} else if (501 <= gold && gold <= 1000) {
			return 2;
		} else if (1001 <= gold && gold <= 2000) {
			return 3;
		} else if (2001 <= gold && gold <= 3000) {
			return 4;
		} else {
			return 5;
		}
	}

	/** 充值档 */
	public int grade(int money) {

		if (money <= 1000) {
			return 1;
		} else if (money > 1000 && money <= 10000) {
			return 2;
		} else if (money >= 10001) {
			return 3;
		}
		return 0;
	}
	
	@Override
	public void sendBigUserCount(Map<String, Object> map) {
		try {

			// 查询出该房间共多少人
			Map<String, Object> mapUser = this.getMap("SELECT COUNT(t_id) totalCount FROM t_big_room_viewer WHERE t_big_room_id = ? ", map.get("roomId"));

			if (null != mapUser && 0 < Integer.parseInt(mapUser.get("totalCount").toString())) {

				// 给主播推送消息
				 List<Map<String, Object>> bigRooms = this.getQuerySqlList("SELECT t_user_id FROM t_big_room_man WHERE t_room_id = ? ", map.get("roomId"));
				if (null != bigRooms && !bigRooms.isEmpty()) {
					IoSession session = UserIoSession.getInstance()
							.getMapIoSession(Integer.parseInt(bigRooms.get(0).get("t_user_id").toString()));
					if (null != session) {
						session.write(JSONObject.fromObject(new HashMap<String, Object>() {
							private static final long serialVersionUID = 1L;
							{
								put("mid", Mid.sendBigUserCountMsg);
								put("userCount", mapUser.getOrDefault("totalCount", 0));
								put("sendUserName", map.get("userName"));
							}
						}).toString());
					}
				}

				// 计算总页数
				int pageCount = Integer.parseInt(mapUser.get("totalCount").toString()) % 5000 == 0
						? Integer.parseInt(mapUser.get("totalCount").toString()) / 5000
						: Integer.parseInt(mapUser.get("totalCount").toString()) / 5000 + 1;

				String qSql = " SELECT t_user_id FROM t_big_room_viewer WHERE t_big_room_id = ?  LIMIT ?,5000; ";

				for (int i = 1; i <= pageCount; i++) {
					// 得到数据
					this.getQuerySqlList(qSql, map.get("roomId"), (i - 1) * 5000).forEach(s -> {
						IoSession session = UserIoSession.getInstance()
								.getMapIoSession(Integer.parseInt(s.get("t_user_id").toString()));
						if (null != session) {
							session.write(JSONObject.fromObject(new HashMap<String, Object>() {
								private static final long serialVersionUID = 1L;
								{
									put("mid", Mid.sendBigUserCountMsg);
									put("userCount", mapUser.getOrDefault("totalCount", 0));
									put("sendUserName", map.get("userName"));
								}
							}).toString());
						}

					});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 关闭直播
	 */
	@Override
	public MessageUtil closeLiveTelecast(int userId, int type) {
		try {

			this.executeSQL("UPDATE t_big_room_man set t_is_debut = 0 WHERE t_user_id = ? ", userId);

			// 删除房间信息
//			RoomTimer.useRooms.remove((userId + 10000) * 100);

			if (type == 1) {
				new StopThread((userId + 10000) * 100).start();
			} else {
				ProhibitService prohibitService = (ProhibitService) SpringConfig.getInstance()
						.getBean("prohibitService");
				prohibitService.handleIllegalityUser(userId, "监控后台封号.");
			}

			return new MessageUtil(1, "直播已关闭.");

		} catch (Exception e) {
			e.printStackTrace();
			return new MessageUtil(0, "程序异常!");
		}
	}
 

}
