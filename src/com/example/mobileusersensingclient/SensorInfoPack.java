package com.example.mobileusersensingclient;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import android.text.format.Time;

public class SensorInfoPack 
	implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public static final boolean BACK_CALL = false;
	public static final boolean FRONT_CALL = true;
	
	
	private String info[];
	private long time;
	
	/**
	 * callId = false if it is the background call
	 * callId = true if user click the collect and send button
	 */
	private boolean callType;
	
	@SuppressWarnings("unused")
	private SensorInfoPack() {}
	
	public SensorInfoPack(String info[], long time) {
		this.info = info;
		this.time = time;
	}
	
	public SensorInfoPack(String info[]) {
		if (info.length == SensorCollection.NUM_SENSORS) {
			this.info = info;
			Time tmpTime = new Time();
			tmpTime.setToNow();
			time = tmpTime.toMillis(true);
		}
		else {
			this.info = new String[
			                 SensorCollection.NUM_SENSORS];
			for (int i = 0; 
					i < SensorCollection.NUM_SENSORS; i++)
				this.info[i] = info[i];
			SimpleDateFormat dateFormatter = 
					new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss");
			Date date;
			try {
				 date = dateFormatter.parse(
						info[SensorCollection.NUM_SENSORS]);
			} catch (ParseException e) {
				time = 0;
				return;
			}
			time = date.getTime();
		}
	}
	
	public void setCallType(boolean x) {
		callType = x;
	}
	
	public boolean getCallType() {
		return callType;
	}
	
	public String getInfo(int index) {
		return info[index];
	}
	
	public String getTime() {
		Time tmpTime = new Time();
		tmpTime.set(time);
		return tmpTime.format(MainActivity.TIME_FORMAT);
	}
	
	public long getRawTime() {
		return time;
	}
	
	public String sendInfo() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < SensorCollection.NUM_SENSORS; i++) {
			res.append(SensorCollection.SEND_SENSOR_NAME[i]);
			res.append("=");
			res.append(info[i]);
			res.append("&");
		}
		res.append(SensorCollection.
				SEND_SENSOR_NAME[SensorCollection.NUM_SENSORS]);
		res.append("=");
		res.append(getTime());
		return res.toString();
	}

	public static class MyComparator 
		implements Comparator<SensorInfoPack>{

		@Override
		public int compare(SensorInfoPack lhs, SensorInfoPack rhs) {
			if (lhs.getRawTime() < rhs.getRawTime())
				return -1;
			else
				if (lhs.getRawTime() == rhs.getRawTime())
					return 0;
				else
					return 1;
		}
	}
	
}
