package com.yiliao.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiliao.service.LoginService;
import com.yiliao.timer.RoomConsumeTimer;
import com.yiliao.timer.SealUserTimer;
import com.yiliao.timer.SendFictitiousMsgTimer;
import com.yiliao.timer.SendTipsMsg;
import com.yiliao.timer.SimulationVideoTimer;
import com.yiliao.timer.SmsTimer;
import com.yiliao.timer.SocketDelayTimer;
import com.yiliao.timer.UserTimer;
import com.yiliao.timer.VideoTiming;
import com.yiliao.timer.VipTimer;
import com.yiliao.timer.VirtualAnchorTimer;
import com.yiliao.timer.WebSocketTimer;
import com.yiliao.util.DateUtils;
import com.yiliao.util.SpringConfig;

public class TimerTask extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Logger logger = LoggerFactory.getLogger(TimerTask.class);

	public void run() {

	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request  the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException      if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the GET method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request  the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException      if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    This is ");
		out.print(this.getClass());
		out.println(", using the POST method");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {

		// ???????????????????????????
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10);
		/*
		 * new SmsTimer(), SmsTimer ???????????? ??????????????? ????????????????????? ??????????????? ??????????????????
		 */
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					// ????????????
					new SmsTimer().delSmsCode();
					// ??????????????????
					new SimulationVideoTimer().run();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, 30 * 1000, 1000 * 60, TimeUnit.MILLISECONDS);
		//?????????????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					// vip??????
					new VipTimer().handleVIPExpire();
					// ??????
					new SealUserTimer().handleVIPExpire();
//					// ?????????????????????????????????
//					new RoomConsumeTimer().run();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}, 30 * 1000, 1000 * 60, TimeUnit.MILLISECONDS);
		
		
		//????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					// ??????????????????
					new SendFictitiousMsgTimer().sendFictitiousMsg();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}, 30 * 1000, 1000 * 10, TimeUnit.MILLISECONDS);
		//?????????????????????????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					//?????????????????????????????????
					new UserTimer().setUpBrower();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 30 * 1000, 1000 * 60*30, TimeUnit.MILLISECONDS);

		// ????????????????????????
		// ?????????????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					new VideoTiming().TimerTiming();
					new VideoTiming().clearUser();
					//socket ??????????????????
					new SocketDelayTimer().SocketDelayRun();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000 * 10, 1000, TimeUnit.MILLISECONDS);
		
		//??????????????? 5????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					//????????????
					new SendTipsMsg().sendSpreedTipsMsg();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 10000, 1000*60*5, TimeUnit.MILLISECONDS);

		// ?????????????????? ?????????????????????????????????
		LoginService loginAppService = (LoginService) SpringConfig.getInstance().getBean("loginAppService");
		loginAppService.startUpOnLine();
		// ???????????????????????????????????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				new VirtualAnchorTimer().handleVIPExpire();
				//????????????????????? ????????????
			
			}
		}, 1000 * 10, 1000 * 60 * 30, TimeUnit.MILLISECONDS);

		// ??????2?????? ??????websocket?????????
		scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				new WebSocketTimer().sotpIoSession();
			}
		}, 1000 * 10, 1000 * 60 * 2, TimeUnit.MILLISECONDS);

		//????????????????????????  ????????????????????????????????????
		long oneDay = 24 * 60 * 60 * 1000;
		long initDelay = DateUtils.getTimeMillis("00:00:00") - System.currentTimeMillis();
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduledThreadPool.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						try {
							new SendFictitiousMsgTimer().updSendtenceList();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				},
				initDelay,
				oneDay,
				TimeUnit.MILLISECONDS);
		// ?????????????????????
//		new RoomTimer().productionFreeRoom();
	}
}
