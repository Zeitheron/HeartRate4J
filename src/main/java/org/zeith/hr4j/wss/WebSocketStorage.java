package org.zeith.hr4j.wss;

import javax.websocket.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WebSocketStorage
{
	static final Map<WebSocketContainer, WebSocketStorage.Data> DATA_MAP = new ConcurrentHashMap<>();
	
	public static class Data
	{
		public final Map<String, Session> ALL_SESSIONS = new ConcurrentHashMap<>();
		public Consumer<Consumer<String>> onConnect = sender ->
		{
		};
	}
}