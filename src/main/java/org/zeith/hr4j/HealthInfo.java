package org.zeith.hr4j;

import java.time.Instant;
import java.util.*;
import java.util.function.*;

public class HealthInfo
{
	protected final List<IHealthListener> listeners = new ArrayList<>();
	protected final Value<Double> bpm = new Value<>(0.0D);
	
	public void addListener(IHealthListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(IHealthListener listener)
	{
		listeners.remove(listener);
	}
	
	public void updateBpm(double bpm, Instant timestamp)
	{
		if(bpm <= 0.5) return;
		double old = this.bpm.getValue();
		if(old == bpm) return;
		var res = this.bpm.update(bpm, timestamp);
		if(!res.ok) return;
		for(var l : listeners) l.onUpdate(this, old, Field.BPM);
	}
	
	public static class Value<T>
	{
		private T value;
		protected long lastUpdate;
		
		public Value(T value)
		{
			this.value = value;
			this.lastUpdate = System.currentTimeMillis();
		}
		
		public T getValue()
		{
			return value;
		}
		
		public Result<T> update(T value, Instant timestamp)
		{
			T prev = this.value;
			long ms = timestamp.toEpochMilli();
			if(ms > lastUpdate)
			{
				lastUpdate = ms;
				this.value = value;
				return new Result<>(prev, true);
			}
			return new Result<>(prev, false);
		}
	}
	
	public static class Result<T>
	{
		public final T res;
		public final boolean ok;
		
		public Result(T res, boolean ok)
		{
			this.res = res;
			this.ok = ok;
		}
	}
	
	public enum Field
	{
		BPM("bpm", i -> Double.toString(i.bpm.getValue()), i -> (int) Math.round(i.bpm.getValue()));
		
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