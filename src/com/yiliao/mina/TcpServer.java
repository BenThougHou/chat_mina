package com.yiliao.mina;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class TcpServer extends IoHandlerAdapter {
	
	public static final int PORT = 18567;
	private static Map<String, IoSession> sessions_ip = new HashMap<String, IoSession>(); // 控制某个IP的连接
	private static int sessionSize = 0; // 控制连接总量

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		cause.printStackTrace();
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		InetAddress address = inetSocketAddress.getAddress();
		String ip = address.getHostAddress();
		int port = inetSocketAddress.getPort();
		// 这里是有这个IP才能移除且总数减一，如果你不想以IP来控制，只是要控制总数，那么在客户端来的时候就要以(IP+端口)为依据来缓存该客户端，然后做出减一操作
		if (null != sessions_ip.get(ip)) {
			System.out.println("客户端离开：" + ip + "：" + port);
			sessions_ip.remove(ip + "：" + port);
			sessionSize--;
		}
		System.out.println("Session closed...");
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		System.out.println("Session created...");
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		System.out.println("Session idle...");
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		if (sessionSize > 2) {
			System.err.println("客户端超出最大数量");
			session.close(true);
			return;
		}
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		InetAddress address = inetSocketAddress.getAddress();
		String ip = address.getHostAddress();
		int port = inetSocketAddress.getPort();
		if (null != sessions_ip.get(ip)) {
			System.err.println("该IP已有客户端连接，禁止新链接");
			session.close(true);
			return;
		}
		System.out.println("新增客户端：" + ip + "：" + port);
		sessions_ip.put(ip, session);
		sessionSize++;
		System.out.println("Session Opened...");
	}

	public TcpServer() throws IOException {
		NioSocketAcceptor acceptor = new NioSocketAcceptor();
		acceptor.setHandler(this);
		SocketSessionConfig scfg = acceptor.getSessionConfig();
		acceptor.bind(new InetSocketAddress(PORT));
		System.out.println("Server started...");
	}

	public static void main(String[] args) throws IOException {
		new TcpServer();
	}
}
