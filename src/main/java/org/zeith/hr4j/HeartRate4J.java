package org.zeith.hr4j;

import org.json.*;
import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.modules.ModuleRegistry;
import org.zeith.hr4j.output.api.BaseOutputModule;
import org.zeith.hr4j.utils.JSONHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class HeartRate4J
{
	public static final String VERSION = "1.0.1";
	
	public static final Path RUN_DIR;
	
	public static Callable<ModuleBridge> configuredBridge;
	public static ModuleBridge bridge;
	
	static
	{
		try
		{
			RUN_DIR = Path.of(HeartRate4J.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.toAbsolutePath()
					.getParent();
		} catch(URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static JSONObject createDefault()
	{
		var obj = new JSONObject();
		
		obj.put("input", "Replace this string with object from array $valid_inputs");
		obj.put("outputs", new JSONArray()
				.put("Put as many outputs with different options as you want. Use $valid_outputs for refefence.")
		);
		
		obj.put("$valid_inputs", ModuleRegistry.dumpAllRegistered(ModuleRegistry.ModuleType.INPUT));
		obj.put("$valid_outputs", ModuleRegistry.dumpAllRegistered(ModuleRegistry.ModuleType.OUTPUT));
		
		var arr = new JSONArray();
		for(HealthInfo.Field value : HealthInfo.Field.values()) arr.put(value.getEventName());
		obj.put("$valid_events", arr);
		
		return obj;
	}
	
	public static void setupConfigs()
			throws IOException
	{
		var cfgPath = RUN_DIR.resolve("configs.json");
		System.out.println("Loading configs at " + cfgPath);
		var def = createDefault();
		var merged = createDefault();
		if(Files.exists(cfgPath))
			merged = JSONHelper.deepMerge(((JSONObject) new JSONTokener(Files.readString(cfgPath)).nextValue()), merged);
		if(!def.similar(merged) || !Files.exists(cfgPath))
			Files.writeString(cfgPath, merged.toString(2));
		
		var inputRaw = merged.get("input");
		var outputsRaw = merged.getJSONArray("outputs");
		
		configuredBridge = () ->
		{
			if(!(inputRaw instanceof JSONObject))
				throw new JSONException("Input has not been set to a valid object.");
			JSONObject inputObj = (JSONObject) inputRaw;
			BaseInputModule in = ModuleRegistry.createInputModule(inputObj);
			if(in == null)
				throw new JSONException(
						"Input module has not been " + (inputObj.optBoolean("enabled") ? "found" : "enabled"));
			List<BaseOutputModule> outs = new ArrayList<>();
			for(int i = 0; i < outputsRaw.length(); i++)
			{
				var o = outputsRaw.get(i);
				if(!(o instanceof JSONObject)) continue;
				var out = ModuleRegistry.createOutputModule((JSONObject) o);
				if(out == null) continue;
				outs.add(out);
			}
			
			System.out.println("Input: " + in.getClass().getSimpleName());
			System.out.println("Outputs: " + outs.stream().map(Object::getClass).map(Class::getSimpleName)
					.collect(Collectors.joining(", ", "[", "]")));
			
			return new ModuleBridge(in, outs);
		};
	}
	
	public static void startBridge()
	{
		if(bridge != null)
		{
			bridge.stop();
			bridge = null;
		}
		
		try
		{
			bridge = configuredBridge.call();
			if(bridge == null)
			{
				System.err.println("Bridge has not been created.");
				return;
			}
			bridge.start();
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
			throws IOException
	{
		System.out.println("Starting HeartRate4J v" + VERSION);
		ModuleRegistry.bootstrap();
		setupConfigs();
		startBridge();
	}
}