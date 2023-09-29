package org.zeith.hr4j.output;

import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortOut;
import org.json.JSONObject;
import org.zeith.hr4j.*;
import org.zeith.hr4j.modules.ModuleSpecs;
import org.zeith.hr4j.output.api.BaseOutputModule;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

public class OscOutputModule
		extends BaseOutputModule
		implements IHealthListener
{
	protected OSCPortOut outcomingPort;
	
	protected ScheduledExecutorService exe;
	
	protected final String ip;
	protected final int port;
	protected final String addr;
	
	public OscOutputModule(String ip, int port, String addr)
	{
		this.ip = ip;
		this.port = port;
		this.addr = addr;
	}
	
	@Override
	public void start()
			throws Exception
	{
		if(outcomingPort != null)
			try
			{
				outcomingPort.disconnect();
				outcomingPort = null;
			} catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		if(exe != null) exe.shutdown();
		
		outcomingPort = new OSCPortOut(InetAddress.getByName(ip), port);
		exe = Executors.newScheduledThreadPool(1);
		
		exe.scheduleWithFixedDelay(this::broadcastEverything, 5L, 5L, TimeUnit.SECONDS);
	}
	
	@Override
	public void stop()
	{
		try
		{
			outcomingPort.disconnect();
			outcomingPort = null;
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		exe.shutdown();
		exe = null;
		super.stop();
	}
	
	protected HealthInfo info;
	
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
	
	protected void broadcastEverything()
	{
		if(info == null) return;
		for(HealthInfo.Field update : HealthInfo.Field.values())
			onUpdate(info, update.get(info), update);
	}
	
	@Override
	public void onUpdate(HealthInfo info, Object oldValue, HealthInfo.Field update)
	{
		String msg = addr.replace("%event%", update.getEventName());
		
		try
		{
			outcomingPort.send(new OSCMessage(msg, List.of(
					(float) (update.getByte(info) / 255F)
			)));
		} catch(IOException | OSCSerializeException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static class Specs
			extends ModuleSpecs<OscOutputModule>
	{
		public Specs(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<OscOutputModule> create(JSONObject obj)
		{
			String ip = obj.optString("ip");
			int port = obj.getInt("port");
			String addr = obj.optString("addr");
			
			return ip != null && !ip.isBlank() && port > 0 && addr != null && !addr.isBlank()
				   ? Optional.of(new OscOutputModule(ip, port, addr))
				   : Optional.empty();
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("ip", "127.0.0.1");
			obj.put("port", 9000); // vrchat default port.
			obj.put("addr", "/avatar/parameters/hr4j/%event%");
			obj.put("addr$comment", "The data will be passed as float [0; 1]. The internal values are mapped from byte [0; 255] into float [0; 1]");
		}
	}
}
