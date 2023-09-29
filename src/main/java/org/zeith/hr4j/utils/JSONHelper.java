package org.zeith.hr4j.utils;

import org.json.*;

public class JSONHelper
{
	public static JSONObject deepMerge(JSONObject source, JSONObject target)
			throws JSONException
	{
		for(String key : JSONObject.getNames(source))
		{
			Object value = source.get(key);
			if(!target.has(key))
			{
				// new value for "key":
				target.put(key, value);
			} else
			{
				Object found = target.get(key);
				
				// existing value for "key" - recursively deep merge:
				if(value instanceof JSONObject)
				{
					JSONObject valueJson = (JSONObject) value;
					if(found instanceof JSONObject)
						deepMerge(valueJson, (JSONObject) found);
					else
						target.put(key, value);
				} else
				{
					target.put(key, value);
				}
			}
		}
		return target;
	}
	
	public static JSONArray deepMerge(JSONArray source, JSONArray target)
			throws JSONException
	{
		JSONArray arr = new JSONArray();
		arr.putAll(source);
		arr.putAll(target);
		return arr;
	}
}