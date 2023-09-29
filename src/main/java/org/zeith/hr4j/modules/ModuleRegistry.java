package org.zeith.hr4j.modules;

import org.json.*;
import org.zeith.hr4j.input.*;
import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.output.*;
import org.zeith.hr4j.output.api.BaseOutputModule;

import java.util.*;
import java.util.function.*;

import static org.zeith.hr4j.utils.Cast.cast;

public class ModuleRegistry
{
	private static final Map<String, ModuleSpecs<? extends BaseInputModule>> INPUT_MODULES = new HashMap<>();
	private static final Map<String, ModuleSpecs<? extends BaseOutputModule>> OUTPUT_MODULES = new HashMap<>();
	
	public static void bootstrap()
	{
		registerInput("hyperate", HypeRate.Specs::new);
		registerInput("udp_app", UDPApp.Spec::new);
		
		registerOutput("websocket_server", WebSocketModule.Specs::new);
	}
	
	public static void registerInput(String id, Function<String, ModuleSpecs<? extends BaseInputModule>> specs)
	{
		INPUT_MODULES.put(id, specs.apply(id));
	}
	
	public static void registerOutput(String id, Function<String, ModuleSpecs<? extends BaseOutputModule>> specs)
	{
		OUTPUT_MODULES.put(id, specs.apply(id));
	}
	
	public static BaseInputModule createInputModule(JSONObject obj)
	{
		boolean enabled = obj.getBoolean("enabled");
		if(!enabled) return null;
		ModuleSpecs<? extends BaseInputModule> spec = INPUT_MODULES.get(obj.getString("id"));
		if(spec == null) return null;
		return spec.create(obj).orElse(null);
	}
	
	public static BaseOutputModule createOutputModule(JSONObject obj)
	{
		boolean enabled = obj.getBoolean("enabled");
		if(!enabled) return null;
		ModuleSpecs<? extends BaseOutputModule> spec = OUTPUT_MODULES.get(obj.getString("id"));
		if(spec == null) return null;
		return spec.create(obj).orElse(null);
	}
	
	public static JSONArray dumpAllRegistered(ModuleType type)
	{
		JSONArray arr = new JSONArray();
		for(ModuleSpecs<?> specs : type.registered.get())
			arr.put(specs.createDefaultConfig());
		return arr;
	}
	
	public enum ModuleType
	{
		INPUT(() -> cast(INPUT_MODULES.values())),
		OUTPUT(() -> cast(OUTPUT_MODULES.values()));
		
		final Supplier<Collection<ModuleSpecs<?>>> registered;
		
		ModuleType(Supplier<Collection<ModuleSpecs<?>>> registered)
		{
			this.registered = registered;
		}
	}
}