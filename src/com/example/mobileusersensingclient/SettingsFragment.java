package com.example.mobileusersensingclient;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
			implements OnSharedPreferenceChangeListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();

		//Set summary
		SharedPreferences sharedPref = 
				getPreferenceManager().getSharedPreferences();
		findPreference(Setting.KEY_PREF_USERNAME).setSummary(
				sharedPref.getString(Setting.KEY_PREF_USERNAME, ""));
		findPreference(Setting.KEY_PREF_SERVER_ADDRESS).setSummary(
				sharedPref.getString(Setting.KEY_PREF_SERVER_ADDRESS, ""));
		int selectedFreq = Integer.valueOf(
				sharedPref.getString(Setting.KEY_PREF_FREQUENCY, "0"));
		findPreference(Setting.KEY_PREF_FREQUENCY).setSummary(
				this.getResources().
				getStringArray(R.array.pref_frequency_entries)[selectedFreq]);

		//Register the listener
		getPreferenceScreen().getSharedPreferences().
		registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		//Unregister the listener
		getPreferenceScreen().getSharedPreferences().
		unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
		//Update Summary
		if (key.equals(Setting.KEY_PREF_USERNAME))
			findPreference(Setting.KEY_PREF_USERNAME).setSummary(
					sharedPref.getString(Setting.KEY_PREF_USERNAME, ""));
		if (key.equals(Setting.KEY_PREF_SERVER_ADDRESS))
			findPreference(Setting.KEY_PREF_SERVER_ADDRESS).setSummary(
					sharedPref.getString(Setting.KEY_PREF_SERVER_ADDRESS, ""));
		if (key.equals(Setting.KEY_PREF_FREQUENCY))  {
			int selectedFreq = Integer.valueOf(
					sharedPref.getString(Setting.KEY_PREF_FREQUENCY, "0"));
			findPreference(Setting.KEY_PREF_FREQUENCY).setSummary(
					this.getResources().
					getStringArray(R.array.pref_frequency_entries)[selectedFreq]);
		}

	}
}