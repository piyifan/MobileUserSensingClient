package com.example.mobileusersensingclient;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;

import com.example.mobileusersensingclient.SensorService.LocalBinder;

public class MainActivity extends Activity {
	
	public static final String DATE_FORMAT = "%Y/%m/%d";
	public static final String TIME_FORMAT = "%Y/%m/%d/%H:%M:%S";
	
	public static final int TIMEOUT_CONNECT = 3000;
	public static final int  TIMEOUT_SOCKET = 5000;
	
	public static final boolean ASYNCTASK_PARALLEL = true;
	
	SensorService mService;
    boolean mBound = false;
    Resources resources;
	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.menu_main_sensor_info:
	        	goSensorInfoShow();
	        	return true;
	        case R.id.menu_main_comm_stat:
	        	goCommStat();
	        	return true;
	        case R.id.menu_main_sensor_search:
	        	goSearch();
	        	return true;
	        case R.id.menu_main_setting:
	        	goSetting();
	        	return true;
	        case R.id.menu_main_bg:
	        	goBackground();
	        	return true;
	        default: 
	        	return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        // Bind to Service
        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	};

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //Set the default value of the preference
        PreferenceManager.setDefaultValues(
        		this, R.xml.preferences, false);
        
        //Get resources
        resources = this.getResources();
        
        //Set grid view
        GridView gridview = (GridView) findViewById(R.id.main_gridview);
        ArrayList<HashMap<String, Object>> lstImageItem = 
        		new ArrayList<HashMap<String, Object>>();  
        
        HashMap<String, Object> map;
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_sensor_info);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_sensor));
        lstImageItem.add(map); 
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_search);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_search));
        lstImageItem.add(map);
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_comm_stat);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_comm));
        lstImageItem.add(map);
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_setting);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_setting));
        lstImageItem.add(map);
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_bg);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_bg));
        lstImageItem.add(map);
        
        map = new HashMap<String, Object>();
        map.put("ItemImage", R.drawable.ic_action_about);
        map.put("ItemText", resources.getString(
        		R.string.grid_text_about));
        lstImageItem.add(map);
        
        SimpleAdapter saImageItems = new SimpleAdapter(this, 
                                                  lstImageItem, 
                                                  R.layout.grid_item,         
                                                  new String[] {"ItemImage","ItemText"}, 
                                                  new int[] {R.id.ItemImage,R.id.ItemText});  
        gridview.setAdapter(saImageItems);    
        gridview.setOnItemClickListener(new ItemClickListener());
    }
    
    private class ItemClickListener implements OnItemClickListener  {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos,
				long row) {
			HashMap<String, Object> item=(HashMap<String, Object>)
					adapter.getItemAtPosition(pos);
			String title = (String) item.get("ItemText");
			if (title.equalsIgnoreCase(resources.getString(
					R.string.grid_text_sensor)))
				goSensorInfoShow();
			else
			if (title.equalsIgnoreCase(resources.getString(
	        		R.string.grid_text_bg)))
					goBackground();
			if (title.equalsIgnoreCase(resources.getString(
	        		R.string.grid_text_comm)))
					goCommStat();
			else
			if (title.equalsIgnoreCase(resources.getString(
			        R.string.grid_text_search)))
					goSearch();
			else
			if (title.equalsIgnoreCase(resources.getString(
			        R.string.grid_text_about)))
					goAbout();
			else
			if (title.equalsIgnoreCase(resources.getString(
				    R.string.grid_text_setting)))
					goSetting();
		}
    	
    }
    
    private void goSensorInfoShow() {
    	// Do something in response to button
    	Intent intent = new Intent(this, SensorInfoShowActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    	startActivity(intent);
    }
    
   	private void goCommStat() {
    	Intent intent = new Intent(this, CommStatActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    	startActivity(intent);
    }
    
   	private void goSetting() {
    	Intent intent = new Intent(this, Setting.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    	startActivity(intent);
    }
    
   	private void goSearch() {
    	Intent intent = new Intent(this, SensorInfoSearchActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    	startActivity(intent);
    }
    
   	private void goBackground() {
    	Intent i = new Intent(Intent.ACTION_MAIN);
    	i.addCategory(Intent.CATEGORY_HOME);
    	startActivity(i);
    }
   	
   	private void goAbout() {
   		showMyDialog(resources.getString(R.string.title_activity_main) + "\n"
   				    + "Ver 1.0\nBy Yifan Pi\npiyifan@gmail.com");
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    private void showMyDialog(String msg)
    {
        AlertDialog.Builder normalDia = new AlertDialog.Builder(this);
        normalDia.setIcon(R.drawable.ic_launcher);
        normalDia.setMessage(msg);
        
        normalDia.setPositiveButton(
        		resources.getString(R.string.dialog_confirm),
        		new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        normalDia.create().show();
    }

}
