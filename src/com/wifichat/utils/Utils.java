package com.wifichat.utils;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class Utils {
	public static void hideSoftKeyboard(Activity activity) {
		try {
			InputMethodManager inputMethodManager = (InputMethodManager) activity
					.getSystemService(Activity.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(activity
					.getCurrentFocus().getWindowToken(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Notify a message to the user
	 */
	public static void notifyUser(Context activity, String message) {
		Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Check if a string is empty
	 */
	public static boolean isEmty(String s) {
		if (s == null || s.trim().equals("")) {
			return true;
		}
		return false;
	}
}
