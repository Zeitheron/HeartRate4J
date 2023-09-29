package org.zeith.hr4j.input;

import org.json.JSONObject;
import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.modules.ModuleSpecs;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class UDPInputModule
		extends BaseInputModule
{
	protected final String prefix;
	protected final int port;
	protected DatagramSocket socket;
	protected Thread task;
	
	public UDPInputModule(String prefix, int port)
	{
		this.prefix = prefix;
		this.port = port;
	}
	
	@Override
	public void start()
	{
		if(task != null) return;
		task = new Thread(this::run);
		task.setName("HeartRate4J");
		task.start();
	}
	
	@Override
	public void stop()
	{
		task = null;
	}
	
	public void run()
	{
		try
		{
			Thread ct = Thread.currentThread();
			socket = new DatagramSocket(38693, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			socket.setSoTimeout(1000);
			
			while(ct == task)
			{
				try
				{
					byte[] recvBuf = new byte[1024];
					DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
					socket.receive(packet);
					
					String data = new String(packet.getData()).trim();
					if(!data.startsWith(prefix))
					{
						System.out.println("Missing prefix. Skipping message " + data);
						continue;
					}
					data = data.substring(prefix.length());
					if(!data.contains("|"))
					{
						System.out.println("Invalid splitter. in data " + data);
						continue;
					}
					var dataArr = data.split("[|]", 2);
					
					switch(dataArr[0].toLowerCase(Locale.ROOT))
					{
						case "bpm":
							getHealthInfo().updateBpm(Double.parseDouble(dataArr[1]));
							break;
						default:
							System.out.println("Unknown data point: " + dataArr[0]);
							break;
					}
				} catch(Exception e)
				{
					if(e instanceof SocketTimeoutException) continue;
					e.printStackTrace();
				}
			}
		} catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static class Spec
			extends ModuleSpecs<UDPInputModule>
	{
		public Spec(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<UDPInputModule> create(JSONObject obj)
		{
			var prefix = obj.optString("prefix", "/209367920764903276/");
			var port = obj.optInt("port", -1);
			
			return port > 0
				   ? Optional.of(new UDPInputModule(prefix, port))
				   : Optional.empty();
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("prefix", "/209367920764903276/");
			obj.put("port", 38693);
		}
	}
}