package com.example.mobileusersensingclient;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

public class SensorService extends Service {
	
	public static final String IDENT_NEW_INFO = 
				"com.example.mobileusersensingclient.sensor.new";
	public static final String IDENT_SEND_INFO =
			    "com.example.mobileusersensingclient.sensor.send";
	public static final String INTENT_SEARCH_TASK =
				"com.example.mobileusersensingclient.sensor.search";
	
	public static final String IDENT_CONTENT_RESULT = 
			    "com.example.mobileusersensingclient.sensor.sendresult";
	public static final String IDENT_CONTENT_INFO = 
			    "com.example.mobileusersensingclient.sensor.sendinfo";
	
	private static final String INTENT_COLLECT_TASK = 
			"com.example.mobileusersensingclient.sensor.collecttask";
	
	public static final int SEND_RESULT_SUCCESS = 0;
	public static final int SEND_RESULT_FAIL_NO_USER = 1;
	public static final int SEND_RESULT_FAIL_NETWORK = 2;
	
	private static final int NOTI_ID = 1;
	private static final long BACKUP_TIME = 30000L;
	
	private final IBinder mBinder = new LocalBinder();
	
	private SensorServiceModel model;
	
	private NotificationManager mNotificationManager;
	
	private AlarmManager alarmManager;

	private CollectReceiver mCollectReceiver;
	private IntentFilter mCollectFilter;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SensorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorService.this;
        }
    }
    
    @Override
	public void onCreate() {
    	super.onCreate();
    	
    	//Get the sensor service model
		model = new SensorServiceModel(this);
		
		//Set notification
		mNotificationManager = 
				(NotificationManager) getSystemService(
						Context.NOTIFICATION_SERVICE);
		
		Intent intent = new Intent(this, MainActivity.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(
				this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification noti =  new Notification.Builder(this)
				.setContentTitle("MUUS")
				.setContentText(
						this.getResources().getString(R.string.noti_content)
						+ "...")
				.setSmallIcon(R.drawable.ic_stat_service)
				.setLargeIcon(BitmapFactory.decodeResource(
						this.getResources(), R.drawable.ic_stat_service))
				.setContentIntent(contentIntent)
				.getNotification();
		 noti.flags |= Notification.FLAG_NO_CLEAR;
		 noti.flags |= Notification.FLAG_ONGOING_EVENT;
		 
		 mNotificationManager.notify(NOTI_ID, noti);
		 
		//Set alarm manager
		alarmManager = (AlarmManager)this.
							getSystemService(Context.ALARM_SERVICE);
		
		//Register Receiver
		mCollectReceiver = new CollectReceiver();
		mCollectFilter = new IntentFilter(INTENT_COLLECT_TASK);
		this.registerReceiver(mCollectReceiver, mCollectFilter);
		
		//Start a periodic backup and sensor reading task 
	    waitForNewCollect();
	    if (MainActivity.ASYNCTASK_PARALLEL)
			new BackupTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, null);
		else
			new BackupTask().execute();
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	//Cancel the notification
    	mNotificationManager.cancel(NOTI_ID);
    	
    	//Unregister Listener
    	model.unRegisterReceviers();
    	this.unregisterReceiver(mCollectReceiver);
    }
    
    /**
     * Collect and Send button
     */
    public void collectAndSendOnce() {
    	model.readOnce(SensorInfoPack.FRONT_CALL);
    }
    
    public SensorInfoPack getLastSensorInfo() {
    	return model.getLastRead();
    }
    
    public ArrayList<SensorInfoPack> getLastSearch() {
    	return model.getLastSearch();
    }
    
    public void startSearchDay(Time time) {
    	model.searchDay(time);
    }
    
    private long getCollectTime() {
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	return Setting.VALUE_FREQUENCY[Integer.valueOf(sharedPref.getString(
    									Setting.KEY_PREF_FREQUENCY, "0"))];
    }

    private void waitForNewCollect() {
		Log.i("pyf", "bg collect");
		model.readOnce(SensorInfoPack.BACK_CALL);
		
    	Intent intent = new Intent(INTENT_COLLECT_TASK);
		PendingIntent sender=
		        PendingIntent.getBroadcast(this, 0, intent, 0);
		long readTime = SystemClock.elapsedRealtime() 
					+ getCollectTime();
		
		alarmManager.cancel(sender);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP
				, readTime, sender);
    }
    
    private class CollectReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Start a new collect task
			waitForNewCollect();
		}
    }
    
    private class BackupTask extends AsyncTask<Void, Void, Void> {
    	SensorInfoPack info;

		@Override
		protected Void doInBackground(Void... arg0) {
			info = model.getBackup();
			Log.i("pyf", "backup!");
			try {
				Thread.sleep(BACKUP_TIME);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			return null;
 		}
		
		@Override
		protected void onPostExecute(Void v) {
			model.sendInfo(info);
			new BackupTask().execute();
		}
    	
    }
    
}
