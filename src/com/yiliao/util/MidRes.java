package com.yiliao.util;

import java.io.Serializable;

public class MidRes implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer mid;

	public Integer getMid() {
		return mid;
	}

	public void setMid(Integer mid) {
		this.mid = mid;
	}

	public MidRes(Integer mid) {
		super();
		this.mid = mid;
	}
	public MidRes() {
		// TODO Auto-generated constructor stub
	}
	
	

}
