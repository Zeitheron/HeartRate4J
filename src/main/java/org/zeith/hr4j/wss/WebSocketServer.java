package org.zeith.hr4j.wss;

import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;
import java.util.Set;

public class WebSocketServer
		extends Server
{
	public WebSocketServer(String hostName, int port, String rootPath, Class<?>... configuration)
	{
		super(hostName, port, rootPath, configuration);
	}
	
	public WebSocketServer(String hostName, int port, String rootPath, Set<Class<?>> configuration)
	{
		super(hostName, port, rootPath, configuration);
	}
	
	public WebSocketStorage.Data getData()
	{
		var ctr = getServerContainer();
		return ctr == null ? null : WebSocketStorage.DATA_MAP.get(ctr);
	}
	
	@Override
	public synchronized void start()
			throws DeploymentException
	{
		super.start();
		WebSocketStorage.DATA_MAP.put(getServerContainer(), new WebSocketStorage.Data());
	}
	
	@Override
	public synchronized void stop()
	{
		WebSocketStorage.DATA_MAP.remove(getServerContainer());
		super.stop();
	}
}