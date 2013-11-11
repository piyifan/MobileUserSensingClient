package com.example.mobileusersensingclient;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.format.Time;

public class DatePickerFragment extends DialogFragment {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
		Time time = new Time();
		time.setToNow();
		time.normalize(true);

		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(getActivity(), 
				(DatePickerDialog.OnDateSetListener) getActivity()
					, time.year, time.month, time.monthDay);
	}
	
}