package org.zeith.hr4j;

import org.zeith.hr4j.input.api.BaseInputModule;
import org.zeith.hr4j.output.api.BaseOutputModule;

import java.util.*;

public class ModuleBridge
	implements IHealthListener
{
	public final HealthInfo info = new HealthInfo();
	
	public final BaseInputModule input;
	public final List<BaseOutputModule> outputs;
	
	public ModuleBridge(BaseInputModule input, List<BaseOutputModule> outputs)
	{
		this.input = input;
		this.outputs = List.copyOf(outputs);
		
		info.addListener(this);
	}
	
	public void start()
			throws Exception
	{
		input.withHealthInfo(info);
		input.start();
		for(BaseOutputModule output : outputs)
		{
			output.subscribe(info);
			output.start();
		}
	}
	
	public void stop()
	{
		input.stop();
		for(BaseOutputModule output : outputs)
		{
			output.stop();
			output.unsubscribe(info);
		}
	}
	
	@Override
	public void onUpdate(HealthInfo info, Object oldValue, HealthInfo.Field update)
	{
		System.out.printf("Got %s update: %s -> %s", update.name(), Objects.toString(oldValue).replaceAll("[.]0+$", ""), Objects.toString(update.get(info)));
		System.out.println();
	}
}