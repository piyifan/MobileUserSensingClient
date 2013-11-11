package com.example.mobileusersensingclient;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
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
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobileusersensingclient.SensorService.LocalBinder;
import com.example.mobileusersensingclient.TableAdapter.TableCell;
import com.example.mobileusersensingclient.TableAdapter.TableRow;

public class SensorInfoSearchActivity extends Activity 
			implements DatePickerDialog.OnDateSetListener{
	
	
	private static final String STATE_SEARCH_TIME = "sensorsearch.searchTime";
	
	private static final int NUM_COLS = 5;
	
	
	private Time searchTime;
	private ListView commListView;
	private TableAdapter tableAdapter;
	private ArrayList<TableRow> table;
	private TableCell[] titles;
	private Resources resources;
	private boolean inSearch;
	private NewSearchResultReceiver newResReceiver;
	private IntentFilter newResFilter;
	private TextView currentSearchText;
	private TextView currentPickText;
	private Time setTime;
	private String sensorNames[];
	
	/**
	 * Service
	 */
	private SensorService mService;
	private boolean mBound;
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_sensor_info_search, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_sensor_search_home:
	        	intent = new Intent(this, MainActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_search_setting:
	        	intent = new Intent(this, Setting.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_search_sensor_info:
	        	intent = new Intent(this, SensorInfoShowActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_sensor_search_comm_stat:
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
        setContentView(R.layout.activity_sensor_info_search);
        
        //Restore the user search info
        if (savedInstanceState != null) {
        	long tTime = savedInstanceState.
        				getLong(STATE_SEARCH_TIME);
        	if (tTime != 0L) {
        		searchTime = new Time();
        		searchTime.set(tTime);
        	}
        	else searchTime = null;
        }
        else
        	searchTime = null;

        //Get system resources
        resources = this.getResources();
        
        //Set up the textview
        currentSearchText = (TextView) findViewById(R.id.sensorSearchHint);
        currentSearchText.setText(
    			resources.getString(R.string.text_current_search) + 
    			resources.getString(R.string.text_null));
        currentPickText = (TextView) findViewById(R.id.sensorSearchPickText);

        //Get the ListView
        commListView = (ListView) this.
        				findViewById(R.id.commListView);
        
        //Set table content container
        table = new ArrayList<TableRow>();
        
        //Set table column title
        sensorNames = resources.getStringArray(R.array.sensor_names);
        
        titles = new TableCell[SensorCollection.NUM_SENSORS + 1];
        for (int i = 0; i <= SensorCollection.NUM_SENSORS; i++)
        	titles[i] = new TableCell(sensorNames[i],
        							  320,
        							  LayoutParams.MATCH_PARENT,
        							  TableCell.STRING);
        table = new ArrayList<TableRow>();  
        table.add(new TableRow(titles));
        
        //Set the ListView
        tableAdapter = new TableAdapter(this, table);
        commListView.setAdapter(tableAdapter);
        
        //Set inSearch flag
        inSearch = false;
        
        //Register the listener
        newResReceiver = new NewSearchResultReceiver();
        newResFilter = new IntentFilter(SensorService.INTENT_SEARCH_TASK);
        registerReceiver(newResReceiver, newResFilter);
        
        //Put the setTime to now
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        setTime = new Time();
        setTime.set(0, 0, 0, day, month, year);
        currentPickText.setText(
        		resources.getString(R.string.text_current_pick)
        		+ setTime.format(MainActivity.DATE_FORMAT));
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
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            
            sensorSearch(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    @Override
    protected void onPause() {
    	super.onPause();
    	unregisterReceiver(newResReceiver);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
        registerReceiver(newResReceiver, newResFilter);
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's search
    	if (searchTime != null)
    		savedInstanceState.putLong(STATE_SEARCH_TIME
    				, searchTime.toMillis(true));
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    private void refresh() {
    	inSearch = true;
    	Log.i("pyf_search", searchTime.format(MainActivity.DATE_FORMAT));
    	//Start search task
    	mService.startSearchDay(searchTime);
    }
    
    /**
     * when user press the search button
     * @param view
     */
    public void sensorSearch(View view) {
    	if (inSearch || !mBound) {
    		Toast.makeText(this,
    					   resources.getString(R.string.toast_wait),
    					   Toast.LENGTH_SHORT).show();
    		return;
    	}
    	searchTime = new Time();
    	searchTime.set(setTime.toMillis(true));
    	refresh();
    }
    
    private class NewSearchResultReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//Set feedback msg
			int resFlag = intent.getIntExtra(
					SensorService.IDENT_CONTENT_RESULT, 
					SensorService.SEND_RESULT_FAIL_NETWORK);
			String msg = resources.getString(R.string.dialog_search_result_prefix) + 
					searchTime.format(MainActivity.DATE_FORMAT) + ": ";
			switch (resFlag) {
			case SensorService.SEND_RESULT_SUCCESS: 
				msg = msg + resources.getString(
						R.string.dialog_send_result_success);
				break;
			case SensorService.SEND_RESULT_FAIL_NO_USER: 
				msg = msg + resources.getString(
						R.string.dialog_send_result_no_user);
				break;
			default:
			case SensorService.SEND_RESULT_FAIL_NETWORK: 
				msg = msg + resources.getString(
						R.string.dialog_send_result_network);
				break;
			}
			if (resFlag != SensorService.SEND_RESULT_SUCCESS) {
				showMyDialog(msg);
				inSearch = false;
				return;
			}
			
			//Update the text view
			if (searchTime == null)
		        currentSearchText.setText(
		        			resources.getString(R.string.text_current_search) + 
		        			resources.getString(R.string.text_all));
			else
				currentSearchText.setText(
	        			resources.getString(R.string.text_current_search) + 
	        			searchTime.format(MainActivity.DATE_FORMAT));
			
			//Update the table view
			table.clear();
			table.add(new TableRow(titles));
			ArrayList<SensorInfoPack> searchInfo = mService.getLastSearch();
			for (SensorInfoPack infoPack : searchInfo) {
				TableCell[] cells = new TableCell[NUM_COLS];
				for (int i = 0; i < SensorCollection.NUM_SENSORS; i++)
					cells[i] = new TableCell(infoPack.getInfo(i),
											 titles[i].width,
											 LayoutParams.MATCH_PARENT,
											 TableCell.STRING);
				cells[SensorCollection.NUM_SENSORS]  = 
						      new TableCell(infoPack.getTime(),
						    		  	    titles[SensorCollection.NUM_SENSORS].width,
						    		  	    LayoutParams.MATCH_PARENT,
						    		  	    TableCell.STRING);
				table.add(new TableRow(cells));
			}
			tableAdapter.notifyDataSetChanged();
			
			//Show the Toast
			Toast.makeText(context, 
					msg, Toast.LENGTH_SHORT).show();
			
			//Update the inSearch flag
			inSearch = false;
		}
		
    }
    
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

	@Override
	public void onDateSet(DatePicker view, int year, int month,
			int day) {
		setTime.set(0, 0, 0, day, month, year);
		setTime.normalize(true);
		currentPickText.setText(resources.getString(R.string.text_current_pick)
					+ setTime.format(MainActivity.DATE_FORMAT));	
	}
   
	
    /**
     * helper for show a dialog with message msg
     * @param msg the message want to show
     */
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
