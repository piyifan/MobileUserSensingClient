package com.example.mobileusersensingclient;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Setting extends Activity {
	
	public static final String KEY_PREF_USERNAME = "pref_username";
	public static final String KEY_PREF_PASSWORD = "pref_password";
	public static final String KEY_PREF_FREQUENCY = "pref_frequency";
	public static final String KEY_PREF_SERVER_ADDRESS = "pref_address";
	public static final String KEY_PREF_USERFACE = "pref_userface";
	
	public static final long[] VALUE_FREQUENCY = {60000,
												  600000,
												 1200000,
												 1800000};
	
	private Resources resources;
	private ImageView userFaceView;
	
	private void refreshFaceView() {
		//Get the saved userface
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(this);
        String previouslyEncodedImage = shre.getString(KEY_PREF_USERFACE, "");
        
        if ( !previouslyEncodedImage.equalsIgnoreCase("") ){
            byte[] b = Base64.decode(previouslyEncodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            userFaceView.setImageBitmap(bitmap);
        }
        else
        	userFaceView.setImageDrawable(
        			resources.getDrawable(R.drawable.ic_launcher));
        userFaceView.invalidate();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        
        resources = this.getResources();
        
        userFaceView = (ImageView)
        			this.findViewById(R.id.image_user_face);
        refreshFaceView();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_setting, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
	    switch (item.getItemId()) {
	        case R.id.menu_setting_home:
	        	intent = new Intent(this, MainActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_setting_sensor_info:
	        	intent = new Intent(this, SensorInfoShowActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_setting_sensor_search:
	        	intent = new Intent(this, SensorInfoSearchActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_setting_comm_stat:
	        	intent = new Intent(this, CommStatActivity.class);
	        	intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	        	startActivity(intent);
	        	return true;
	        default: 
	        	return super.onOptionsItemSelected(item);
	    }
	}
	

    public void refreshFace(View view) {
    	Log.i("pyf", "refresh face task");
    	if (MainActivity.ASYNCTASK_PARALLEL)
			new RefreshFaceTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, null);
		else
			new RefreshFaceTask().execute();
    }
    
    private static final String GET_FACE_ADDR = ":8080/Web/MobilePicServlet?";
    private static final String GET_IMG_ADDR = ":8080/Web/";
    private static final String ERROR_STR = "error";
    
    private class RefreshFaceTask extends AsyncTask<Void, Void, Boolean> {
    	
    	private boolean userInvalid = false;
    	
    	private Bitmap getFace() {
    		SharedPreferences sharedPref = PreferenceManager.
					getDefaultSharedPreferences(Setting.this);
			String serverAddr = sharedPref.getString(
				Setting.KEY_PREF_SERVER_ADDRESS, "");
			String username = sharedPref.getString(
				Setting.KEY_PREF_USERNAME, "null");
			String password = sharedPref.getString(
				Setting.KEY_PREF_PASSWORD, "");
			String urlString = "http://" + serverAddr + GET_FACE_ADDR +
					  "username=" + username + "&" +
			          "password=" + password;
			Log.i("pyf_face", "Search url: " + urlString);
			URL url;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				Log.i("pyf_face", "invalid uri when fetch uri");
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
				Log.i("pyf_face", "i/o error when fetch uri");
				return null;
			}
		
			if (!in.hasNextLine()) {
				in.close();
				urlConnection.disconnect();
				Log.i("pyf_face", "can not get send feedback" +
						"when fetch uri");
				return null;
			}
			
			String resString = in.nextLine();
			Log.i("pyf_face", "Result String = " + resString);
			in.close();
			urlConnection.disconnect();
			
			if (resString.contains(ERROR_STR)) {
				userInvalid = true;
				return null;
			}
			
			//After get the uri, start to read image
			String imageAddr = "http://" + serverAddr + 
					GET_IMG_ADDR + resString;
			try {
				url = new URL(imageAddr);
			} catch (MalformedURLException e) {
				Log.i("pyf_face", "invalid uri when get image");
				return null;
			}
			Log.i("pyf_face", "image uri: " + imageAddr);
			InputStream is;
			try {
				urlConnection = 
						(HttpURLConnection) url.openConnection();
				urlConnection.setConnectTimeout(
						MainActivity.TIMEOUT_CONNECT);
				urlConnection.setReadTimeout(
						MainActivity.TIMEOUT_SOCKET);
				urlConnection.connect();
				is = urlConnection.getInputStream();
			} catch (IOException e) {
				Log.i("pyf_face", "i/o error when get image");
				return null;
			}
			return BitmapFactory.decodeStream(is);
    	}
    	
		@Override
		protected Boolean doInBackground(Void... params) {
			
			Bitmap newImage = getFace();
			if (newImage == null) return false;
			//Save the newImage
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			newImage.compress(Bitmap.CompressFormat.PNG, 100, baos);   
			byte[] b = baos.toByteArray(); 

			String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

			SharedPreferences shre = PreferenceManager.
					getDefaultSharedPreferences(Setting.this);
			Editor edit=shre.edit();
			edit.putString(KEY_PREF_USERFACE, encodedImage);
			edit.commit();
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean res) {
			if (userInvalid) {
				showMyDialog(resources.getString(
						R.string.dialog_send_result_no_user));
				return;
			}
			if (!res) {
				showMyDialog(resources.getString(
						R.string.dialog_send_result_network));
				return;
			}
			Log.i("pyf", "get image");
			Toast.makeText(Setting.this, 
					resources.getString(R.string.toast_fetch_new_face),
					Toast.LENGTH_SHORT).show();
			refreshFaceView();
		}
    }
    
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
