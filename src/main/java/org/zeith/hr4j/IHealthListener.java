package org.zeith.hr4j;

public interface IHealthListener
{
	default void onBPMChange(double oldBPM, double newBPM)
	{
	}
	
	default void onUpdate(HealthInfo info, Object oldValue, HealthInfo.Field update)
	{
		switch(update)
		{
			case BPM:
				onBPMChange((Double) oldValue, info.bpm);
				break;
			default:
				return;
		}
	}
}