package org.zeith.hr4j.input;

import org.json.*;
import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.modules.ModuleSpecs;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PulsoidInputModule
		extends BaseInputModule
{
	public static final Pattern VIEW_ID = Pattern.compile("https?://pulsoid\\.net/widget/view/(?<id>[^/]+)");
	public static final Predicate<String> VIEW_MATCHER = VIEW_ID.asMatchPredicate();
	
	private final HttpClient http;
	
	protected WebSocket ws;
	
	protected boolean supposedToClose;
	
	protected final String overlayId;
	
	public PulsoidInputModule(String widgetURL)
			throws GeneralSecurityException, IllegalArgumentException
	{
		var m = VIEW_ID.matcher(widgetURL.toString());
		if(m.find())
			overlayId = m.group("id");
		else
			throw new IllegalArgumentException("Did not find the id of overlay in " + widgetURL);
		
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
		
		var resp = http.send(HttpRequest.newBuilder(URI.create("https://pulsoid.net/v1/api/public/rpc"))
						.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36")
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(
								new JSONObject()
										.put("id", UUID.randomUUID().toString())
										.put("jsonrpc", "2.0")
										.put("method", "getWidget")
										.put("params", new JSONObject()
												.put("widgetId", overlayId)
										)
										.toString()
						))
						.build(),
				HttpResponse.BodyHandlers.ofString()
		);
		
		var wssUri = ((JSONObject) new JSONTokener(resp.body()).nextValue())
				.getJSONObject("result")
				.getString("ramielUrl");
		
		ws = http.newWebSocketBuilder()
				.buildAsync(URI.create(wssUri), new WebSocket.Listener()
				{
					Long lastUpd;
					
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
					{
						if(data.charAt(0) == '[' || data.charAt(0) == '{')
						{
							var json = new JSONTokener(data.toString()).nextValue();
							
							if(json instanceof JSONObject)
							{
								var obj = (JSONObject) json;
								
								if(obj.has("data") && obj.has("timestamp"))
								{
									long time = obj.getLong("timestamp");
									if(lastUpd == null || time > lastUpd)
									{
										var dataObj = obj.getJSONObject("data");
										if(dataObj.has("heartRate"))
										{
											double bpm = dataObj.getDouble("heartRate");
											getHealthInfo().updateBpm(bpm, Instant.ofEpochMilli(time));
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
	}
	
	@Override
	public void stop()
	{
		supposedToClose = true;
		
		var w = ws;
		if(w != null)
		{
			w.abort();
			ws = null;
		}
	}
	
	public static class Specs
			extends ModuleSpecs<PulsoidInputModule>
	{
		public Specs(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<PulsoidInputModule> create(JSONObject obj)
		{
			var url = obj.optString("overlay_url", "");
			if(!VIEW_MATCHER.test(url)) return Optional.empty();
			try
			{
				return Optional.of(new PulsoidInputModule(url));
			} catch(GeneralSecurityException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("overlay_url", "https://pulsoid.net/widget/view/00000000-0000-0000-0000-000000000000");
		}
	}
}