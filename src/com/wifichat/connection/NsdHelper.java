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

import java.util.ArrayList;
import java.util.List;

import com.wifichat.MainActivity;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public static final String SERVICE_PREFIX = "WifiChat_";
    public String mServiceName;
    private Callback mServiceFoundHandler;

    private List<NsdServiceInfo> mServices;

    public NsdHelper(Context context, String serviceName, Callback serviceFoundHandler) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mServiceName = SERVICE_PREFIX + serviceName;
        mServiceFoundHandler = serviceFoundHandler;
        
        mServices = new ArrayList<NsdServiceInfo>();
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
        //mNsdManager.init(mContext.getMainLooper(), this);
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                Log.d(TAG, service.getServiceName() + " " + mServiceName);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains(SERVICE_PREFIX)){
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost" + service);
                int index = findService(service);
                if (index != -1) {
                	mServices.remove(index);
                	mServiceFoundHandler.execute();
                }
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);        
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                
                // handle the new service
                Log.d(TAG, "Search: " + findService(serviceInfo));
                if (findService(serviceInfo) == -1) {
                	mServices.add(serviceInfo);
                	mServiceFoundHandler.execute();
            	}
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            	Log.d(TAG, "Service registerd");
                mServiceName = NsdServiceInfo.getServiceName();
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            	Log.d(TAG, "Registration failed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            	Log.d(TAG, "Service unregistered");
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            	Log.d(TAG, "Unregistration failed");
            }
            
        };
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }
    
    public void unregisterService() {
    	mNsdManager.unregisterService(mRegistrationListener);
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }
    
    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public List<NsdServiceInfo> getServices() {
    	return mServices;
    }
       
    public void tearDown() {
    	Log.d(TAG, "Tearing down service");
        unregisterService();
        //stopDiscovery();
    }
    
    private int findService(NsdServiceInfo serviceInfo) {
    	if (mServices != null && serviceInfo != null){
	    	for (int i = 0; i < mServices.size(); i++) {
	    		if (mServices.get(i).toString().equals(serviceInfo.toString())) {
	    			return i;
	    		}
	    	}
    	}
    	return -1; 
    }
}
