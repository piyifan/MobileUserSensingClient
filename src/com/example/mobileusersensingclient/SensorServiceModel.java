package com.example.mobileusersensingclient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

public class SensorServiceModel {
	
	private static final String INTENT_READ_TASK = 
			"com.example.mobileusersensingclient.sensor.readtask.intent";
	private static final String INTENT_READ_TASK_TYPE = 
			"com.example.mobileusersensingclient.sensor.readtask.type";
	
	private SensorCollection sensors;
	
	private SensorInfoPack lastRead;
	
	private ArrayList<SensorInfoPack> lastSearch;
	
	private Context context;
	
	private BackupDB backupDB;
	
	private AlarmManager alarmManager;
	private ReadTaskReceiver mReceiver;
	private IntentFilter mFilter;
	
	private int readID = 0;
	
	@SuppressWarnings("unused")
	private SensorServiceModel() {}
	
	public SensorServiceModel(Context context) {
		this.context = context;
		sensors = new SensorCollection(context);
		
		//Set Backup Database
		backupDB = new BackupDB(context);
		
		//Set alarm manager
		alarmManager = (AlarmManager)context.
						getSystemService(Context.ALARM_SERVICE);
		
		//Set receiver
		mReceiver = new ReadTaskReceiver();
		mFilter = new IntentFilter(INTENT_READ_TASK);
		context.registerReceiver(mReceiver, mFilter);
	}
	
	public void unRegisterReceviers() {
		context.unregisterReceiver(mReceiver); 
	}
	
	public SensorInfoPack getLastRead() {
		return lastRead;
	}
	
	public ArrayList<SensorInfoPack> getLastSearch() {
		return lastSearch;
	}
	
