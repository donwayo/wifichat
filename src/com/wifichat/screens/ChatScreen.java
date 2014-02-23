package com.wifichat.screens;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.wifichat.R;
import com.wifichat.R.layout;
import com.wifichat.R.menu;
import com.wifichat.connection.ChatConnection;
import com.wifichat.data.ChatMessage;
import com.wifichat.data.User;
import com.wifichat.screens.adapters.MessageAdapter;
import com.wifichat.screens.adapters.PeopleAdapter;
import com.wifichat.utils.Utils;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ChatScreen extends Activity {
	private static final String TAG = "ChatScreen";

	private EditText mNewMessageView;
	private MessageAdapter mMessageAdapter;
	private ChatConnection mConnection;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_screen);
		
		// get passed data
		Intent intent = getIntent();
		String name = intent.getStringExtra(PeopleAdapter.NAME);
		String address = intent.getStringExtra(PeopleAdapter.ADDRESS);
		int port = intent.getIntExtra(PeopleAdapter.PORT, 0);
		
		// init navigation bar
		getActionBar().setDisplayHomeAsUpEnabled(true);
		this.setTitle(name);
		
		// init chat view
		List<ChatMessage> messageList = new ArrayList<ChatMessage>();
		ListView messageListView = (ListView) findViewById(R.id.messageList);
		mMessageAdapter = new MessageAdapter(this, messageList);
		messageListView.setAdapter(mMessageAdapter);
		
		// init send button's action
		mNewMessageView = (EditText) findViewById(R.id.newMessage);
		Button sendBtn = (Button) findViewById(R.id.sendBtn);
		sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String msg = mNewMessageView.getText().toString().trim();
				if (!Utils.isEmty(msg)) {
					sendMessage(msg);
				}
				mNewMessageView.setText("");
			}
		});

		// init chat connection
		mConnection = new ChatConnection(this);
		try {
			mConnection.connectToServer(InetAddress.getByName(address), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send message
	 */
	public void sendMessage(String content) {
		ChatMessage message = new ChatMessage(content, User.sharedInstance.username);
		mConnection.sendMessage(message);
	}
	
	/**
	 * Update new message
	 */
	public void updateMessage(final ChatMessage message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (message != null) {
					mMessageAdapter.add(message);
				}
			}
		});
	}
	
	@Override
	public void onDestroy() {
		if (mConnection != null) {
			mConnection.tearDown();
		}
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
	        NavUtils.navigateUpFromSameTask(this);
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_screen, menu);
		return true;
	}

}
