package com.example.mobileusersensingclient;

import java.io.Serializable;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.text.format.Time;
import android.util.Log;

public class CommStatModel {
	
	public static final String INTENT_COMM_RESULT =
		    "com.example.mobileusersensingclient.commstat.result";
	
	public static final long TWELVE_HOURS = 12 * 60 * 60 * 1000;
	
	private Context context;
	private static CommStatModel instance = null;
	
	private CommStatModel() {}
	private CommStatModel(Context context) {
		this.context = context;
	}
	
	/**
	 * Singleton
	 * @param context - only support one context
	 */
	public synchronized static 
		CommStatModel getInstance(Context context) {
		if (instance == null)
			instance = new CommStatModel(context);
		return instance;
	}
	
	/**
	 * the class represents the communiaction record for
	 * one user
	 * @author piyifan
	 */
	public class CommRecord implements Serializable {
		private static final long serialVersionUID = 1L;
		private int call = 0;
		private int sms = 0;
		
		public CommRecord(int call, int sms) {
			this.call = call;
			this.sms = sms;
		}
		
		public void addCall() {
			call++;
		}
		
		public void addSms() {
			sms++;
		}
		
		public int getCall(){
			return call;
		}
		
		public int getSms() {
			return sms;
		}
	}
	
	private HashMap<String, CommRecord> lastQueryResult;
	
	public void startSearcn(Time time) {
		if (MainActivity.ASYNCTASK_PARALLEL)
			new SearchTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, time);
		else
			new SearchTask().execute(time);
	}
	
	public HashMap<String, CommRecord> getLastResult() {
		return lastQueryResult;
	}
	
	private class SearchTask extends 
		AsyncTask<Time, Void, HashMap<String, CommRecord>> {
		
		@Override
		protected HashMap<String, CommRecord> doInBackground(Time... params) {
			Time time = params[0];
			long start, end;
			if (time == null) {
				//Want to fetch all records
				start = 0;
				Time now = new Time();
				now.setToNow();
				end = now.toMillis(true);
			}
			else {
				Log.i("pyf", time.format(MainActivity.DATE_FORMAT) + 
						" " + time.format(MainActivity.TIME_FORMAT));
				start = time.toMillis(true);
				end = start + TWELVE_HOURS + TWELVE_HOURS;
			}
			
			//Set the query for phone calls
			String[] phoneProjection = {CallLog.Calls.NUMBER};
			String[] args = {String.valueOf(start), String.valueOf(end)};
			Cursor phoneCursor = context.getContentResolver().query(
					CallLog.Calls.CONTENT_URI,
					phoneProjection,
					CallLog.Calls.DATE + " >= ? AND " 
					+ CallLog.Calls.DATE  + " <= ?",
					args,
					null);
			
			//Put the result in the HashMap
			HashMap<String, CommRecord> stats = 
					new HashMap<String, CommRecord>();
			if (phoneCursor.moveToFirst()) {
				do {
					String numStr = phoneCursor.getString(
							phoneCursor.getColumnIndex(
									CallLog.Calls.NUMBER));
					if (numStr == null) 
						numStr = "Unknown";
					else
						numStr = PhoneNumberUtils.formatNumber(numStr);
					if (!stats.containsKey(numStr))
						stats.put(numStr, new CommRecord(1, 0));
					else
						stats.get(numStr).addCall();
				} while(phoneCursor.moveToNext()); 
			}
			phoneCursor.close();
			
			//Set the query for sms
			Uri inboxUri = Uri.parse("content://sms/inbox");
			Uri sentUri = Uri.parse("content://sms/sent");
			String[] smsProjection = {"address"};
			Cursor smsCursor[] = new Cursor[2];
			smsCursor[0] = context.getContentResolver().query(
					inboxUri,
					smsProjection,
					"date >= ? AND date <= ?",
					args,
					null);
			smsCursor[1] = context.getContentResolver().query(
					sentUri,
					smsProjection,
					"date >= ? AND date <= ?",
					args,
					null);
			
			//Put the result in the HashMap
			for (int i = 0; i < 2; i++) {
				if (smsCursor[i].moveToFirst()) {
					do {
						String numStr = smsCursor[i].getString(
								smsCursor[i].getColumnIndex(
										"address"));
						if (numStr == null) 
							numStr = "Unknown";
						else
							numStr = PhoneNumberUtils.formatNumber(numStr);
						if (!stats.containsKey(numStr))
							stats.put(numStr, new CommRecord(0, 1));
						else
							stats.get(numStr).addSms();
					} while(smsCursor[i].moveToNext()); 
				}
				smsCursor[i].close();
			}
			
			return stats;
		}
		
		@Override
		protected void onPostExecute(HashMap<String, CommRecord> result) {
			lastQueryResult = result;
			
			//Send the broadcast to inform activity to refresh the View
			context.sendBroadcast(new Intent(CommStatModel.INTENT_COMM_RESULT));
		}
		
	}
	
}
