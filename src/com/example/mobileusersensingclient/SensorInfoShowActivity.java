package com.example.mobileusersensingclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.mobileusersensingclient.SensorService.LocalBinder;

public class SensorInfoShowActivity extends Activity {
	
	/**
	 * Resources
	 */
	Resources resources;
	
	/**
	 * Service
	 */
	private SensorService mService;
	private boolean mBound;
	
	/**
	 * Listview and Adapter
	 */
	private ListView sensorInfoView;
	private ArrayAdapter<String> sensorInfoAdapter;
	private String[] currentSensorInfo;
	
	
	/**
	 * Broadcast receiver
	 */
	private NewSensorInfoReceiver mNewSensorInfoReceiver;
	private IntentFilter newInfoFilter;
	private SendSensorReceiver mSendSensorReceiver;
	private IntentFilter sendInfoFilter;
	
	/**
	 * be true if the read&send button is clicked but
	 * the result has not been sent yet.
	 */
	private boolean clickedReadAndSend;
	private static final String STATE_CLICKED = "clickedReadAndSend";
	
	private String sensorNames[];
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    // Save the state
	    savedInstanceState.putBoolean(STATE_CLICKED, clickedReadAndSend);
	    
	    // Always call the superclass so it can save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_sensor_info_show, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_sensor_info_home:
	        	intent = new Intent(this, MainActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_info_setting:
	        	intent = new Intent(this, Setting.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_info_sensor_search:
	        	intent = new Intent(this, SensorInfoSearchActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_info_comm_stat:
	        	intent = new Intent(this, CommStatActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        default: 
	        	return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_info_show);
        
        //Restore saved state
        if (savedInstanceState != null) {
        	clickedReadAndSend = savedInstanceState.getBoolean(STATE_CLICKED);
        } else {
        	clickedReadAndSend = false;
        }
        
        //Set resources
        resources = this.getResources();
        
        //Get sensor names
        sensorNames = resources.getStringArray(R.array.sensor_names);
        
        //Set the ListView of sensor info 
        currentSensorInfo = new String[SensorCollection.NUM_SENSORS + 1];
        sensorInfoAdapter = new ArrayAdapter<String>(
      		  this,
      		  android.R.layout.simple_list_item_1,
      		  android.R.id.text1, 
      		  currentSensorInfo);
        updateSensorInfo(null);
        sensorInfoView = (ListView) findViewById(R.id.sensor_info_list);
        sensorInfoView.setAdapter(sensorInfoAdapter);
        
        //Set bound flag
        mBound = false;
        
        //Set clickedReadAndSend
        clickedReadAndSend = false;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to SensorService
        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }	
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (mBound) 
    		updateSensorInfo(mService.getLastSensorInfo());
    	mNewSensorInfoReceiver = new NewSensorInfoReceiver();
        newInfoFilter = new IntentFilter(SensorService.IDENT_NEW_INFO);
        mSendSensorReceiver = new SendSensorReceiver();
        sendInfoFilter = new IntentFilter(SensorService.IDENT_SEND_INFO);
        
    	//Register the broadcast receiver
    	registerReceiver(mNewSensorInfoReceiver, newInfoFilter);
    	registerReceiver(mSendSensorReceiver, sendInfoFilter);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	//Unregister the broadcast receiver
    	unregisterReceiver(mNewSensorInfoReceiver);
    	unregisterReceiver(mSendSensorReceiver);
    }

    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            
            //Refresh the sensor info view
            updateSensorInfo(mService.getLastSensorInfo());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    /**
     * update currentSensorInfo with new SensorInfoPack
     * @param info - new sensor info
     */
    private void updateSensorInfo(SensorInfoPack info) {
    	for (int i = 0; i < SensorCollection.NUM_SENSORS; i++) {
    		if (info == null) 
    			currentSensorInfo[i] = sensorNames[i] + 
						   " : " + SensorCollection.UNGET_MSG;
    		else
    			currentSensorInfo[i] = sensorNames[i] + 
    							   " : " +info.getInfo(i);
    	}
    	if (info == null) 
    		currentSensorInfo[SensorCollection.NUM_SENSORS] = 
    				sensorNames[SensorCollection.NUM_SENSORS] 
    					+ ": " +  SensorCollection.UNGET_MSG;
    	else
    		currentSensorInfo[SensorCollection.NUM_SENSORS] =  
    				sensorNames[SensorCollection.NUM_SENSORS]
    				+ ": " + info.getTime();
    	sensorInfoAdapter.notifyDataSetChanged();
    }

    /**
     * Called when user want to collect and send sensor info once.
     * @param view
     */
    public void collectAndSend(View view) {
    	if (!mBound) {
    		showMyDialog(resources.getString(
    				R.string.dialog_service_not_bound), false);
    		return;
    	}
    	//Wait for previous task
    	if (clickedReadAndSend) {
    		showMyDialog(resources.getString(
    				R.string.dialog_already_clicked), false);
    		return;
    	}
    	Toast.makeText(SensorInfoShowActivity.this, 
				resources.getString(R.string.toast_fetch_sensor_info)
				, Toast.LENGTH_SHORT).show();
    	clickedReadAndSend = true;
    	mService.collectAndSendOnce();
    }
    
    /**
     * helper for show a dialog with message msg
     * @param msg the message want to show
     */
    private void showMyDialog(String msg, final boolean goSetting)
    {
        AlertDialog.Builder normalDia = new AlertDialog.Builder(this);
        normalDia.setIcon(R.drawable.ic_launcher);
        normalDia.setMessage(msg);
        
        normalDia.setPositiveButton(
        		resources.getString(R.string.dialog_confirm),
        		new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	if (!goSetting)
            		dialog.cancel();
            	else {
            		Intent intent = new Intent(
            				SensorInfoShowActivity.this, Setting.class);
            		intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            		startActivity(intent);
            	}
            }
        });
        
