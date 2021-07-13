package com.yiliao.service.impl;

import org.springframework.stereotype.Service;

import com.yiliao.service.VIPService;

/**
 * vip 服务层实现类
 * @author Administrator
 *
 */
@Service("vIPService")
public class VIPServiceImpl extends ICommServiceImpl implements VIPService {

	/**
	 * 修改VIP已到期的数据
	 */
	@Override
	public void updateVIPExpire() {
		
		try {
			String updateSql = "UPDATE t_vip v,t_user u SET u.t_is_vip = 1 WHERE v.t_user_id = u.t_id AND t_end_time <= now();";
		
		    this.getFinalDao().getIEntitySQLDAO().executeSQL(updateSql);
		    
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改VIP到期异常!", e);
		}
	}
	@Override
	public void updateVirtualState() {
		try {
			//更新 t_anchor
			StringBuffer sql = new StringBuffer();
			sql.append("UPDATE t_user u SET u.t_onLine = CASE u.t_onLine  WHEN 0 THEN 1 WHEN 1 THEN 2 WHEN 2 THEN 0 END ");
			sql.append(" WHERE t_id IN (SELECT v.t_user_id FROM t_virtual v) AND u.t_disable !=2");
			this.executeSQL(sql.toString());
			
			// t_home_table
			this.executeSQL("UPDATE t_home_table h,t_user u SET h.t_online = u.t_onLine WHERE h.t_id = u.t_id ; ");
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("修改虚拟主播异常!", e);
		}
	}

}
