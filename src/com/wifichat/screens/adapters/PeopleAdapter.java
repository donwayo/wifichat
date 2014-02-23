package com.wifichat.screens.adapters;

import java.util.List;

import com.wifichat.R;
import com.wifichat.connection.NsdHelper;
import com.wifichat.data.ChatMessage;
import com.wifichat.data.User;
import com.wifichat.screens.ChatScreen;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class PeopleAdapter extends ArrayAdapter<NsdServiceInfo>{
	public static final String ADDRESS = "address";
	public static final String NAME = "name";
	public static final String PORT = "port";
	
	private static final int LAYOUT_ID = R.layout.person_row;
	private static final int MARGIN = 50;
	
	private LayoutInflater mLayoutInflater;
	private Context mContext;
	private int mMargin; // margin in pixels

	public PeopleAdapter(Context context, List<NsdServiceInfo> peopleList) {
		super(context, LAYOUT_ID, peopleList);
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
		final int fPosition = position;
		
		TextView nameView = (TextView) convertView.findViewById(R.id.personName);
		nameView.setText(this.getItem(position).getServiceName()
								.substring(NsdHelper.SERVICE_PREFIX.length()));
		
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mContext, ChatScreen.class);
				intent.putExtra(NAME, getItem(fPosition).getServiceName()
								.substring(NsdHelper.SERVICE_PREFIX.length()));
				intent.putExtra(ADDRESS, getItem(fPosition).getHost().getHostAddress());
				intent.putExtra(PORT, getItem(fPosition).getPort());
				mContext.startActivity(intent);
			}
		});
		return convertView;
	}

}
