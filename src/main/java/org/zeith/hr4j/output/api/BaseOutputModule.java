package org.zeith.hr4j.output.api;

import org.zeith.hr4j.HealthInfo;

public abstract class BaseOutputModule
{
	public abstract void subscribe(HealthInfo info);
	
	public abstract void unsubscribe(HealthInfo info);
	
	public void start()
			throws Exception
	{
	}
	
	public void stop()
	{
	}
}