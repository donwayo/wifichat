package com.wifichat.data;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
	public static final String CONTENT = "content";
	public static final String AUTHOR = "author";
	
	private String content;
	private String author;
	
	public ChatMessage(String content, String author) {
		this.content = content;
		this.author = author;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}
	
	public String toJSONString() {
		JSONObject o = new JSONObject();
		try {
			o.put(CONTENT, this.content);
			o.put(AUTHOR, this.author);
			return o.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static ChatMessage parse(String jsonString) {
		try {
			JSONObject o = new JSONObject(jsonString);
			return new ChatMessage(o.getString(CONTENT), o.getString(AUTHOR));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
}
