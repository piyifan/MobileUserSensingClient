package com.example.mobileusersensingclient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
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

import com.example.mobileusersensingclient.CommStatModel.CommRecord;
import com.example.mobileusersensingclient.TableAdapter.TableCell;
import com.example.mobileusersensingclient.TableAdapter.TableRow;

public class CommStatActivity extends Activity
           implements DatePickerDialog.OnDateSetListener{
	
	private static final int NUM_COLS = 3;
	
	private static final String STATE_SEARCH_TIME = "searchTime";
	
	private Time searchTime;
	private ListView commListView;
	private TableAdapter tableAdapter;
	private ArrayList<TableRow> table;
	private TableCell[] titles;
	private Resources resources;
	private boolean inSearch;
	private CommStatModel model;
	private NewSearchResultReceiver newResReceiver;
	private IntentFilter newResFilter;
	private TextView currentSearchText;
	private TextView currentPickText;
	private Time setTime;
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_comm_stat, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_comm_stat_home:
	        	intent = new Intent(this, MainActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_comm_stat_sensor_info:
	        	intent = new Intent(this, SensorInfoShowActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_comm_stat_sensor_search:
	        	intent = new Intent(this, SensorInfoSearchActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_comm_stat_setting:
	        	intent = new Intent(this, Setting.class);
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
        setContentView(R.layout.activity_comm_stat);
        
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
        currentSearchText = (TextView) findViewById(R.id.commStatHint);
        currentSearchText.setText(
    			resources.getString(R.string.text_current_search) + 
    			resources.getString(R.string.text_null));
        currentPickText = (TextView) findViewById(R.id.commStatPickText);

        //Get the ListView
        commListView = (ListView) this.
        				findViewById(R.id.commListView);
        
        //Set table content container
        table = new ArrayList<TableRow>();
        
        //Set table column title
        titles = new TableCell[NUM_COLS];
        titles[0] = new TableCell(
        		resources.getString(R.string.table_column_phonenumber),
        		320,
        		LayoutParams.MATCH_PARENT,
        		TableCell.STRING
        		);
        titles[1] = new TableCell(
        		resources.getString(R.string.table_column_sms),
        		100,
        		LayoutParams.MATCH_PARENT,
        		TableCell.STRING
        		);
        titles[2] = new TableCell(
        		resources.getString(R.string.table_column_call),
        		100,
        		LayoutParams.MATCH_PARENT,
        		TableCell.STRING
        		);
        table = new ArrayList<TableRow>();  
        table.add(new TableRow(titles));
        
        //Set the ListView
        tableAdapter = new TableAdapter(this, table);
        commListView.setAdapter(tableAdapter);
        
        //Set the model
        model = CommStatModel.getInstance(this);
        
        //Set inSearch flag
        inSearch = false;
        
        //Register the listener
        newResReceiver = new NewSearchResultReceiver();
        newResFilter = new IntentFilter(CommStatModel.INTENT_COMM_RESULT);
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
        
        //Refresh the view
        refresh();
    }
    
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
    	
    	//Search calls and SMS record from provider
    	model.startSearcn(searchTime);
    }
    
    /**
     * when user press the search button
     * @param view
     */
    public void searchCommStat(View view) {
    	if (inSearch) {
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
			HashMap<String, CommRecord> commStat = model.getLastResult();
			Log.i("pyf", "size of "+commStat.size());
			Iterator<Entry<String, CommRecord>> iter = commStat.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, CommRecord> entry = iter.next();
				TableCell[] cells = new TableCell[NUM_COLS];
				cells[0] = new TableCell(entry.getKey(),
										titles[0].width,
										LayoutParams.MATCH_PARENT,
										TableCell.STRING);
				cells[1] = new TableCell(String.valueOf(entry.getValue().getSms()),
										 titles[1].width,
										 LayoutParams.MATCH_PARENT,
										 TableCell.STRING);
				cells[2] = new TableCell(String.valueOf(entry.getValue().getCall()),
										 titles[2].width,
										 LayoutParams.MATCH_PARENT,
										 TableCell.STRING);
				table.add(new TableRow(cells));
			}
			tableAdapter.notifyDataSetChanged();
			
			//Show the Toast
			Toast.makeText(context, 
					resources.getString(R.string.toast_new_comm_stat)
					, Toast.LENGTH_SHORT).show();
			
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
   
}
