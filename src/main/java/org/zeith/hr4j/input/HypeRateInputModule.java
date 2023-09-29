package org.zeith.hr4j.input;

import org.json.*;
import org.jsoup.Jsoup;
import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.modules.ModuleSpecs;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public class HypeRateInputModule
		extends BaseInputModule
{
	private final HttpClient http;
	
	protected final Map<String, String> cookies = new HashMap<>();
	protected final String id;
	
	protected int packet = 4;
	
	protected ScheduledExecutorService exe;
	
	protected String _csrf_token;
	protected String phxSession;
	protected String phxStatic;
	protected String phxRootId;
	
	protected WebSocket ws;
	
	protected boolean supposedToClose;
	
	public HypeRateInputModule(String id)
			throws GeneralSecurityException
	{
		this.id = id;
		
		var sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, null, new SecureRandom());
		http = HttpClient.newBuilder()
				.sslContext(sslContext)
				.build();
	}
	
	@Override
	public void start()
			throws IOException, InterruptedException
	{
		stop();
		supposedToClose = false;
		exe = Executors.newScheduledThreadPool(1);
		
		var resp = http.send(HttpRequest.newBuilder(URI.create("https://app.hyperate.io/" + id))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36")
				.build(), HttpResponse.BodyHandlers.ofString());
		for(String cookie : resp.headers().allValues("set-cookie"))
		{
			String[] keyVal = cookie.split(";", 2)[0].split("=", 2);
			cookies.put(keyVal[0], keyVal[1]);
		}
		
		var html = Jsoup.parse(resp.body());
		
		_csrf_token = html
				.head()
				.select("meta[name='csrf-token']")
				.stream()
				.findFirst()
				.map(e -> e.attr("content"))
				.orElse("-");
		
		var e = html.body().selectFirst("div[data-phx-main='true']");
		
		phxSession = e.attr("data-phx-session");
		phxStatic = e.attr("data-phx-static");
		phxRootId = e.attr("id");
		
		ws = http.newWebSocketBuilder()
				.subprotocols("permessage-deflate", "client_max_window_bits")
				.header("Cookie", "_hyperate_key=" + cookies.get("_hyperate_key"))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36")
				.header("Origin", "https://app.hyperate.io")
				.header("Pragma", "no-cache")
				.header("Cache-Control", "no-cache")
				.buildAsync(URI.create(
						"wss://app.hyperate.io/live/websocket?_csrf_token=" + _csrf_token + "&_mounts=0&vsn=2.0.0"
				), new WebSocket.Listener()
				{
					@Override
					public void onOpen(WebSocket webSocket)
					{
						JSONArray arr = new JSONArray();
						arr.put(4).put(4);
						arr.put("lv:" + phxRootId);
						arr.put("phx_join");
						arr.put(new JSONObject()
								.put("url", "https://app.hyperate.io/" + id)
								.put("params", new JSONObject()
										.put("_csrf_token", _csrf_token)
										.put("_mounts", 0)
								)
								.put("session", phxSession)
								.put("static", phxStatic)
						);
						webSocket.sendText(arr.toString(), true);
						WebSocket.Listener.super.onOpen(webSocket);
					}
					
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
					{
						if(data.charAt(0) == '[' || data.charAt(0) == '{')
						{
							var json = new JSONTokener(data.toString()).nextValue();
							if(json instanceof JSONArray)
							{
								var arr = (JSONArray) json;
								if(arr.length() > 4 && Objects.equals(arr.opt(3), "diff"))
								{
									var update = arr.getJSONObject(4);
									var events = update.getJSONArray("e");
									for(int i = 0; i < events.length(); i++)
									{
										var evt = events.getJSONArray(i);
										if(Objects.equals(evt.opt(0), "new-heartbeat"))
										{
											double bpm = evt.getJSONObject(1).getDouble("heartbeat");
											getHealthInfo().updateBpm(bpm);
										}
									}
								}
							}
						}
						return WebSocket.Listener.super.onText(webSocket, data, last);
					}
					
					@Override
					public void onError(WebSocket webSocket, Throwable error)
					{
						System.out.println("onError: ");
						error.printStackTrace(System.out);
						WebSocket.Listener.super.onError(webSocket, error);
					}
					
					@Override
					public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
					{
						if(!supposedToClose)
						{
							try
							{
								start();
							} catch(IOException | InterruptedException ex)
							{
								throw new RuntimeException(ex);
							}
						} else
							ws = null;
						
						return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
					}
				})
				.join();
		
		var task = exe.scheduleAtFixedRate(() ->
		{
			var w = ws;
			if(w == null) return;
			w.sendText(String.format("[null,\"%d\",\"phoenix\",\"heartbeat\",{}]", ++packet), true);
		}, 30L, 30L, TimeUnit.SECONDS);
		
		var t = new Thread(() ->
		{
			while(true)
			{
				var w = ws;
				if(w == null) break;
				try
				{
					Thread.sleep(1000L);
				} catch(InterruptedException ex)
				{
					throw new RuntimeException(ex);
				}
			}
			
			task.cancel(false);
			exe.shutdown();
		});
		t.setName("HypeRate Monitor " + id);
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	@Override
	public void stop()
	{
		supposedToClose = true;
		
		var e = exe;
		if(e != null)
		{
			e.shutdown();
			exe = null;
		}
		
		var w = ws;
		if(w != null)
		{
			w.abort();
			ws = null;
		}
	}
	
	public static class Specs
			extends ModuleSpecs<HypeRateInputModule>
	{
		public Specs(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<HypeRateInputModule> create(JSONObject obj)
		{
			String id = obj.optString("hyperate_user_id", null);
			try
			{
				return id == null ? Optional.empty() : Optional.of(new HypeRateInputModule(id));
			} catch(GeneralSecurityException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("hyperate_user_id", "YOUR ID GOES HERE");
		}
	}
}