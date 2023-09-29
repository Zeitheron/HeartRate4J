package org.zeith.hr4j;

import java.util.*;
import java.util.function.*;

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
		BPM("bpm", i -> Double.toString(i.bpm), i -> (int) Math.round(i.bpm));
		
		private final String eventName;
		private final Function<HealthInfo, String> toString;
		private final ToIntFunction<HealthInfo> toByte;
		
		Field(String eventName, Function<HealthInfo, String> toString, ToIntFunction<HealthInfo> toByte)
		{
			this.eventName = eventName;
			this.toString = toString;
			this.toByte = toByte;
		}
		
		public String get(HealthInfo inf)
		{
			String val = toString.apply(inf);
			return val.replaceAll("[.]0+$", "");
		}
		
		public byte getByte(HealthInfo inf)
		{
			return (byte) Math.max(0, Math.min(toByte.applyAsInt(inf), 255));
		}
		
		public String getEventName()
		{
			return eventName;
		}
	}
}