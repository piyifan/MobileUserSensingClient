package com.example.mobileusersensingclient;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class Utils {
	
	private static String convertToHex(byte[] data) { 
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < data.length; i++) { 
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do { 
				if ((0 <= halfbyte) && (halfbyte <= 9)) 
					buf.append((char) ('0' + halfbyte));
				else 
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while(two_halfs++ < 1);
		} 
		return buf.toString();
	}

	/**
	 * SHA-1 encryption algorithm
	 * @param text - the text want to encrypt
	 * @return The SHA-1 value of text
	 */
	public static String SHA1(String text) { 
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			md.update(text.getBytes("UTF-8"), 0, text.length());
		} catch (NoSuchAlgorithmException e) {
			Log.e("pyf", "No SHA-1 algorithm!");
			return text;
		} catch (UnsupportedEncodingException e) {
			Log.e("pyf", "Unsupported UTF-8 when SHA-1 " + text);
			return text;
		}        
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}
	
}
