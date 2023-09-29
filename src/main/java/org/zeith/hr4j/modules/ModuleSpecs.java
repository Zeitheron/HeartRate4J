package org.zeith.hr4j.modules;

import org.json.JSONObject;

import java.util.Optional;

public abstract class ModuleSpecs<T>
{
	protected final String id;
	
	public ModuleSpecs(String id)
	{
		this.id = id;
	}
	
	public abstract Optional<T> create(JSONObject obj);
	
	protected abstract void populateDefaultConfig(JSONObject obj);
	
	public final JSONObject createDefaultConfig()
	{
		JSONObject obj = new JSONObject();
		populateDefaultConfig(obj);
		obj.put("id", getId());
		obj.put("enabled", true);
		return obj;
	}
	
	public final String getId()
	{
		return id;
	}
}