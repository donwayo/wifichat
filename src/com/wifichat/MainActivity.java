package com.wifichat;

import java.util.ArrayList;
import java.util.List;

import com.wifichat.connection.Callback;
import com.wifichat.connection.ChatConnection;
import com.wifichat.connection.NsdHelper;
import com.wifichat.data.User;
import com.wifichat.screens.adapters.PeopleAdapter;
import com.wifichat.utils.Utils;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final String PREFS_NAME = "Preferences";
	public static final String PREFS_USER_NAME = "username";
	
	private static final String TAG = "MainActivity";
	private MainActivity mActivity;
	private PeopleAdapter mPeopleAdapter;
	private ListView mPeopleListView;
	private TextView mNoContactText;
	private NsdHelper mNsdHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mActivity = this;
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		String username = settings.getString(PREFS_USER_NAME, null);
		// ask for username if not available
		if (username == null) {
			askUsername();
		// otherwise, initialize everything
		} else {
			new User(username);
			initialize();
		}
	}

	/**
	 * Ask for username
	 */
	private void askUsername() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.usernamePromptTitle));

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setSingleLine();
		alert.setView(input);

		alert.setPositiveButton(R.string.usernamePromptBtn,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String username = input.getText().toString();
					
					// ask again if the username is emty
					if (Utils.isEmty(username)) {
						askUsername();
						return;
					}
					
					// create a new user with the given username
					new User(username);
					
					// save username
					SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				    SharedPreferences.Editor editor = settings.edit();
				    editor.putString(PREFS_USER_NAME, username);
			        editor.commit();
			        
					// initialize everything
					initialize();
				}
			});

		alert.show();
	}
	
	private void initialize() {
		// no one available text
		mNoContactText = (TextView) findViewById(R.id.noContactText);
		
		// init people list view
		mPeopleListView = (ListView) findViewById(R.id.peopleList);
		List<NsdServiceInfo> peopleList = new ArrayList<NsdServiceInfo>();
		mPeopleAdapter = new PeopleAdapter(this, peopleList);
		mPeopleListView.setAdapter(mPeopleAdapter);
		
		// init nsd helper
		mNsdHelper = new NsdHelper(mActivity, User.sharedInstance.username,
			new Callback() {
	            public void execute(Object... data) {
	                updatePeopleList();
	            }
		});
        mNsdHelper.initializeNsd();
        
        advertiseService();
        mNsdHelper.discoverServices();
        
        // init refresh button
        Button refreshButton = (Button)findViewById(R.id.refreshbutton);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mNsdHelper.discoverServices();
			}
		});
	}
	
	private void updatePeopleList() {
		Log.d(TAG, "Updating people list");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				List<NsdServiceInfo> serviceList = mNsdHelper.getServices();
				if (serviceList == null) {
					serviceList = new ArrayList<NsdServiceInfo>();
				}
				
				if (serviceList.size() == 0) {
					mNoContactText.setVisibility(View.VISIBLE);
					mPeopleListView.setVisibility(View.GONE);
				} else {
					mNoContactText.setVisibility(View.GONE);
					mPeopleListView.setVisibility(View.VISIBLE);
				}
				
				mPeopleAdapter.clear();
				mPeopleAdapter.addAll(serviceList);
				mPeopleAdapter.notifyDataSetChanged();
			}
		});
	}
	
	/**
	 * Register service
	 */
	public void advertiseService() {
        mNsdHelper.registerService(ChatConnection.PORT);
    }
	
	@Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }
    
    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        //mConnection.tearDown();
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
