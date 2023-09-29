package org.zeith.hr4j.wss;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;

@ServerEndpoint("/")
public class StaticWebSocketEndpoint
{
	@OnOpen
	public void open(Session session, EndpointConfig cfg)
	{
		WebSocketStorage.Data data = WebSocketStorage.DATA_MAP.get(session.getContainer());
		if(data == null)
		{
			System.out.println("Tried opening a session without a data for the ws container.");
			try
			{
				session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "There is no contextualized storage for the server."));
			} catch(IOException e)
			{
				throw new RuntimeException(e);
			}
			return;
		}
		
		Map<String, Session> allSessions = data.ALL_SESSIONS;
		synchronized(allSessions)
		{
			allSessions.put(session.getId(), session);
		}
		
		data.onConnect.accept(session.getAsyncRemote()::sendText);
		
		System.out.println("Opened websocket connection to " + session);
	}
	
	@OnClose
	public void close(Session session)
	{
		WebSocketStorage.Data data = WebSocketStorage.DATA_MAP.get(session.getContainer());
		if(data == null) return;
		Map<String, Session> allSessions = data.ALL_SESSIONS;
		synchronized(allSessions)
		{
			allSessions.remove(session.getId());
		}
	}
	
	@OnMessage
	public void onMessage(String msg, Session session)
	{
	}
}