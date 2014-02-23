package com.wifichat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.wifichat.connection.Callback;
import com.wifichat.connection.ChatConnection;
import com.wifichat.connection.NsdHelper;
import com.wifichat.data.User;
import com.wifichat.screens.ChatScreen;
import com.wifichat.screens.adapters.PeopleAdapter;
import com.wifichat.utils.Utils;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {
	public static final int REMOVE_TYPE = 1;
	public static final int ADD_TYPE = 2;
	public static final String UPDATE_TYPE = "updateType";
	
	private static final String TAG = "MainActivity";
	private MainActivity mActivity;
	private PeopleAdapter mPeopleAdapter;
	private NsdHelper mNsdHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mActivity = this;
		
		// create a new user with the given username
		//new User("Thanh");
		
		// ask for username
		askUsername();
		
		// initialize();
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
					String value = input.getText().toString();
					
					// ask again if the username is emty
					if (Utils.isEmty(value)) {
						askUsername();
						return;
					}
					
					// create a new user with the given username
					new User(value);
					// initialize everything
					initialize();
				}
			});

		alert.show();
	}
	
	private void initialize() {
		// init people list view
		ListView peopleListView = (ListView) findViewById(R.id.peopleList);
		List<NsdServiceInfo> peopleList = new ArrayList<NsdServiceInfo>();
		mPeopleAdapter = new PeopleAdapter(this, peopleList);
		peopleListView.setAdapter(mPeopleAdapter);
		
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
        /*Intent intent = new Intent(mActivity, ChatScreen.class);
		startActivity(intent);*/
        
        /*ListView peopleListView = (ListView) findViewById(R.id.peopleList);
		List<Person> peopleList = new ArrayList<Person>();
		peopleList.add(new Person("Thanh", "192.168.56.101"));
		peopleList.add(new Person("Test", "192.168.56.102"));
		
		mPeopleAdapter = new PeopleAdapter(this, peopleList);
		peopleListView.setAdapter(mPeopleAdapter);*/
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
        
        /*if(mConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }*/
        
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
