package org.zeith.hr4j;

import java.util.*;
import java.util.function.Function;

public class HealthInfo
{
	protected final List<IHealthListener> listeners = new ArrayList<>();
	protected double bpm;
	
	public void addListener(IHealthListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(IHealthListener listener)
	{
		listeners.remove(listener);
	}
	
	public void updateBpm(double bpm)
	{
		if(bpm <= 0.5) return;
		double old = this.bpm;
		if(old == bpm) return;
		this.bpm = bpm;
		for(var l : listeners) l.onUpdate(this, old, Field.BPM);
	}
	
	public enum Field
	{
		BPM(i -> Double.toString(i.bpm));
		
		private final Function<HealthInfo, String> toString;
		
		Field(Function<HealthInfo, String> toString)
		{
			this.toString = toString;
		}
		
		public String get(HealthInfo inf)
		{
			return toString.apply(inf);
		}
	}
}