	public void readOnce(boolean type) {
		//Start Listen
		sensors.startListenSensorInfo();
		

		//Set a alarm 
	    long readTime = SystemClock.elapsedRealtime() 
	    		+ SensorCollection.WAIT_TIME;
	    
		//Set a intent
		Intent intent = new Intent(INTENT_READ_TASK);
		intent.putExtra(INTENT_READ_TASK_TYPE, type);
		readID++;
		PendingIntent sender=
		        PendingIntent.getBroadcast(context
		        		, readID     //Use this to get different 
		        					 //PendingIntent in order to
		        		             //set multi alarm
		        		, intent, 0);
		
		alarmManager.cancel(sender);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP
				, readTime, sender);
		
	}
	
	public SensorInfoPack getBackup() {
		SensorInfoPack info = backupDB.get();
		if (info != null)
			info.setCallType(SensorInfoPack.BACK_CALL);
		return info;
	}
	
	public void searchDay(Time time) {
		Log.i("pyf_search", "hi");
		if (MainActivity.ASYNCTASK_PARALLEL)
			new SearchTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, time);
		else
			new SearchTask().execute(time);
	}
	
	public void sendInfo(SensorInfoPack info) {
		if (info == null) return;
		Log.i("pyf_send", "prepare to send:" + info.sendInfo());
		if (MainActivity.ASYNCTASK_PARALLEL)
			new SendInfoTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, info);
		else
			new SendInfoTask().execute(info);
	}
	
	private void saveUnsent(SensorInfoPack info) {
		backupDB.insert(info);
	}
	
	private class ReadTaskReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context receive, Intent res) {
			//Get the reading
			SensorInfoPack result = sensors.getSensorInfo();
			result.setCallType(res.getBooleanExtra(
					INTENT_READ_TASK_TYPE, SensorInfoPack.BACK_CALL));
			
			//Set last read result
	        lastRead = result;
	        

	        //Try to send
	        sendInfo(result);
	        
	        //Broadcast
	        Intent intent = new Intent(SensorService.IDENT_NEW_INFO);
	        
	        context.sendBroadcast(intent);
		}
		
	}
	
	private static final String SEND_INFO_ADDR = ":8080/Web/MobileServlet?";
	private static final String SEARCH_INFO_ADDR = ":8080/Web/MobileRequestServlet?";
	private static final String SEND_RESPONSE_SUCCESS = "success";
	//private static final String SEND_RESPONSE_ERROR = "error";
	
	private class SendInfoTask extends AsyncTask<SensorInfoPack, Void, Integer> {
		
		private SensorInfoPack info;
		
		@Override
		protected Integer doInBackground(SensorInfoPack... params) {
			info = params[0];
			Log.i("pyf_send", "start send " + info.sendInfo());
			SharedPreferences sharedPref = PreferenceManager.
						getDefaultSharedPreferences(context);
			String serverAddr = sharedPref.getString(
					Setting.KEY_PREF_SERVER_ADDRESS, "");
			String username = sharedPref.getString(
					Setting.KEY_PREF_USERNAME, "null");
			String password = sharedPref.getString(
					Setting.KEY_PREF_PASSWORD, "");
			String urlString = "http://" + serverAddr + SEND_INFO_ADDR +
						  "username=" + username + "&" +
				          "password=" + password + "&" +
					       info.sendInfo();
			Log.i("pyf", "Send url: " + urlString);
			URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				Log.i("pyf", "invalid uri");
				saveUnsent(info);
				return SensorService.SEND_RESULT_FAIL_NETWORK;
			}
			HttpURLConnection urlConnection;
			Scanner in;
			try {
				 urlConnection = 
						(HttpURLConnection) url.openConnection();
				 urlConnection.setConnectTimeout(
						 MainActivity.TIMEOUT_CONNECT);
				 urlConnection.setReadTimeout(
						 MainActivity.TIMEOUT_SOCKET);
				 urlConnection.connect();
				 in = new Scanner(new BufferedInputStream(
							urlConnection.getInputStream()));
			} catch (IOException e) {
				Log.i("pyf", "i/o error when open url");
				Log.i("pyf_io", e.getClass().toString());
				saveUnsent(info);
				return SensorService.SEND_RESULT_FAIL_NETWORK;
			}
			
			if (!in.hasNextLine()) {
				in.close();
				urlConnection.disconnect();
				Log.i("pyf", "can not get send feedback");
				saveUnsent(info);
				return SensorService.SEND_RESULT_FAIL_NETWORK;
			}
			
			String resString = in.nextLine();
			in.close();
			urlConnection.disconnect();
			
			Log.i("pyf", "Result String = " + resString);
			
			if (resString.contains(SEND_RESPONSE_SUCCESS))
				return SensorService.SEND_RESULT_SUCCESS;
			else {
				saveUnsent(info);
				return SensorService.SEND_RESULT_FAIL_NO_USER;
			}
		}
		
		@Override
	    protected void onPostExecute(Integer result) {
			//Broadcast the result
			Log.i("pyf_send", "after send " + info.sendInfo());
			Intent intent = new Intent(SensorService.IDENT_SEND_INFO);
			intent.putExtra(SensorService.IDENT_CONTENT_RESULT, result);
			intent.putExtra(SensorService.IDENT_CONTENT_INFO, info);
			context.sendBroadcast(intent);
	    }
		
	}
		
	private class SearchTask 
		extends AsyncTask<Time, Void, ArrayList<SensorInfoPack>> {
		
		private int searchResult;

		@Override
		protected ArrayList<SensorInfoPack> doInBackground(Time... arg) {
			Time time = arg[0];
			SharedPreferences sharedPref = PreferenceManager.
					getDefaultSharedPreferences(context);
			String serverAddr = sharedPref.getString(
				Setting.KEY_PREF_SERVER_ADDRESS, "");
			String username = sharedPref.getString(
				Setting.KEY_PREF_USERNAME, "null");
			String password = sharedPref.getString(
				Setting.KEY_PREF_PASSWORD, "");
			String urlString = "http://" + serverAddr + SEARCH_INFO_ADDR +
					  "username=" + username + "&" +
			          "password=" + password + "&" +
				      "date=" + time.format(MainActivity.DATE_FORMAT);
			Log.i("pyf_search", "Search url: " + urlString);
			URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				Log.i("pyf_search", "invalid uri");
				searchResult = SensorService.SEND_RESULT_FAIL_NETWORK;
				return null;
			}
			HttpURLConnection urlConnection;
			Scanner in;
			try {
				urlConnection = 
						(HttpURLConnection) url.openConnection();
				urlConnection.setConnectTimeout(
						MainActivity.TIMEOUT_CONNECT);
				urlConnection.setReadTimeout(
						MainActivity.TIMEOUT_SOCKET);
				urlConnection.connect();
				in = new Scanner(new BufferedInputStream(
						urlConnection.getInputStream()));
			} catch (IOException e) {
				Log.i("pyf_search", "i/o error when open url");
				searchResult = SensorService.SEND_RESULT_FAIL_NETWORK;
				return null;
			}
		
			if (!in.hasNextLine()) {
				in.close();
				urlConnection.disconnect();
				Log.i("pyf_search", "can not get send feedback");
				searchResult = SensorService.SEND_RESULT_FAIL_NETWORK;
				return null;
			}
		
			String resString = in.nextLine();
			Log.i("pyf_search", "Result String = " + resString);
			if (!resString.contains(SEND_RESPONSE_SUCCESS)) {
				Log.i("pyf_search", "wrong user info");
				searchResult = SensorService.SEND_RESULT_FAIL_NO_USER;
				return null;
			}
			
			ArrayList<SensorInfoPack> ans = new ArrayList<SensorInfoPack>();
			while (in.hasNextLine()) {
				String infoStr[] = in.nextLine().split("&");
				if (infoStr.length != SensorCollection.NUM_SENSORS + 1)
					continue;
				ans.add(new SensorInfoPack(infoStr));
			}
			Collections.sort(ans, new SensorInfoPack.MyComparator());
			
			in.close();
			urlConnection.disconnect();
			
			searchResult = SensorService.SEND_RESULT_SUCCESS;
			return ans;
		}
		
		@Override
		protected void onPostExecute(ArrayList<SensorInfoPack> result) {
			Intent intent = new Intent(SensorService.INTENT_SEARCH_TASK);
			intent.putExtra(SensorService.IDENT_CONTENT_RESULT, searchResult);
			lastSearch = result;
			context.sendBroadcast(intent);
		}
		
	}
	
}
