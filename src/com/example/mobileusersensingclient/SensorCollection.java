package com.example.mobileusersensingclient;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class SensorCollection {

	public static final int NUM_SENSORS = 4;
	
	public static final int WIFI = 3;
	public static final int LOCATION = 0;
	public static final int LIGHT = 1;
	public static final int ACCELERATION = 2;
	public static final long WAIT_TIME = 5000L;
	
	public static final String UNGET_MSG = "--";
	
	public static final String SEND_SENSOR_NAME[] = {
				"location", "light", "accelerator", "wifi", "time"};
	
	private SensorManager mSensorManager;
	private WifiManager mWifiManager;
	private LocationManager mLocationManager;
	private Context context;
	
	
	private boolean inListen = false;
	
	@SuppressWarnings("unused")
	private SensorCollection() {}
	
	private SensorProto sensors[] = new SensorProto[NUM_SENSORS]; 
	
	public SensorCollection(Context context) {
		this.context = context;
		//Get some managers
		mSensorManager = (SensorManager) context.
					getSystemService(Context.SENSOR_SERVICE);
		mWifiManager = (WifiManager) context.
					getSystemService(Context.WIFI_SERVICE);
		mLocationManager = (LocationManager) context.
				getSystemService(Context.LOCATION_SERVICE);
		
		//Initialize the sensors
		sensors[0] = new LocationSensor();
		sensors[1] = new LightSensor();
		sensors[2] = new AccelerationSensor();
		sensors[3] = new WifiSensor();
	}
	
	public synchronized void startListenSensorInfo() {
		if (inListen) return;
		
		inListen = true;
		
		//Start Listen
		for (int i = 0; i < NUM_SENSORS; i++)
			sensors[i].startListen();
	}
	
	public synchronized SensorInfoPack getSensorInfo() {
		String res[] = new String[NUM_SENSORS];
		
		//Not start a listen process
		if (!inListen) {
			for (int i = 0; i < NUM_SENSORS; i++)
				res[i] = UNGET_MSG;
			return new SensorInfoPack(res);
		}
		inListen = false;
		
		//Get the info
		for (int i = 0; i < NUM_SENSORS; i++) {
			res[i] = sensors[i].getInfo();
			if (res[i] == null)
				res[i] = UNGET_MSG;
		}
		
		return new SensorInfoPack(res);
	}
	
	
	abstract class SensorProto {
		
		protected int type;
		protected boolean successListen = false;
		protected boolean successGet = false;
		
		/**
		 * Waiting time for sensor reading
		 */
		public static final int SENSOR_WAIT_TIME = 500;
		public static final int WIFI_WAIT_TIME = 3000;
		
		/**
		 * Get the type ID of the sensor (Store in SensorProto)
		 * @return the type ID of the sensor (Store in SensorProto)
		 */
		abstract int getType();
		
		/**
		 * Set up to listen the sensor
		 */
		abstract void startListen();
		
		/**
		 * Get the current state of the Sensor. Must
		 * start to listen at first.
		 * @return the info string or null if the sensor
		 * is not with problem.
		 */
		abstract String getInfo();
		
	}
	
	/**
	 * Sensor class for Acceleration
	 * @author piyifan
	 */
	class AccelerationSensor extends SensorProto {
		
		private float gravity[] = new float[3];
		private float linear_acceleration[] = new float[3];
		private Sensor mAcc;
		private SensorEventListener mListener;
		
		@Override
		void startListen() {
			successListen = false;
			
			//Have not manager
			if (mSensorManager == null) return;
			
			//Get sensor
			mAcc = mSensorManager.getDefaultSensor(
	   				 Sensor.TYPE_ACCELEROMETER);
			if (mAcc == null) return;
			
			//Register the listener
			mListener = new AccelerationListener();
			if (!mSensorManager.registerListener(mListener, mAcc,
						   SensorManager.SENSOR_DELAY_FASTEST))
				return;
			
			successListen = true;
			successGet = false;
		}
		
		@Override
		synchronized String getInfo() {
			if (!successListen) return null;
			successListen = false;
			
			//Unregister the listener for power issue
			mSensorManager.unregisterListener(mListener, mAcc);
			
			//Return result
			if (successGet) return linear_acceleration[0] + "," +
								linear_acceleration[1] + "," +
								linear_acceleration[2];
			else return null;
		}

		@Override
		int getType() {
			return SensorCollection.ACCELERATION;
		}
		
		class AccelerationListener implements SensorEventListener {
			@Override
			public void onSensorChanged(SensorEvent event){
				// In this example, alpha is calculated as t / (t + dT),
				// where t is the low-pass filter's time-constant and
				// dT is the event delivery rate.

				final float alpha = 0.8f;

				// Isolate the force of gravity with the low-pass filter.
				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

				// Remove the gravity contribution with the high-pass filter.
				linear_acceleration[0] = event.values[0] - gravity[0];
				linear_acceleration[1] = event.values[1] - gravity[1];
				linear_acceleration[2] = event.values[2] - gravity[2];

				successGet = true;
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		}

	}
	
	/**
	 * Sensor class for light
	 * @author piyifan
	 *
	 */
	class LightSensor extends SensorProto {
		
		private float lux;
		private Sensor mLgt;
		private SensorEventListener mListener;
		
		@Override
		int getType() {
			return SensorCollection.LIGHT;
		}
		
		@Override
		void startListen() {
			successListen = false;
			if (mSensorManager == null) return;
			//Get sensor
			mLgt = mSensorManager.getDefaultSensor(
	   				 Sensor.TYPE_LIGHT);
			if (mLgt == null) return;
			
			//Register the listener
			SensorEventListener mListener = new LightListener();
			if (!mSensorManager.registerListener(mListener, mLgt,
						   SensorManager.SENSOR_DELAY_FASTEST))
				return;
			
			successGet = false;
			successListen = true;
		}

		@Override
		synchronized String getInfo() {
			if (!successListen) return null;
			successListen = false;
			
			//Unregister the listener for power issue
			mSensorManager.unregisterListener(mListener, mLgt);
			
			//Return result
			if (successGet) return "" + lux;
			else return null;
		}
		
		class LightListener implements SensorEventListener {
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}

			@Override
			public synchronized void onSensorChanged(SensorEvent event) {
				lux = event.values[0];
				successGet = true;
			}
		}

	}
	
	class WifiSensor extends SensorProto {
		
		private boolean wifiState;
		
		@Override
		int getType() {
			return SensorCollection.WIFI;
		}
		
		@Override
		void startListen() {
			successListen = false;
			
			if (mWifiManager == null) return;
			
			//Open the Wifi
			wifiState = mWifiManager.isWifiEnabled();
	        if (!wifiState) {
	        	if (!mWifiManager.setWifiEnabled(true))
	            	return;
	        }
	        
	        //Search for Wifi networks
	        if (!mWifiManager.startScan()) return;
	        
	        successListen = true;
		}

		@Override
		String getInfo() {
			if (!successListen) return null;
			successListen = false;
	        
	        //Find the strongest Wifi signal
	        List<ScanResult> list = mWifiManager.getScanResults();
	        
	        //Close the Wifi if it is closed before
	        if (!wifiState)
	        	mWifiManager.setWifiEnabled(false);
	        
	        if (list == null || list.size() == 0) return null;
	        int maxLevel = Integer.MIN_VALUE;
	        ScanResult res = null;
	        for (ScanResult x : list) {
	        	if (x.level > maxLevel) {
	        		maxLevel = x.level;
	        		res = x;
	        	}
	        }
	 
	        //Return the result
	        return res.SSID;
		}
		
	}
	
	class LocationSensor extends SensorProto {
		
		private LocationListener mListener;
		private String provider;
		private Location mLocation;

		@Override
		int getType() {
			return SensorCollection.LOCATION;
		}
		
		@Override
		void startListen() {
		}
		
		@Override
		String getInfo() {
			mLocationManager = (LocationManager) context.
					getSystemService(Context.LOCATION_SERVICE);
			
			Location location;
			Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_FINE); 
	        criteria.setPowerRequirement(Criteria.POWER_HIGH);
	        criteria.setAltitudeRequired(true);
	        criteria.setBearingRequired(false);
	        criteria.setSpeedRequired(false);
	        
	        //Get the best provider
	        provider = 
	        		mLocationManager.getBestProvider(criteria, true);
	        if (provider == null) return null;
	        Log.i("pyf_gps", provider);
			location = 
	        		mLocationManager.getLastKnownLocation(provider);
	        
	        if (location == null) return null;
	        else return String.valueOf(location.getLongitude())
	        			+"," + String.valueOf(location.getLatitude());
		}

		class MyLocationListener implements LocationListener {

			@Override
			public void onLocationChanged(Location location) {
				successGet = true;
				mLocation = location;
				Log.i("pyf", "location changed!");
			}

			@Override
			public void onProviderDisabled(String provider) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {}
			
		}
		
	}
	
}

