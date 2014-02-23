package com.wifichat.screens.adapters;

import java.util.List;

import com.wifichat.R;
import com.wifichat.data.ChatMessage;
import com.wifichat.data.User;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class MessageAdapter extends ArrayAdapter<ChatMessage>{
	private static final int LAYOUT_ID = R.layout.chat_row;
	private static final int MARGIN = 50;
	
	private LayoutInflater mLayoutInflater;
	private Context mContext;
	private int mMargin; // margin in pixels

	public MessageAdapter(Context context, List<ChatMessage> messageList) {
		super(context, LAYOUT_ID, messageList);
		mContext = context;
		mLayoutInflater = LayoutInflater.from(mContext);
		
		float d = context.getResources().getDisplayMetrics().density;
		mMargin = (int)(MARGIN * d); 
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(LAYOUT_ID, null);
		}
		TextView contentView = (TextView) convertView.findViewById(R.id.messageContent);
		contentView.setText(this.getItem(position).getContent());
		
		LinearLayout.LayoutParams p = (LayoutParams) contentView.getLayoutParams();
		if (this.getItem(position).getAuthor().equals(User.sharedInstance.username)) {
			p.setMargins(mMargin, p.topMargin, 0, p.bottomMargin);
			contentView.setBackgroundResource(R.drawable.my_chat_background);
		} else {
			p.setMargins(0, p.topMargin, mMargin, p.bottomMargin);
			contentView.setBackgroundResource(R.drawable.other_chat_background);
		}
		
		return convertView;
	}

}
