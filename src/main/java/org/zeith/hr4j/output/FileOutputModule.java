package org.zeith.hr4j.output;

import org.json.*;
import org.zeith.hr4j.*;
import org.zeith.hr4j.modules.ModuleSpecs;
import org.zeith.hr4j.output.api.BaseOutputModule;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class FileOutputModule
		extends BaseOutputModule
		implements IHealthListener
{
	protected final Path path;
	protected final Set<String> updates;
	
	public FileOutputModule(String path, Set<String> updates)
			throws MalformedURLException, URISyntaxException
	{
		this.path = HeartRate4J.RUN_DIR.resolve(path).toAbsolutePath();
		this.updates = updates;
	}
	
	@Override
	public void subscribe(HealthInfo info)
	{
		info.addListener(this);
	}
	
	@Override
	public void unsubscribe(HealthInfo info)
	{
		info.removeListener(this);
	}
	
	@Override
	public void onUpdate(HealthInfo info, Object oldValue, HealthInfo.Field update)
	{
		if(!updates.contains(update.getEventName())) return;
		try
		{
			Files.writeString(path, update.get(info));
		} catch(IOException e)
		{
			System.out.println("Failed file write for event " + update.getEventName() + ": " + e);
		}
	}
	
	public static class Spec
			extends ModuleSpecs<FileOutputModule>
	{
		public Spec(String id)
		{
			super(id);
		}
		
		@Override
		public Optional<FileOutputModule> create(JSONObject obj)
		{
			var file = obj.optString("file");
			var update = obj.opt("update");
			
			Set<String> updates = new HashSet<>();
			if(update instanceof JSONArray)
			{
				for(int i = 0; i < ((JSONArray) update).length(); i++)
					updates.add(((JSONArray) update).getString(i));
			} else if(update != null)
				updates.add(Objects.toString(update));
			
			try
			{
				return file != null && !file.isBlank() && !updates.isEmpty()
					   ? Optional.of(new FileOutputModule(file, updates))
					   : Optional.empty();
			} catch(MalformedURLException | URISyntaxException e)
			{
				System.out.println("Failed to read path: " + file);
				return Optional.empty();
			}
		}
		
		@Override
		protected void populateDefaultConfig(JSONObject obj)
		{
			obj.put("file", "bpm.txt");
			obj.put("update", new JSONArray().put(HealthInfo.Field.BPM.getEventName()));
		}
	}
}