package com.arubanetworks.aledemonstrator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.json.JSONArray;

import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class MainActivity extends Activity {
	static String TAG = "MainActivity";
	TextView aleHostTextView;
	TextView scanningEnabledTextView;
	TextView aleUsernameTextView;
	TextView alePasswordTextView;
	static Button selectFloorButton;
	TextView statusText01;
	TextView statusText02;
	static Button pickTargetButton;
	Button trackHistoryButton;
	Button surveyButton;
	Button emailButton;
	static Button surveyConfirmButton;
	FloorPlanView floorPlanView;
	
	static String selectFloorButtonText = "select a floor to track";
	static String pickTargetButtonText = "showing all";
	
	static Context context;

	static ArrayList<AleCampus> campusList;
	static ArrayList<AleBuilding> buildingList;
	static ArrayList<AleFloor> floorList = new ArrayList<AleFloor>();;
	static int floorListIndex = -1;
	
	static boolean zmqEnabled = false;
	private volatile ZMQSubscriber zmqSubscriber;
//	ZMQSubscriber2 zmqSubscriber;
	static Handler zmqHandler;
	// the zmqfilter sets up the pub-sub contract with ale server.  change this to get different feeds
	static String[] zmqFilter = {"location", "presence", "station", "destination", "application", "device_rec",  "geofence"};
	//static String[] zmqFilter = {"location"};
	static String ZMQ_PROGRESS_MESSAGE = "zmqProgress";
	static String zmqStatusString = "ZMQ Status";
	static long zmqMessageCounter = 0;
	static long zmqMessagesForMyMac = 0;
	static long zmqLastSeq = 0;
	static long zmqMissedSeq = 0;
	
	static String aleHost = "";
	//static String alePort = "8080";
	static String alePort = "8443";
	//static String floorplanDownloadPort = "80";
	static String floorplanDownloadPort = "8443";
	static float site_xAle = 0;
	static float site_yAle = 0;
	static Bitmap floorPlan;
	static String aleUsername = "";
	static String alePassword = "";
	static String httpStatusString1 = "http Status";
	static String httpStatusString2 = "";
	static CookieManager cookieManager = new CookieManager();
	static long cookieExpires = 0;
    
    SensorManager sensorMan;
    float[] valuesAccelerometer = new float[3];
    float[] valuesMagneticField = new float[3];
    static float compassDegrees = 0;
    
    BluetoothBeaconScanner bluetoothBeaconScanner;
	public BluetoothAdapter.LeScanCallback bluetoothScanCallback;
    static boolean bluetoothEnabled = false;
	BluetoothManager bluetoothManager;
	static BluetoothAdapter bluetoothAdapter;
	static final int REQUEST_ENABLE_BT = 1;
	static String bleBeaconResults = " ";
	static JSONArray iBeaconJsonArray = null;
    static int iBeaconsTotal = 0;
    static int iBeaconsUnique = 0;
	
	GetAleDiscoveryAsyncTask getAleDiscoveryAsyncTask;
	static boolean aleDiscoveryAsyncTaskInProgress = false;	
	static boolean getFloorplanAsyncTaskInProgress = false;	
	static boolean findMyLocationAsyncTaskInProgress = false;
	static boolean getFingerprintMapAsyncTaskInProgress = false;
	static boolean sendChunkedDataAsyncTaskInProgress = false;
	static boolean getCookieAsyncTaskInProgress = false;
	
	Handler handler = new Handler();
	int counter = 0;
	static int DELAY = 2000;
	
	static String myMac = "";  // myMac is derived from the Wi-Fi interface and is only used as a lookup key in JSON location requests
	static String myHashMac = null;  // myHashMac is discovered from JSON location requests and used for all tests that match my mac (because of anonymization)
	static String myFloorId = null;
	static String showingFloorId = null;
	static String latestFloorId = null;
	static boolean moveAcrossFloors = false;
	static int myFloorIndex = -1;
	
	static String targetMac = null;  // this is only used for the value on the pick target button.  It is not always present in JSON or protobuf messages.
	static String targetHashMac = null;  // always using targetHashMap whether or not it's encrypted.  If null indicates not tracking target.
	
	static boolean showHistory = false;
	static boolean showAllMacs = true;
	
	static boolean waitingToTouchTarget = false;
	static String touchTargetHashMac = null;
	
	// this hash map uses hashed mac as key and a list of position history objects as value, for targets matching this floor id
    static HashMap<String, ArrayList<PositionHistoryObject>> aleAllPositionHistoryMap = new HashMap<String, ArrayList<PositionHistoryObject>>(500);
	// this is a list of position history objects for a single target, my hashmac or a target hashmac
    static ArrayList<PositionHistoryObject> alePositionHistoryList = new ArrayList<PositionHistoryObject>();
    // this hash map uses hashed mac as key and a list of all events received for that target as value.  location, station, presence...
	static HashMap<String, ArrayList<String>> eventLogMap = new HashMap<String, ArrayList<String>>(500);
	// this is a list of position history objects for a single target, holding site survey results
	static ArrayList<PositionHistoryObject> surveyHistoryList = new ArrayList<PositionHistoryObject>();
	// this is a list of verify objects, holding verify survey results
	static ArrayList<VerifyObject> verifyHistoryList = new ArrayList<VerifyObject>();
	// this is a list of position history objects from http lookups, used for verification if zmqEnabled is false
	static ArrayList<PositionHistoryObject> aleHttpPositionHistoryList = new ArrayList<PositionHistoryObject>();
	
	static Animation animAlpha;

	WifiManager wifiManager;
	static boolean scanningEnabled = false;
	
	static float surveyPointX = 0;
	static float surveyPointY = 0;
	
	static final int MODE_TRACK = 0;
	static final int MODE_SURVEY = 1;
	static final int MODE_VERIFY = 2;
	static int trackMode = MODE_TRACK;

	static boolean postFingerprintAsyncTaskInProgress = false;
	
	String csvEmailAddress = "";
	
	static CustomVerifier customVerifier = new CustomVerifier();
	static boolean checkCertBool = false;
	static boolean blacklistCertBool = false;
	static String certData = "empty";
	static String trustedCert = "X";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_main);
		setViewsAndListeners();
		readSharedPreferences();
		
		animAlpha = AnimationUtils.loadAnimation(this,  R.anim.anim_alpha);
		
		initialiseZmqAndOthers();   	
		
		bluetoothEnabled = initialiseBluetooth();
	
		setViewText();
		
	}
    
  
    Runnable runnable = new Runnable(){
    	public void run(){    		
    		
    		initialiseConfigViews();
    		statusText01.setText("HTTP STATUS\n"+httpStatusString1+"\n"+httpStatusString2);
    		statusText02.setText("ZMQ STATUS\n"+zmqStatusString);
    		if(counter%120 == 2){ printCookies(); }
    		
    		if(counter%3 == 0 && validCookie() == false && getCookieAsyncTaskInProgress == false){
	    		GetCookieAsyncTask getCookieAsyncTask = new GetCookieAsyncTask();
	    		getCookieAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	    		Log.v(TAG, "cookie count was zero, started new login");
    		}
    		
    		if(blacklistCertBool == true || !certData.equals(trustedCert)) {
    			if(blacklistCertBool == true) {
    				Log.w(TAG, "cert is blacklisted "+certData);
    				httpStatusString1 = "you rejected the server certificate";
    				httpStatusString2 = "reconfigure for a different address";
    			}
    			else if ( !certData.equals(trustedCert) && !certData.equals("empty") && MainActivity.checkCertBool == false ) {
    				MainActivity.checkCertBool = true;
    				showTrustDialog();
    			}
    		} 

    		else {
    		
	    		// check whether the target has changed floors and load the new floorplan
	    		if(moveAcrossFloors == true && floorList != null && latestFloorId != null && floorList.size() > 1 && latestFloorId.equals(showingFloorId)){
	    			for(int i=0; i<floorList.size(); i++){
	    				if(floorList.get(i).floor_id.equals(latestFloorId)) {
	         				Toast toast = Toast.makeText(context, ("target has moved to floor "+floorList.get(i).floor_name), Toast.LENGTH_LONG);
	        				toast.show();
	        				
	        				floorListIndex = i;
				    		showingFloorId = floorList.get(i).floor_id;
				    		// download the floorplan
				    		if(floorPlan != null) { floorPlan = null; }
				    		floorPlanView.initialize();
				    		Log.v(TAG, "url for floorplan "+"/api"+floorList.get(i).floor_img_path);
				    		GetFloorplanAsyncTask getFloorplanAsyncTask = new GetFloorplanAsyncTask();
				    		getFloorplanAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "/api"+floorList.get(i).floor_img_path);
	        				
	    				}
	    			}
	    		}
	    		
	            if(counter%10 == 2 && validCookie() && (floorList == null || floorList.size() < 1) && aleDiscoveryAsyncTaskInProgress == false) {
	            	GetAleDiscoveryAsyncTask getAleDiscoveryAsyncTask = new GetAleDiscoveryAsyncTask();
	            	getAleDiscoveryAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	            }
	           
		        if(counter%30 == 3 && validCookie() && myHashMac == null && findMyLocationAsyncTaskInProgress == false) {
		        	GetMyLocationAsyncTask getMyLocationAsyncTask = new GetMyLocationAsyncTask();
		        	String[] params = {myMac, "true"};
		        	//findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
		        	getMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		        }
		        
		        // added to allow http updates for verification if zmq is disabled
		        if(zmqEnabled == false && trackMode == MODE_VERIFY && counter%3 == 2 && findMyLocationAsyncTaskInProgress == false) {
		        	GetMyLocationAsyncTask getMyLocationAsyncTask = new GetMyLocationAsyncTask();
		        	String[] params = {myMac, "true"};
		        	//findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
		        	getMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		        	Log.w(TAG, "http updates getmylocation ");
		        }
	    		
	            floorPlanView.invalidate();
		
	    		if(counter%7 == 2 && zmqSubscriber == null){
	    			zmqMessageCounter = 0;
	    			zmqMessagesForMyMac = 0;
	    			zmqLastSeq = 0;
	    			zmqMissedSeq = 0;
	    			// added if statement so we keep history if zmq is disabled
	    			if(zmqEnabled == true) {
	    				alePositionHistoryList = new ArrayList<PositionHistoryObject>();
	    			}
	    			eventLogMap = new HashMap<String, ArrayList<String>>(500);
	
	    			if(zmqEnabled) {
		        		try{
		        			zmqSubscriber = new ZMQSubscriber(zmqHandler, zmqFilter);
		//        			zmqSubscriber = new ZMQSubscriber2(zmqHandler, zmqFilter);
		        			zmqSubscriber.start();
		        			Log.v(TAG, "zmqSubscriber was null, restarting with host "+aleHost);
		        		} catch (Exception e) { Log.e(TAG, "Exception starting new thread for zmqSubscriber "+e); }
	    			}
	    		} 
	        	
	    		if(zmqMessageCounter > 0) { 
	    			zmqStatusString = +zmqMessageCounter+" messages, "+zmqMessagesForMyMac+" for my MAC "; 
	    		}
	    		
	    		if(counter%5 == 1 && scanningEnabled && wifiManager != null){
	    			wifiManager.startScan();
	    			Log.d(TAG, "start scan");
	    		}
	    		
	    		if(counter%5 == 3 && trackMode != MODE_TRACK){
	    			getFingerprintMap();
	    		}
	    		
	    		// this logs a periodic count of the ale all position history map to show how many devices are tracked and how many points each
	/*    		if(counter%77 == 7) {
	    			int count = 0;
	    			ArrayList<String> floorList = new ArrayList<String>();
	    			for(Map.Entry<String, ArrayList<PositionHistoryObject>> entry : aleAllPositionHistoryMap.entrySet()){
	    				count++;
	    				for(int i=0; i<entry.getValue().size(); i++){
	    					String floorId = entry.getValue().get(i).floorId;
	    					boolean match = false;
	    					for(int j=0; j<floorList.size(); j++){
	    						if(floorId.equals(floorList.get(j))) { 
	    							match = true; 
	    						}	
	    					}
	    					if(match == false) floorList.add(floorId);
	    				}
						Log.v(TAG, count+"  of  "+MainActivity.aleAllPositionHistoryMap.size()+"  map list size "+entry.getValue().size()+"  floors "+floorList.size()+"  floor "+floorList.get(0));
	    				floorList.clear();
	    			}
	    		}
	*/   		
	    		counter++;
    		}
    		
        	handler.postDelayed(this, DELAY);
    	}
    };
    
    private void zmqMessageReceived(Message msg){
    	if(msg.getData().containsKey(ZMQ_PROGRESS_MESSAGE)) {
    		//Log.v(TAG, "new ZMQ progress message");
    		try{
    			String progress = new String(msg.getData().getByteArray(ZMQ_PROGRESS_MESSAGE), "UTF-8");
    			if(progress.contains("Closed")) {
    				progress = "socket closed";
    			}
    			zmqStatusString = progress;
    			//Log.v(TAG, "the zmq message was a progress message... "+progress);
    		} catch (Exception e) { Log.e(TAG, "Exception reading progress message content as string "+e); }
    	} else  if (msg.getData() != null){
    		zmqMessageCounter++;
    		Set<String> keySet = msg.getData().keySet();
    		for(String s : keySet) {
    			//Log.v(TAG, "new message with key "+s);
    			byte[] messageBody = msg.getData().getByteArray(s);
    			if(zmqMessageCounter%500 == 1) Log.v(TAG, "zmq message count "+zmqMessageCounter+
    					" last message length "+messageBody.length+"  tracking "+aleAllPositionHistoryMap.size()+" devices");
    			ProtobufParsers.parseAleMessage(messageBody);
    		}
    	}
    }
    
    private void getFingerprintMap(){
    	if(getFingerprintMapAsyncTaskInProgress == false  && floorList != null && floorListIndex != -1 && floorList.size() >= floorListIndex){
    		String[] floorId = {floorList.get(floorListIndex).floor_id};
    		GetFingerprintMapAsyncTask getFingerprintMapAsyncTask = new GetFingerprintMapAsyncTask();
    		getFingerprintMapAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, floorId);
    	}
    }

    
    private OnClickListener buttonFloorListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    		if(v == selectFloorButton){
    			AlertDialog.Builder builder = new AlertDialog.Builder(context);
    			builder.setTitle("Pick a Floor");
    			builder.setItems(floorListing(), new DialogInterface.OnClickListener() {
    			    @Override
    			    public void onClick(DialogInterface dialog, int i) {
    			    	if(floorList != null && i == 0){
    			    		if(myFloorIndex != -1){ i = myFloorIndex; }
    			    		else { 
    			    			selectFloorButtonText = "can't find the floor yet\nplease try again";
    			    			selectFloorButton.setText(selectFloorButtonText); 
    			    			floorListIndex = -1;
    			    			if(floorPlan != null) { floorPlan = null; }
    			    			floorPlanView.initialize();
    			    			//targetHashMac = myMac;  // sets target to my MAC in case http lookup doesn't work
    			    			Log.w(TAG,"selected my floor but bad myFloorIndex "+myFloorIndex);
    			    	        if(findMyLocationAsyncTaskInProgress == false) {
    			    	        	GetMyLocationAsyncTask getMyLocationAsyncTask = new GetMyLocationAsyncTask();
    			    	        	String[] params = {myMac, "true"};
    			    	        	//findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
    			    	        	getMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    			    	        }
    			    			return;
    			    		}
    			    		Log.v(TAG, "selected my floor "+myFloorIndex+"  i "+i);
    			    	}
    			    	else if(floorList != null && i > 0) {
    			    		i = i-1;
    			    		Log.v(TAG, "selected index i "+i);
    			    	}
    			    	if(floorList != null){
	    			    	Log.v(TAG, "selected floor i "+i+"  "+floorList.get(i).floor_name + " grid "+floorList.get(i).grid_size);
	    			    	selectFloorButtonText = floorList.get(i).campus_name+"\n"+floorList.get(i).building_name+" : "+floorList.get(i).floor_name;
				    		selectFloorButton.setText(selectFloorButtonText);
				    		floorListIndex = i;
				    		showingFloorId = floorList.get(i).floor_id;
				    		// download the floorplan
				    		if(floorPlan != null) { floorPlan = null; }
				    		floorPlanView.initialize();
				    		Log.v(TAG, "url for floorplan "+"/api"+floorList.get(i).floor_img_path);
				    		GetFloorplanAsyncTask getFloorplanAsyncTask = new GetFloorplanAsyncTask();
				    		getFloorplanAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "/api"+floorList.get(i).floor_img_path);
    			    	}
    			    }
    			});
    			builder.show();
	    	}
    	}
    };
 
    
    private CharSequence[] floorListing(){
    	if(floorList == null){ 
        	CharSequence[] nullResult = {"we don't have a floor list yet\nplease try again"};
    		return nullResult; 
    	} else {
	    	addToFloorList();  // parses and adds campus and building name objects to each floor object in the list
	    	ArrayList<CharSequence> list = new ArrayList<CharSequence>();
    		list.add("*** TRACK ME ***");
	    	for(int i=0; i<floorList.size(); i++){
	    		list.add(floorList.get(i).campus_name+" : "+floorList.get(i).building_name+" : "+floorList.get(i).floor_name);
	    	}
	    	return (CharSequence[]) list.toArray(new String[0]);
    	}
    }

    
    private void addToFloorList(){
    	for(int i=0; i<floorList.size(); i++){
    		if(floorList.get(i).floor_id.equals(myFloorId)) { myFloorIndex = i; }
    		for(int j=0; j<buildingList.size(); j++){
    			if(buildingList.get(j).building_id.equals(floorList.get(i).building_id)){
    				floorList.get(i).building_name = buildingList.get(j).building_name;
    				for(int k=0; k<campusList.size(); k++){
    					if(campusList.get(k).campus_id.equals(buildingList.get(j).campus_id)){
    						floorList.get(i).campus_id = campusList.get(k).campus_id;
    						floorList.get(i).campus_name = campusList.get(k).campus_name;
    						break;
    					}
    				}
    				break;
    			}
    		}
    	}
    }
    
    
    private OnClickListener pickTargetButtonListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
        	if(v == pickTargetButton){
    			AlertDialog.Builder builder = new AlertDialog.Builder(context);
    			builder.setTitle("Pick a target to track");
    			CharSequence[] targetList = {"show all devices", "show this device ("+myMac+")", "select target by touch", "enter target MAC address (11:22:33:AA:BB:CC)"};
    			builder.setItems(targetList, new DialogInterface.OnClickListener() {
    				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == 0){
							// selected show all MACs
							showAllMacs = true;
							targetMac = null;
							targetHashMac = null;
							pickTargetButtonText = "showing all devices";
							pickTargetButton.setText(pickTargetButtonText);
							Log.v(TAG, "showing all devices");
						}
						if(which == 1){
							// selected show my MAC
							showAllMacs = false;
							targetMac = null;
							targetHashMac = null;
							pickTargetButtonText = "showing this device";
							pickTargetButton.setText(pickTargetButtonText);
							Log.v(TAG, "showing this device");
						}
						if(which == 2){
							// selected to select target MAC by touch
							pickTargetButtonText = "touch a target";
							pickTargetButton.setText(pickTargetButtonText);
							showAllMacs = true;
							targetMac = null;
							targetHashMac = null;
							waitingToTouchTarget = true;
						}
						if(which == 3){
							// selected to enter MAC address
							AlertDialog.Builder macBuilder = new AlertDialog.Builder(context);
							macBuilder.setTitle("Enter target's MAC address  (11:22:33:AA:BB:CC)");
							final EditText input = new EditText(context);
							if(targetHashMac != null) { input.setText(targetHashMac); }
				        	if(targetMac != null) { input.setText(targetMac); }
							macBuilder.setView(input);
							macBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						        public void onClick(DialogInterface dialog, int whichButton) {
						            Editable value = input.getText();
						            if(value.toString() != null) { 
						            	showAllMacs = false;
						            	targetMac = convertToMacFormat(value.toString().toUpperCase(Locale.US));
						            	targetHashMac = targetMac;
						            	Log.v(TAG, "entered target MAC, hashMAC "+targetMac+"  "+targetHashMac);
						            	pickTargetButtonText = "showing "+targetMac;
						            	pickTargetButton.setText(pickTargetButtonText);
						    	        if(findMyLocationAsyncTaskInProgress == false) {
						    	        	GetMyLocationAsyncTask getMyLocationAsyncTask = new GetMyLocationAsyncTask();
						    	        	String[] params = {targetMac, "false"};
						    	        	//findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, targetHashMac);
						    	        	getMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
						    	        }
						            }
						            else { 
						            	Log.w(TAG, "entered target MAC but it was null");
						            	showAllMacs = true;
						            	targetMac = null;
						            	targetHashMac = null;
						            	pickTargetButtonText = "showing all devices";
						            	pickTargetButton.setText(pickTargetButtonText);
						            	return; 
						            }

						        }
							});
							macBuilder.show();
						}

					} 				
    			});
    			builder.show();
    		}
    	}
    };
      
    
    private OnClickListener trackHistoryButtonListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    		if(showHistory){
    			showHistory = false;
    			trackHistoryButton.setText("not showing history");
    		} else {
    			showHistory = true;
    			trackHistoryButton.setText("showing history");
    		}
    	}
    };
    
    
    private OnClickListener surveyModeListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    		if(v == surveyButton){
    			final CharSequence[] modeChoices = {"track mode", "survey mode", "verify mode"};
    			AlertDialog.Builder builder = new AlertDialog.Builder(context);
    			builder.setTitle("Pick a Mode");
    			builder.setSingleChoiceItems(modeChoices, trackMode, new DialogInterface.OnClickListener() {
    			    @Override
    			    public void onClick(DialogInterface dialog, int i) {
    			    	trackMode = i;
    			    }
    			});
    			builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
    				@Override
    				public void onClick(DialogInterface dialog, int i) {
    					setViewText();
    					Log.v(TAG, "set survey button to "+surveyButton.getText().toString());
    	    			if(trackMode == MODE_SURVEY){
    	    				// activate the compass sensors if we are now in survey mode
    	    				try{
    	    					sensorMan.registerListener(sensorListener, sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    	    					sensorMan.registerListener(sensorListener, sensorMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    	        	    	    Log.v(TAG, "sensor listener registered, sensors type all enabled " + sensorMan.getSensorList(Sensor.TYPE_ALL));
    	    				} catch (Exception e) { Log.e(TAG, "Exception registering sensors "+e); }
    	    			} else {
    	    				// deactivate the compass sensors
    	    				try{
    	    					sensorMan.unregisterListener(sensorListener);
    	        	    		Log.v(TAG, "sensor listener unregistered");
    	    				} catch (Exception e) { Log.e(TAG, "Exception unregistering sensors "+e); }
    	    			}
    					return;
    				}
    			});
    			builder.show();
	    	}
    	}
    };
    
    private OnClickListener surveyButtonListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    		
    		if (v == surveyConfirmButton && trackMode == MODE_SURVEY) {
    			Log.v(TAG, "survey point confirmed "+surveyPointX+"  "+surveyPointY);
    			if(wifiManager != null) { wifiManager.startScan(); }  // sends probe requests that the APs will forward to ALE
    			if(floorList != null && floorList.size() > 0 && floorListIndex != -1) {  // don't attempt to send a fingerprint without a floor
    				addSurveyPointToAle();
    			}
    		}
    		
    		else if (v == surveyConfirmButton && trackMode == MODE_VERIFY) {
    			// if the verify button was clicked, bind the centred point with the last ALE position for myMac
    			if(alePositionHistoryList.size() > 0) {
    				Date trueTime = new Date();
    				Date aleTime = alePositionHistoryList.get(0).timestamp;
    				float measuredX = alePositionHistoryList.get(0).measuredX;
    				float measuredY = alePositionHistoryList.get(0).measuredY;
    				VerifyObject newVerifyObject = new VerifyObject(trueTime, surveyPointX, surveyPointY, aleTime, measuredX, measuredY);
    				verifyHistoryList.add(newVerifyObject);
    				float diff = (float)(trueTime.getTime() - aleTime.getTime())/(float)1000;
    				double distDiff = Math.sqrt((surveyPointX - measuredX)*(surveyPointX - measuredX) + (surveyPointY - measuredY)*(surveyPointY - measuredY));
     				Toast toast = Toast.makeText(context, ("add verify point success\n"+String.format("%.2f",surveyPointX)+"  "+String.format("%.2f",surveyPointY)+" error "+String.format("%.2f",distDiff)), Toast.LENGTH_LONG);
    				toast.show();
    				Log.v(TAG, "verify point confirmed "+surveyPointX+","+surveyPointY+"  ale "+measuredX+","+measuredY+"  dist diff "+distDiff+"  time diff "+diff+"  verify hist "+verifyHistoryList.size()+"  position hist "+alePositionHistoryList.size());
    			}
    			// added to allow http location to be used for verify if zmq is disabled
    			else if (zmqEnabled == false && aleHttpPositionHistoryList.size() > 0) {
    				Date trueTime = new Date();
    				Date aleTime = aleHttpPositionHistoryList.get(0).timestamp;
    				float measuredX = aleHttpPositionHistoryList.get(0).measuredX;
    				float measuredY = aleHttpPositionHistoryList.get(0).measuredY;
    				VerifyObject newVerifyObject = new VerifyObject(trueTime, surveyPointX, surveyPointY, aleTime, measuredX, measuredY);
    				verifyHistoryList.add(newVerifyObject);
    				//float diff = (float)(trueTime.getTime() - aleTime.getTime())/(float)1000;
    				double distDiff = Math.sqrt((surveyPointX - measuredX)*(surveyPointX - measuredX) + (surveyPointY - measuredY)*(surveyPointY - measuredY));
     				Toast toast = Toast.makeText(context, ("add verify point success\n"+String.format("%.2f",surveyPointX)+"  "+String.format("%.2f",surveyPointY)+" error "+String.format("%.2f",distDiff)), Toast.LENGTH_LONG);
    				toast.show();
    			}
    			else {
    				Log.w(TAG, "tried to confirm a verify point but alePositionHistoryList was empty and aleHttpPositionHistoryList was empty ");
     				Toast toast = Toast.makeText(context, "tried to confirm a verify point but ALE position lists are empty", Toast.LENGTH_LONG);
     				toast.show();
    			}
    		}
    		
    		else if (v == emailButton && trackMode == MODE_VERIFY) {
    			Log.d(TAG, "clicked email button");
    			emailCsvVerifyFile();
    		}
    		
    	}
    };
    
    // when we get the response to the add survey point post, success or not, we take actions.  We only add the point to the history list if it was successful.
    public static void addSurveyPointToList(PositionHistoryObject pho, boolean success){
    	if(success == true) {
			boolean addIt = surveyHistoryList.add(pho);
			if(addIt == true) {
				Toast toast = Toast.makeText(context, ("add survey point success\n"+pho.touchX+"  "+pho.touchY), Toast.LENGTH_LONG);
				toast.show();
				return;
			}
    	}
		Toast toast = Toast.makeText(context, ("add survey point failed\n"+pho.touchX+"  "+pho.touchY), Toast.LENGTH_LONG);
		toast.show();
    }
    
    // when we get the response to the delete survey point post, success or not, we take actions.  We only add the point to the history list if it was successful.
    public static void deleteSurveyPointFromList(PositionHistoryObject pho, boolean success){
    	if(success == true) {
    		boolean deleteIt = surveyHistoryList.remove(pho);
    		if(deleteIt == true) { 
    			Toast toast = Toast.makeText(context, ("delete survey point success\n"+pho.touchX+"  "+pho.touchY), Toast.LENGTH_LONG);
    			toast.show();
    			return;
    		} 
    	}	
    	Toast toast = Toast.makeText(context, ("delete survey point failed\n"+pho.touchX+"  "+pho.touchY), Toast.LENGTH_LONG);
    	toast.show();
	}
    
    // sends a POST to ALE with new survey point
    public static void addSurveyPointToAle(){
		if(postFingerprintAsyncTaskInProgress == false) {
			SurveyObject surveyObject = new SurveyObject(formSurveyPositionHistoryObject(), "add", false, 0);
    		PostFingerprintAsyncTask postFingerprintAsyncTask = new PostFingerprintAsyncTask();
    		postFingerprintAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, surveyObject);
    		
    		if(sendChunkedDataAsyncTaskInProgress == false) {
    			PostChunkedDataAsyncTask postChunkedDataAsyncTask = new PostChunkedDataAsyncTask();
    			postChunkedDataAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    		}
    			
		}
    }
    
    // sends a POST to ALE to delete a survey point
    public static void deleteSurveyPointFromAle(PositionHistoryObject pho){
		if(postFingerprintAsyncTaskInProgress == false) {
			SurveyObject surveyObject = new SurveyObject(pho, "delete", false, 0);
    		PostFingerprintAsyncTask postFingerprintAsyncTask = new PostFingerprintAsyncTask();
    		postFingerprintAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, surveyObject);
		}
    }
    
    public static PositionHistoryObject formSurveyPositionHistoryObject() {
    	Date date = new Date();
    	String floorId = floorList.get(floorListIndex).floor_id;
    	String units = floorList.get(floorListIndex).units;
    	String deviceMfg = Build.MANUFACTURER;
    	String deviceModel = Build.MODEL;
    	return new PositionHistoryObject(date, surveyPointX, surveyPointY, 0, 0, 0, false, 0, 
    			floorId, "XXX", "XXX", myMac, "XX", units, deviceMfg, deviceModel, compassDegrees, iBeaconJsonArray);
    }
        
    
    private OnClickListener settingsOnClickListener = new OnClickListener(){
    	@Override
    	public void onClick(final View v) {
    		
    		// builder1 shows all the settings in an alert dialog list with an OK button to exit
			AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
			builder1.setTitle("current settings");
			String scanningEnabledString = "disabled";
			if(scanningEnabled) { scanningEnabledString = "enabled"; }
			String enableZmqString = "disabled";
			if(zmqEnabled) { enableZmqString = "enabled"; }
			String moveAcrossFloorsString = "disabled";
			if(moveAcrossFloors) { moveAcrossFloorsString = "enabled"; }
			final String[] titles = {"ALE host address  ", "ALE port (8443)  ", "ALE username  ", "ALE password  ", "Scanning ", "Floorplan download port (80)  ", 
					"ZMQ publish-subscribe ", "Follow moves across floors ", "Reset ALE Demonstrator (clear all data)"};
			final String[] values = {aleHost, alePort, aleUsername, "*password*", scanningEnabledString, floorplanDownloadPort, enableZmqString, moveAcrossFloorsString, ""};
			CharSequence[] targetList = {titles[0]+values[0], titles[1]+values[1], titles[2]+values[2], titles[3]+values[3], titles[4]+values[4], titles[5]+values[5], 
					titles[6]+values[6], titles[7]+values[7], titles[8]+values[8]};
			builder1.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				// this returns from the alert dialog to the main view.
				@Override
				public void onClick(DialogInterface dialog, final int which) {
					Log.v(TAG, "saving dialog with aleHost "+aleHost+" : "+alePort);
					saveSharedPreferences();
					return;
				}
			});
			builder1.setItems(targetList, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, final int which) {			
					// builder2 brings up an alert dialog for each setting depending on which was touched from the builder1 list
					if(which != 4 && which != 6 && which != 7 && which != 8) {
						final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
						final EditText input = new EditText(context);
						// put the value from the list into this new alert dialog
						if(values[which] != null) { input.setText(values[which]); }
						// if it's the password we show an empty edittext
						if(which == 3) { input.setText(null); }					
						// now launch a separate dialog for whichever builder1 settings item was chosen and populate with the current setting
						builder2.setView(input);
						builder2.setTitle(titles[which]);
						// this positions the cursor at the right end of the edittext when it appears
						input.setSelection(input.getText().length());
						builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// read the new value for the individual setting
					            Editable value = input.getText();
					            if(value.toString() != null && value.toString().length() > 0) { 
					            	Log.v(TAG, "entered new "+titles[which]+"  _"+value.toString()+"_");
					            	if(which == 0) {
					            		aleHost = value.toString();
										resetAllData();
										setViewText();
										initialiseConfigViews();
										initialiseZmqAndOthers();
										initialiseConfigViews();
					            		Log.v(TAG, "new ALE Host, clearing floorList "+aleHost+" : "+alePort);
					            	}
					            	if(which == 1) {
					            		alePort = value.toString();
					            		floorList = null;
					            		myHashMac = null;
					            		initialiseConfigViews();
					            		Log.v(TAG, "new ALE Port, clearing floorList "+aleHost+" : "+alePort);
					            	}
					            	if(which == 2) aleUsername = value.toString();
					            	if(which == 3) alePassword = value.toString();
//					            	if(which == 4) {
//					            		if(value.toString().equalsIgnoreCase("Scanning enabled")) { scanningEnabled = true; }
//					            		else { scanningEnabled = false; }
//					            	}
					            	if(which == 5) floorplanDownloadPort = value.toString();
					            	initialiseConfigViews();
					            }
					            else { Log.w(TAG, "entered new "+titles[which]+" but it was null"); }
					            settingsOnClickListener.onClick(v);  // this brings back a fresh dialog with the _new_ settings values rather than a new dialog of the old settings
					        }
						});
						// this makes sure the keyboard is pulled up and the cursor placed in the settings alert dialog when it appears
						AlertDialog alertToShow = builder2.create();
						alertToShow.getWindow().setSoftInputMode(
						    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						alertToShow.show();
						//builder2.show();	
					} else if (which == 4) {
						final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
						builder2.setTitle("enable background scanning");
						builder2.setNegativeButton("NO", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// read the new value for background scanning
								Log.v(TAG, "set scanning to false");
								scanningEnabled = false;
								initialiseConfigViews();
								settingsOnClickListener.onClick(v);
					        }
						});
						builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// read the new value for background scanning
								Log.v(TAG, "set scanning to true");
								scanningEnabled = true;
								initialiseConfigViews();
								settingsOnClickListener.onClick(v);
					        }
						});
						// this makes sure the keyboard is pulled up and the cursor placed in the settings alert dialog when it appears
						AlertDialog alertToShow = builder2.create();
						alertToShow.getWindow().setSoftInputMode(
						    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						alertToShow.show();
					}  else if (which == 6) {
						final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
						builder2.setTitle("enable ZMQ");
						builder2.setNegativeButton("NO", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// read the new value for zmq
								Log.v(TAG, "set ZMQ to false");
								zmqEnabled = false;
								initialiseZmqAndOthers();
								initialiseConfigViews();
								settingsOnClickListener.onClick(v);
					        }
						});
						builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// read the new value for zmq
								Log.v(TAG, "set ZMQ to true");
								zmqEnabled = true;
								initialiseZmqAndOthers();
								initialiseConfigViews();
								settingsOnClickListener.onClick(v);
					        }
						});
						// this makes sure the keyboard is pulled up and the cursor placed in the settings alert dialog when it appears
						AlertDialog alertToShow = builder2.create();
						alertToShow.getWindow().setSoftInputMode(
						    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						alertToShow.show();
					} else if (which == 7) {
							final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
							builder2.setTitle("Follow moves across floors");
							builder2.setNegativeButton("NO", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {		
									// read the new value for follow moves across floors
									Log.v(TAG, "set follow moves across floors to false");
									moveAcrossFloors = false;
									initialiseZmqAndOthers();
									initialiseConfigViews();
									settingsOnClickListener.onClick(v);
						        }
							});
							builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {		
									// read the new value for follow moves across floors
									Log.v(TAG, "set follow moves across floors to true");
									moveAcrossFloors = true;
									initialiseZmqAndOthers();
									initialiseConfigViews();
									settingsOnClickListener.onClick(v);
						        }
							}
						);
						// this makes sure the keyboard is pulled up and the cursor placed in the settings alert dialog when it appears
						AlertDialog alertToShow = builder2.create();
						alertToShow.getWindow().setSoftInputMode(
						    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						alertToShow.show();
					} else if (which == 8) {
						final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
						builder2.setTitle("reset ALE Demonstrator");
						builder2.setNegativeButton("NO", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// do nothing
					        }
						});
						builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {		
								// reset all the dynamic data
								Log.v(TAG, "set 'reset ALE Demonstrator' to true");
								resetAllData();
								setViewText();
								initialiseZmqAndOthers();
								initialiseConfigViews();
								settingsOnClickListener.onClick(v);
					        }
						});
						// this makes sure the keyboard is pulled up and the cursor placed in the settings alert dialog when it appears
						AlertDialog alertToShow = builder2.create();
						alertToShow.getWindow().setSoftInputMode(
						    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						alertToShow.show();
					} 
				}
			});
			builder1.show();
		} 		
    };
    
    
	// Listener for compass updates
    SensorEventListener sensorListener = new SensorEventListener(){
	   public void onAccuracyChanged(Sensor sensor, int accuracy){}
	   public void onSensorChanged(SensorEvent event){
		   switch(event.sensor.getType()){	
		   case Sensor.TYPE_ACCELEROMETER:
			   for(int i =0; i < 3; i++){
				    valuesAccelerometer[i] = event.values[i];
			   }			    
			   break;
		   case Sensor.TYPE_MAGNETIC_FIELD:
			   for(int i =0; i < 3; i++){
		   		   	valuesMagneticField[i] = event.values[i];
		   		}
			   break;
		   }

		   float[] matrixR = new float[9];
		   float[] matrixI = new float[9];
		   boolean success = SensorManager.getRotationMatrix( matrixR, matrixI, valuesAccelerometer, valuesMagneticField);    
		  if(success){
			  float[] matrixValues = new float[3];
			  SensorManager.getOrientation(matrixR, matrixValues);			     
			  compassDegrees = (float)Math.toDegrees(matrixValues[0]);
			  if(compassDegrees <0 ) { compassDegrees = (float)360 + compassDegrees;  }
			  //Log.v(TAG, "compass degrees "+compassDegrees);
		  }
	   }
    };
    
    public String convertToMacFormat(String in){
    	String result = "";
    	try {
    		if(in.length() == 12) {
    			result = in.substring(0,2)+":"+in.substring(2,4)+":"+in.substring(4,6)+":"+in.substring(6,8)+":"+in.substring(8,10)+":"+in.substring(10,12);
    		}
    		result = result.toUpperCase(Locale.US);
    	}catch (Exception e) { Log.e(TAG, "Exception converting input to MAC format "+in+"  "+e); }
    	return result;
    }
    
    private boolean initialiseBluetooth() {
    	// if the device doesn't support BLE then return false
    	boolean result = false;
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { 
			Log.w(TAG, "this device is capable of bluetooth LE");
			result = initialiseBluetoothAdapter(); 
		}
		return result;
    }
    
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public boolean initialiseBluetoothAdapter(){
	// now see if we can get the BLE adapter.  If not, return false
		bluetoothManager = (BluetoothManager) MainActivity.context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null){ 
			Log.w(TAG, "could not enable bluetooth LE adapter");
			return false; 
		} 
		
		// now see if the BLE adapter is enabled.  If so, return true.  Otherwise false
		if(bluetoothAdapter.isEnabled() == true) { 
			Log.i(TAG, "bluetooth LE adapter was already enabled ");
			return true; 
		}
		else {
			Log.w(TAG, "bluetooth LE adapter is not enabled, we'll try to enable it");
		    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
		    try { Thread.sleep(500); } catch (Exception e) { Log.e(TAG, "Exception sleeping "+e); }
		    if(bluetoothAdapter.isEnabled() == true ) { 
		    	Log.i(TAG, "bluetooth LE adapter is now enabled ");
		    	return true; 
		    }
		}
		return false;
	}	
	
	@SuppressLint("SimpleDateFormat")
    public void emailCsvVerifyFile(){
		if(verifyHistoryList.size() > 0) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MMdd_HHmmss");
			String s = "verify point for "+myMac+",time diff sec,distance diff,true x,true y,last ALE date,last ALE x,last ALE y\n";
			for(int i=0; i<verifyHistoryList.size(); i++) {
				s = s + verifyHistoryList.get(i).toCsv();
			}
			CharSequence tViewCharSeq = s;
			Log.d(TAG, "sending csv file \n"+s);
			String logFileName = new String ("ALE_VerifyFile_" + simpleDateFormat.format(new Date()) + ".csv");
			File logFile = new File(MainActivity.context.getExternalFilesDir(null), logFileName);
			logFile.setReadable(true);
			try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile));
				bufferedWriter.append(tViewCharSeq);
				bufferedWriter.flush();
				bufferedWriter.close();	
			} catch (IOException e) {
				Log.e(TAG, "Exception saving csv log file "+e);
				Toast.makeText(MainActivity.context, "Could not save csv log file" + e, Toast.LENGTH_LONG).show();
			}		
			try {
				Log.v(TAG, "Sending log file length "+logFile.length());
		        Intent sendIntent = new Intent(Intent.ACTION_SEND);
		      	sendIntent.setType("text/plain");
		      	if(csvEmailAddress != "") sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{csvEmailAddress});
		    	sendIntent.putExtra(Intent.EXTRA_SUBJECT, "ALE Verify File Attached");
		    	sendIntent.putExtra(Intent.EXTRA_TEXT, logFileName+" file size "+logFile.length());
		    	sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+logFile.getAbsolutePath()));
		    	startActivity(Intent.createChooser(sendIntent, "Email:"));
			} catch (Exception e) { Log.e(TAG, "sending email "+e); }
		}
		else {
			Log.w(TAG, "wanted to email csv verify file but the list was empty");
			Toast.makeText(MainActivity.context, "wanted to email verify file but it was empty", Toast.LENGTH_LONG).show();
		}
	}
	
    
    private void readSharedPreferences(){
    	Log.i(TAG, "reading shared preferences");
    	SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
    	aleHost = sharedPreferences.getString("hostAddress", aleHost);
    	alePort = sharedPreferences.getString("hostPort", alePort);
    	aleHostTextView.setText(aleHost+" port "+alePort);
    	aleUsername = sharedPreferences.getString("userid", aleUsername);
    	aleUsernameTextView.setText(aleUsername);
    	alePassword = sharedPreferences.getString("password", alePassword);
    	showHistory = sharedPreferences.getBoolean("showHistory",  false);
    	scanningEnabled = sharedPreferences.getBoolean("scanningEnabled", false);
    	if(scanningEnabled) { scanningEnabledTextView.setText("scanning enabled"); }
    	else { scanningEnabledTextView.setText("scanning disabled"); }
    	floorplanDownloadPort = sharedPreferences.getString("floorplanDownloadPort", floorplanDownloadPort);
    	zmqEnabled = sharedPreferences.getBoolean("zmqEnabled", zmqEnabled);
    	moveAcrossFloors = sharedPreferences.getBoolean("moveAcrossFloors", moveAcrossFloors);
    	trustedCert = sharedPreferences.getString("trustedCert", null);
    }
    
    private void saveSharedPreferences(){
    	Log.i(TAG, "saving shared preferences");
    	SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
    	editor.putString("hostAddress", aleHost);
    	editor.putString("hostPort", alePort);
    	editor.putString("userid", aleUsername);
    	editor.putString("password", alePassword);
    	editor.putBoolean("showHistory", showHistory);
    	editor.putBoolean("scanningEnabled", scanningEnabled);
    	editor.putString("floorplanDownloadPort", floorplanDownloadPort);
    	editor.putBoolean("zmqEnabled", zmqEnabled);
    	editor.putBoolean("moveAcrossFloors", moveAcrossFloors);
    	editor.putString("trustedCert", trustedCert);
    	editor.commit();
    }
    
    
    private void initialiseZmqAndOthers(){
		try{
			zmqSubscriber.interrupt();
			zmqStatusString = "Restarting with "+aleHost;
			Log.v(TAG, "interrupting ZMQ thread");
		} catch (Exception e) { Log.e(TAG, "Exception in initialize interrupting ZMQ thread "+e); }
		try { Thread.sleep(1000); } catch (Exception e) { Log.e(TAG, "Exception sleeping in initializeZmq "+e);	}
		zmqSubscriber = null;
		zmqMessageCounter = 0;
		zmqMessagesForMyMac = 0;
		zmqLastSeq = 0;
		zmqMissedSeq = 0;	
		zmqHandler = new Handler(){
	    	@Override
	    	public void handleMessage(Message msg){ zmqMessageReceived(msg); }
	    }; 

		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		try { myMac = wifiInfo.getMacAddress().toUpperCase(Locale.US); } catch (Exception e) { Log.e(TAG, "Exception getting MAC address "+e); }
        
		aleDiscoveryAsyncTaskInProgress = false;
        getFloorplanAsyncTaskInProgress = false;
    	findMyLocationAsyncTaskInProgress = false;
        postFingerprintAsyncTaskInProgress = false;
        sendChunkedDataAsyncTaskInProgress = false;
        getCookieAsyncTaskInProgress = false;
        
        GetMyLocationAsyncTask getMyLocationAsyncTask = new GetMyLocationAsyncTask();
        String[] params = {myMac, "true"};
        getMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		customVerifier.trustAllHosts();
		
    }
    
    public static boolean validCookie() {
    	boolean result = false;
    	List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
    	for(int i=0; i<cookieList.size(); i++){
    		if(cookieList.get(i).getDomain() != null && cookieList.get(i).getDomain().equals(aleHost) ){
    			long remainingLifetime = cookieExpires - System.currentTimeMillis();
    			if (remainingLifetime > 15000) { result = true; } 
    			else { Log.d(TAG, "BANG4! no valid cookie!"); }
    		}
    	}
    	return result;
    }
    
    public static void printCookies() {
    	List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
    	for(int i=0; i<cookieList.size(); i++){
    		DateFormat df = new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss z");
    		df.setTimeZone(TimeZone.getTimeZone("GMT"));
    		String date = df.format(Calendar.getInstance().getTime());
			Log.d(TAG, "cookie "+i+" name "+cookieList.get(i).getName()+" domain "+cookieList.get(i).getDomain()+"  ID "+cookieList.get(i).getValue()+
					"/nmax_age "+cookieList.get(i).getMaxAge()+"  ttl msec"+(cookieExpires - System.currentTimeMillis())+" timeNow "+date );
    	}
    }
    
    public void showTrustDialog() {	
    	Log.d(TAG, "BANG20! show trust dialog with "+certData+"  "+context.toString());
    	AlertDialog.Builder builder = new AlertDialog.Builder( context );
		builder.setTitle("Could not verify Server Certificate");
		builder.setMessage("Click yes to accept this certificate\n"+certData);
		builder.setCancelable(false);
		builder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						Log.d(TAG, "BANG21! ACCEPTED NEW CERT");
						trustedCert = certData;
						blacklistCertBool = false;
						checkCertBool = false;
						saveSharedPreferences();
						dialog.cancel();
					}
				  });
		builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						Log.d(TAG, "BANG22! REJECTED NEW CERT");
						blacklistCertBool = true;
						checkCertBool = false;
						dialog.cancel();
					}
				});
		builder.show();	
    }
    
    
    public void resetAllData(){
    	try {
	    aleAllPositionHistoryMap.clear();
		alePositionHistoryList = new ArrayList<PositionHistoryObject>();
		aleHttpPositionHistoryList = new ArrayList<PositionHistoryObject>();
		surveyHistoryList = new ArrayList<PositionHistoryObject>();
		verifyHistoryList = new ArrayList<VerifyObject>();
		eventLogMap = new HashMap<String, ArrayList<String>>(500);
		if(floorPlan != null) { floorPlan = null; }
		floorList.clear();
		floorListIndex = -1;
		httpStatusString1 = "http Status";
		httpStatusString2 = "";
		zmqStatusString = "ZMQ Status";
		selectFloorButtonText = "select a floor to track";
		pickTargetButtonText = "showing all";
		myMac = ""; 
		myHashMac = null;
		myFloorId = null;
		myFloorIndex = -1;
		targetMac = null;
		targetHashMac = null;
		showHistory = false;
		showAllMacs = true;
		waitingToTouchTarget = false;
		touchTargetHashMac = null;
		trackMode = MODE_TRACK;
		checkCertBool = false;
		blacklistCertBool = false;
    	} catch (Exception e) { Log.e(TAG, "resetAllData "+e); }
    }
    
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
     
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {		
		case R.id.menu_exit:
			finish();
			break;
		}
		return false;
	}
    
    public void setViewsAndListeners(){
		aleHostTextView = (TextView) findViewById(R.id.aleHostTextView);
		aleHostTextView.setOnClickListener(settingsOnClickListener);
		scanningEnabledTextView = (TextView) findViewById(R.id.scanningEnabledTextView);
		scanningEnabledTextView.setOnClickListener(settingsOnClickListener);
		aleUsernameTextView = (TextView) findViewById(R.id.aleUsernameTextView);
		aleUsernameTextView.setOnClickListener(settingsOnClickListener);
		alePasswordTextView = (TextView) findViewById(R.id.alePasswordTextView);
		alePasswordTextView.setOnClickListener(settingsOnClickListener);
		selectFloorButton = (Button) findViewById(R.id.selectionButtonFloor);
        selectFloorButton.setOnClickListener(buttonFloorListener);
        selectFloorButton.setFocusableInTouchMode(true);
        selectFloorButton.requestFocus();
        statusText01 = (TextView) findViewById(R.id.statusText01);
        statusText02 = (TextView) findViewById(R.id.statusText02);
        pickTargetButton = (Button) findViewById(R.id.pickTargetButton);
        pickTargetButton.setOnClickListener(pickTargetButtonListener);
        trackHistoryButton = (Button) findViewById(R.id.trackHistoryButton);
        trackHistoryButton.setOnClickListener(trackHistoryButtonListener);
        surveyButton = (Button) findViewById(R.id.surveyButton);
        surveyButton.setOnClickListener(surveyModeListener);
        emailButton = (Button) findViewById(R.id.emailButton);
        emailButton.setOnClickListener(surveyButtonListener);
        surveyConfirmButton = (Button) findViewById(R.id.surveyConfirmButton);
        surveyConfirmButton.setOnClickListener(surveyButtonListener);
		floorPlanView = (FloorPlanView) findViewById(R.id.FloorPlanView);
    }
    
    public void setViewText(){
    	selectFloorButton.setText(selectFloorButtonText);
		statusText01.setText("HTTP STATUS\n"+httpStatusString1+"\n"+httpStatusString2);
		statusText02.setText("ZMQ STATUS\n"+zmqStatusString);
		pickTargetButton.setText(pickTargetButtonText);
    	if(showHistory) { trackHistoryButton.setText("showing history"); }
    	else { trackHistoryButton.setText("not showing history"); }
		if(trackMode == MODE_TRACK) { 
			surveyButton.setText("in track mode"); 
			emailButton.setVisibility(View.GONE);
			surveyConfirmButton.setVisibility(View.GONE);
		}
		else if(trackMode == MODE_SURVEY) { 
			surveyButton.setText("in survey mode"); 
			emailButton.setVisibility(View.GONE);
			surveyConfirmButton.setVisibility(View.VISIBLE);
			surveyConfirmButton.setText("confirm survey pt");
		}
		else if(trackMode == MODE_VERIFY) { 
			surveyButton.setText("in verify mode"); 
			emailButton.setVisibility(View.VISIBLE);
			surveyConfirmButton.setVisibility(View.VISIBLE);
			surveyConfirmButton.setText("confirm pt");
		}
    }
    
    public void initialiseConfigViews(){
    	aleHostTextView.setText(aleHost+" port "+alePort);
    	if(scanningEnabled) { scanningEnabledTextView.setText("scanning enabled"); }
    	else { scanningEnabledTextView.setText("scanning disabled"); }
    	aleUsernameTextView.setText(aleUsername);
    	alePasswordTextView.setText("*password*");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig){
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.activity_main); 
    	setViewsAndListeners();
    	setViewText();
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG, "onPause");
    	try{ sensorMan.unregisterListener(sensorListener); } catch (Exception e) { Log.e(TAG, "Exception unregistering sensor listener "+e); }
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	Log.i(TAG, "onResume");
    }
    
    @Override
    public void onStart(){
    	super.onStart();
    	Log.i(TAG, "onStart");
    	//readSharedPreferences();
    	//setViewText();
    	runnable.run();
		if(bluetoothEnabled) { 
			try {
				bluetoothBeaconScanner = new BluetoothBeaconScanner(); 
				bluetoothBeaconScanner.bluetoothScanRunnable.run();
			} catch (Exception e) { Log.e(TAG, "Exception starting bluetoothScanRunnable "+e); }
		}
    }
    
	@Override
	public void onStop(){
		super.onStop();
		Log.i(TAG, "onStop");
		saveSharedPreferences();
		if(zmqEnabled) {
			try{
				zmqSubscriber.interrupt();
				Log.v(TAG, "onStop interrupting ZMQ thread");
			} catch (Exception e) { Log.e(TAG, "Exception in initialize interrupting ZMQ thread "+e); }
			try { Thread.sleep(1000); } catch (Exception e) { Log.e(TAG, "Exception sleeping in initializeZmq "+e);	}
			zmqSubscriber = null;
			zmqStatusString = "Stopping with "+aleHost;
		}
		zmqMessageCounter = 0;
		zmqMessagesForMyMac = 0;
		zmqLastSeq = 0;
		zmqMissedSeq = 0; 
		try{
			handler.removeCallbacks(runnable);
		} catch (Exception e) { Log.e(TAG, "onStop() exception stopping runnable "+e); }
    	aleDiscoveryAsyncTaskInProgress = false;
    	getFloorplanAsyncTaskInProgress = false;
    	findMyLocationAsyncTaskInProgress = false;
    	postFingerprintAsyncTaskInProgress = false;
    	sendChunkedDataAsyncTaskInProgress = false;
    	getCookieAsyncTaskInProgress = false;
    	checkCertBool = false;
    	blacklistCertBool = false;
    	if(bluetoothBeaconScanner != null) {
    	try {
    		bluetoothBeaconScanner.stopScanning();
    	} catch (Exception e) { Log.e(TAG, "Exception stopping bluetooth handler "+e); }
    	}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "onDestroy");
/*		if(zmqEnabled) {
			try{
				zmqSubscriber.interrupt();
				Log.v(TAG, "onStop interrupting ZMQ thread");
			} catch (Exception e) { Log.e(TAG, "Exception in initialize interrupting ZMQ thread "+e); }
			try { Thread.sleep(1000); } catch (Exception e) { Log.e(TAG, "Exception sleeping in initializeZmq "+e);	}
			zmqSubscriber = null;
			zmqStatusString = "Stopping with "+aleHost;
		}
		zmqMessageCounter = 0;
		zmqMessagesForMyMac = 0;
		zmqLastSeq = 0;
		zmqMissedSeq = 0; */
//	    aleAllPositionHistoryMap.clear();
//		alePositionHistoryList = new ArrayList<PositionHistoryObject>();
//		surveyHistoryList = new ArrayList<PositionHistoryObject>();
//		eventLogMap = new HashMap<String, ArrayList<String>>(500);
//		pickTargetButtonText = "showing all";
//		showAllMacs = true;
		
	}
	
	
}
