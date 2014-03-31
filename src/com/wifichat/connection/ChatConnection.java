/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wifichat.connection;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.wifichat.R;
import com.wifichat.data.ChatMessage;
import com.wifichat.data.User;
import com.wifichat.screens.ChatScreen;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatConnection {

    private ChatScreen mUpdateHandler;
    private ChatServer mChatServer;
    private ChatClient mChatClient;

    private static final String TAG = "ChatConnection";
    public static final int PORT = 44300;

    private Socket mSocket;
    private int mPort = -1;

    public ChatConnection(ChatScreen handler) {
        mUpdateHandler = handler;
        mChatServer = new ChatServer();
    }

    public void tearDown() {
    	if (mChatServer != null) {
    		mChatServer.tearDown();
    	}
        if (mChatClient != null) {
        	mChatClient.tearDown();
        }
    }

    public void connectToServer(InetAddress address, int port) {
    	Log.d(TAG, "Connecting to server: " + address.toString() + " " + port);
        mChatClient = new ChatClient(address, port);
    }

    public void sendMessage(ChatMessage message) {
        if (mChatClient == null) {
        	return;
        }
        
        String s = message.toJSONString();
        if (s != null) {
        	mChatClient.sendMessage(s);
        	updateMessages(message);
        }
    }
    
    public int getLocalPort() {
        return mPort;
    }
    
    public void setLocalPort(int port) {
        mPort = port;
    }
    

    public synchronized void updateMessages(ChatMessage message) {
    	if (message == null) {
    		return;
    	}
        Log.e(TAG, "Updating message: " + message.getAuthor() + " " + message.getContent());
        mUpdateHandler.updateMessage(message);
    }

    private synchronized void setSocket(Socket socket) {
        Log.d(TAG, "setSocket being called.");
        if (socket == null) {
            Log.d(TAG, "Setting a null socket.");
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    private Socket getSocket() {
        return mSocket;
    }

    private class ChatServer {
    	SSLServerSocket mServerSocket = null;
        Thread mThread = null;

        public ChatServer() {
            mThread = new Thread(new ServerThread());
            mThread.start();
        }

        public void tearDown() {
            mThread.interrupt();
            try {
                mServerSocket.close();
            } catch (Exception ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        class ServerThread implements Runnable {

            @Override
            public void run() {
                try {
                	  String trustStoreType = KeyStore.getDefaultType();
	               	  KeyStore trustStore = KeyStore.getInstance(trustStoreType);
	               	  TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	               	  InputStream trustStoreStream = mUpdateHandler.getResources().openRawResource(R.raw.truststore);
	               	  trustStore.load(trustStoreStream, "123456".toCharArray());
	               	  tmf.init(trustStore);
                	 	  
					  String keyStoreType = KeyStore.getDefaultType();
					  KeyStore keyStore = KeyStore.getInstance(keyStoreType);
					  InputStream keyStoreStream = mUpdateHandler.getResources().openRawResource(R.raw.server);
					  keyStore.load(keyStoreStream, "123456".toCharArray());
					                  
					
					  String keyalg = KeyManagerFactory.getDefaultAlgorithm();
					  KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyalg);
					  kmf.init(keyStore, "123456".toCharArray());
					
					  SSLContext context = SSLContext.getInstance("TLS");
					  context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);          
					  mServerSocket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(PORT);
					  //mServerSocket.setNeedClientAuth(true);
					  mServerSocket.setWantClientAuth(true);
					  mServerSocket.setEnabledCipherSuites(mServerSocket.getSupportedCipherSuites());

                    //mServerSocket = new ServerSocket(PORT);
                    setLocalPort(mServerSocket.getLocalPort());
                    
                    Log.d(TAG, "KeyManagerFactory " + kmf.getKeyManagers().length);
                    Log.d(TAG, "TrustManagerFactory " + tmf.getTrustManagers().length);

                    
                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        setSocket(mServerSocket.accept());
                        Log.d(TAG, "Connected.");
                        
                        int port = mSocket.getPort();
                        InetAddress address = mSocket.getInetAddress();
                        connectToServer(address, port);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            }
        }
    }

    private class ChatClient {

        private InetAddress mAddress;
        private int PORT;

        private final String CLIENT_TAG = "ChatClient";

        private Thread mSendThread;
        private Thread mRecThread;
        
        private BlockingQueue<String> mMessageQueue;
        private int QUEUE_CAPACITY = 10;

        public ChatClient(InetAddress address, int port) {
            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;
            
            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }
        
        class SendingThread implements Runnable {

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    if (getSocket() == null) {
                    	String trustStoreType = KeyStore.getDefaultType();
						KeyStore trustStore = KeyStore.getInstance(trustStoreType);
						TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						InputStream trustStoreStream = mUpdateHandler.getResources().openRawResource(R.raw.truststore);
						trustStore.load(trustStoreStream, "123456".toCharArray());
						tmf.init(trustStore);
						 	  
						String keyStoreType = KeyStore.getDefaultType();
						KeyStore keyStore = KeyStore.getInstance(keyStoreType);
						InputStream keyStoreStream = mUpdateHandler.getResources().openRawResource(R.raw.server);
						keyStore.load(keyStoreStream, "123456".toCharArray());
						                  
						String keyalg = KeyManagerFactory.getDefaultAlgorithm();
						KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyalg);
						kmf.init(keyStore, "123456".toCharArray());
						
						SSLContext sslContext = SSLContext.getInstance("TLS");
						sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
						
                    	SocketFactory sf = sslContext.getSocketFactory();
                    	SSLSocket socket = (SSLSocket) sf.createSocket(mAddress, PORT);
  					  	socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
  					  
  					  	
                    	//setSocket(new Socket(mAddress, PORT));
                        setSocket(socket);
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");
                        Log.d(CLIENT_TAG, "KeyManagerFactory " + kmf.getKeyManagers().length);
                        Log.d(CLIENT_TAG, "TrustManagerFactory " + tmf.getTrustManagers().length);

                    } else {
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                    }

                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                } catch (IOException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                } catch (Exception e) {
                	e.printStackTrace();
                }
                
                try{
	                Socket socket = getSocket();
	                if (socket == null) {
	                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
	                    return;
	                } else if (socket.getOutputStream() == null) {
	                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
	                    return;
	                }
	
	                PrintWriter out = new PrintWriter(
	                        new BufferedWriter(
	                                new OutputStreamWriter(getSocket().getOutputStream())), true);
	                
	                while (!Thread.currentThread().isInterrupted()) {
	                    try {
	                        String msg = mMessageQueue.take();
	                        out.println(msg);
	                        out.flush();
	                        
	                        Thread.sleep(50);
	                    } catch (InterruptedException ie) {
	                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
	                    }
	                }
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            
        }

        class ReceivingThread implements Runnable {

        	@Override
            public void run() {
                BufferedReader input = null;
                try {
                    
                    while (!Thread.currentThread().isInterrupted()) {
                    	if (input == null && mSocket != null) {
                    		input = new BufferedReader(new InputStreamReader(
                                    mSocket.getInputStream()));
                    	}
                    	if (input == null) {
                    		continue;
                    	}
                        String messageStr = null;
                        messageStr = input.readLine();
                        if (messageStr == null) {
                        	break;
                        }
                        Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);
                        JSONObject o = new JSONObject(messageStr);
                        updateMessages(new ChatMessage(o.getString(ChatMessage.CONTENT), 
                        				o.getString(ChatMessage.AUTHOR)));
                    }
                    input.close();
                } catch (Exception e) {
                    Log.e(CLIENT_TAG, "Server loop error: ", e);
                }
            }
        }

        public void tearDown() {
            try {
            	mSendThread.interrupt();
            	mRecThread.interrupt();
            	if (mSocket != null) {
            		mSocket.close();
            	}
            } catch (Exception e) {
                Log.e(CLIENT_TAG, "Error when closing server socket.");
                e.printStackTrace();
            }
        }

        public void sendMessage(String msg) {
            mMessageQueue.add(msg);
        }
    }
}