        normalDia.create().show();
    }
    
    /**
     * receiver for new sensor info comes
     * @author piyifan
     *
     */
    private class NewSensorInfoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("pyf", "in receiver");
			//Show the Toast
			Toast.makeText(SensorInfoShowActivity.this, 
					resources.getString(R.string.toast_new_sensor_info)
					, Toast.LENGTH_SHORT).show();
			//Call the backgroud service for update
			updateSensorInfo(mService.getLastSensorInfo());
		}
		
    }
    
    private class SendSensorReceiver extends BroadcastReceiver {
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		SensorInfoPack info = (SensorInfoPack) intent.
    					getSerializableExtra(
    							SensorService.IDENT_CONTENT_INFO);
    		if (info ==  null || 
    				info.getCallType() == SensorInfoPack.BACK_CALL)
    			return;
    		clickedReadAndSend = false;
    		int result = intent.getIntExtra(
    						SensorService.IDENT_CONTENT_RESULT,
    						SensorService.SEND_RESULT_FAIL_NETWORK);
    		String msg;
    		switch (result) {
    			case SensorService.SEND_RESULT_SUCCESS: 
    				msg = resources.getString(R.string.dialog_send_result_success);
    				break;
    			case SensorService.SEND_RESULT_FAIL_NETWORK: 
    				msg = resources.getString(R.string.dialog_send_result_network);
    				break;
    			case SensorService.SEND_RESULT_FAIL_NO_USER: 
    				msg = resources.getString(R.string.dialog_send_result_no_user);
    				break;
    			default: return;
    		}
    		msg = resources.getString(R.string.dialog_send_result_info) 
    				+ info.getTime()
    				+ " : " 
    				+ msg;
    		if (result != SensorService.SEND_RESULT_SUCCESS)
    			showMyDialog(msg, true);
    		else
    			showMyDialog(msg, false);
    	}
    	
    }
}
