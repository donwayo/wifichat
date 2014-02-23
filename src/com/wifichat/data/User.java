package com.wifichat.data;

public class User {
	public String username;
	public static User sharedInstance;
	
	public User(String username) {
		this.username = username;
		sharedInstance = this;
	}
}
