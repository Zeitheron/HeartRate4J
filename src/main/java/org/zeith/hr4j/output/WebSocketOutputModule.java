package org.zeith.hr4j.output;

import org.json.JSONObject;
import org.zeith.hr4j.*;
import org.zeith.hr4j.modules.ModuleSpecs;
import org.zeith.hr4j.output.api.BaseOutputModule;
import org.zeith.hr4j.wss.*;

import javax.websocket.Session;
import java.util.*;
import java.util.function.Consumer;

public class WebSocketOutputModule
		extends BaseOutputModule
		implements IHealthListener
{
	protected final String ip;
	protected final int port;
	protected final String format;
	
	protected WebSocketServer wss;
	
	public WebSocketOutputModule(String ip, int port, String format)
	{
		this.ip = ip;
		this.port = port;
		this.format = format;
	}
	
	HealthInfo info;
	
	@Override
	public void subscribe(HealthInfo info)
	{
		this.info = info;
		info.addListener(this);
	}
	
	@Override
	public void unsubscribe(HealthInfo info)
	{
		this.info = null;
		info.removeListener(this);
	}
	
	@Override
	public void start()
			throws Exception
	{
		wss = new WebSocketServer(ip, port, "/", StaticWebSocketEndpoint.class);
		wss.start();
		wss.getData().onConnect = this::onConnect;
		System.out.println("Start websocket on ws://" + ip + ":" + port + "/");
	}
	
	private void onConnect(Consumer<String> sender)
	{
		if(info == null)
			return;
		for(HealthInfo.Field update : HealthInfo.Field.values())
			sender.accept(format
					.replace("%event%", update.name().toLowerCase(Locale.ROOT))
					.replace("%value%", update.get(info)));
	}
	
	@Override
	public void stop()
	{
		wss.stop();
		wss = null;
	}
	
	@Override
	public void onUpdate(HealthInfo info, Object oldValue, HealthInfo.Field update)
	{
		WebSocketStorage.Data data = wss.getData();
		if(data != null)
		{
			String msg = format
					.replace("%event%", update.name().toLowerCase(Locale.ROOT))
					.replace("%value%", update.get(info));
			
			Map<String, Session> allSessions = data.ALL_SESSIONS;
			synchronized(allSessions)
			{
				for(Session value : allSessions.values())
					value.getAsyncRemote().sendText(msg);
			}
		}
	}
	
	public static class Specs
			extends ModuleSpecs<WebSocketOutputModule>
	{
		public Specs(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<WebSocketOutputModule> create(JSONObject obj)
		{
			String ip = obj.optString("ip");
			int port = obj.getInt("port");
			String format = obj.optString("format");
			
			return ip != null && !ip.isBlank() && port > 0 && format != null && !format.isBlank()
				   ? Optional.of(new WebSocketOutputModule(ip, port, format))
				   : Optional.empty();
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("ip", "127.0.0.1");
			obj.put("port", 54675); // default Resonite port
			obj.put("format", "%event%/%value%");
		}
	}
}