package org.zeith.hr4j.input.api;

import org.zeith.hr4j.HealthInfo;

public abstract class BaseInputModule
{
	private HealthInfo info;
	
	public abstract void start() throws Exception;
	
	public abstract void stop();
	
	public BaseInputModule withHealthInfo(HealthInfo info)
	{
		this.info = info;
		return this;
	}
	
	public HealthInfo getHealthInfo()
	{
		if(info == null) info = new HealthInfo();
		return info;
	}
}