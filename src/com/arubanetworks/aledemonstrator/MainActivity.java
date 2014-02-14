package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;


import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;


public class MainActivity extends Activity {
	String TAG = "MainActivity";
	TextView aleHostTextView;
	TextView aleUsernameTextView;
	TextView alePasswordTextView;
	Button selectFloorButton;
	TextView statusText01;
	TextView statusText02;
	static Button pickTargetButton;
	Button trackHistoryButton;
	FloorPlanView floorPlanView;
	
	final Context context = this;

	static ArrayList<AleCampus> campusList;
	static ArrayList<AleBuilding> buildingList;
	static ArrayList<AleFloor> floorList;
	static int floorListIndex = -1;
	
	private volatile ZMQSubscriber zmqSubscriber;
	static Handler zmqHandler;
	static String[] zmqFilter = {"location", "presence", "station", "destination", "application", "device_rec"};  // change this to get different feeds
	//static String[] zmqFilter = {"location"};
	static String ZMQ_PROGRESS_MESSAGE = "zmqProgress";
	static String zmqStatusString = "ZMQ Status";
	static long zmqMessageCounter = 0;
	static long zmqMessagesForMyMac = 0;
	static long zmqLastSeq = 0;
	static long zmqMissedSeq = 0;
	
	static String aleHost = "10.4.250.13";
	static float site_xAle = 0;
	static float site_yAle = 0;
	static Bitmap floorPlan;
	static String aleUsername = "root";
	static String alePassword = "Aruba@2013";
	static String httpStatusString = "http Status";
	
	AleDiscoveryAsyncTask aleDiscoveryAsyncTask;
	static boolean aleDiscoveryAsyncTaskInProgress = false;	
	static boolean downloadFloorplanAsyncTaskInProgress = false;	
	static boolean findMyLocationAsyncTaskInProgress = false;
	Handler handler = new Handler();
	int counter = 0;
	static int DELAY = 1000;
	
	static String myMac = null;
	static String myHashMac = null;
	static String myFloorId = null;
	static int myFloorIndex = -1;
	
//	static String targetMac = null;
	static String targetHashMac = null;
	
	static boolean showHistory = false;
	static boolean showAllMacs = true;
	
	static boolean waitingToTouchTarget = false;
	static String touchTargetHashMac = null;
	
    static HashMap<String, ArrayList<PositionHistoryObject>> aleAllPositionHistoryMap = new HashMap<String, ArrayList<PositionHistoryObject>>(500);
	static ArrayList<PositionHistoryObject> alePositionHistoryList = new ArrayList<PositionHistoryObject>();
	static HashMap<String, ArrayList<String>> eventLogMap = new HashMap<String, ArrayList<String>>(500);
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setViewsAndListeners();
		
		readSharedPreferences();
		initialize();   

	    runnable.run();
				
	}
    
  
    Runnable runnable = new Runnable(){
    	public void run(){
    		statusText01.setText("HTTP STATUS\n"+httpStatusString);
    		statusText02.setText("ZMQ STATUS\n"+zmqStatusString);
    		
            if(counter%10 == 1 && (floorList == null || floorList.size() < 1) && aleDiscoveryAsyncTaskInProgress == false) {
            	AleDiscoveryAsyncTask aleDiscoveryAsyncTask = new AleDiscoveryAsyncTask();
            	aleDiscoveryAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            
	        if(counter%30 == 1 && myHashMac == null && findMyLocationAsyncTaskInProgress == false) {
	        	FindMyLocationAsyncTask findMyLocationAsyncTask = new FindMyLocationAsyncTask();
	        	findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
	        }
    		
            setUpFloorPlanView();
            floorPlanView.invalidate();
	
    		if(zmqSubscriber == null){
        		try{
        			zmqSubscriber = new ZMQSubscriber(zmqHandler, zmqFilter);
        			zmqSubscriber.start();
        			Log.v(TAG, "zmqSubscriber was null, restarting with host "+aleHost);
        		} catch (Exception e) { Log.e(TAG, "Exception starting new thread for zmqSubscriber "+e); }
    		} 
        	
    		if(zmqMessageCounter > 0) zmqStatusString = +zmqMessageCounter+" ZMQ messages, "+zmqMessagesForMyMac+" for my MAC ";
    		
    		counter++;
        	handler.postDelayed(this, DELAY);
    	}
    };
    
    private void zmqMessageReceived(Message msg){
    	if(msg.getData().containsKey(ZMQ_PROGRESS_MESSAGE)) {
    		Log.v(TAG, "new ZMQ progress message");
    		try{
    			String progress = new String(msg.getData().getByteArray(ZMQ_PROGRESS_MESSAGE), "UTF-8");
    			if(progress.contains("Closed")) {
     				zmqSubscriber = null;
    				zmqMessageCounter = 0;
    				zmqMessagesForMyMac = 0;
    				zmqMissedSeq = 0;
    				progress = "socket closed";
    			}
    			zmqStatusString = "ZMQ "+progress;
    			Log.v(TAG, "the zmq message was a progress message... "+progress);
    		} catch (Exception e) { Log.e(TAG, "Exception reading progress message content as string "+e); }
    	} else  if (msg.getData() != null){
    		zmqMessageCounter++;
    		Set<String> keySet = msg.getData().keySet();
    		for(String s : keySet) {
    			//Log.v(TAG, "new message with key "+s);
    			byte[] messageBody = msg.getData().getByteArray(s);
    			if(zmqMessageCounter%500 == 1) Log.v(TAG, "zmq message count "+zmqMessageCounter+" last message length "+messageBody.length);
    			ProtobufParsers.parseAleMessage(messageBody);
    		}
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
	    			    			selectFloorButton.setText("can't find the floor yet\nplease try again"); 
	    			    			floorListIndex = -1;
	    			    			floorPlan = null;
	    			    			floorPlanView.initialize();
	    			    			Log.v(TAG,"selected my floor but bad myFloorIndex "+myFloorIndex);
	    			    	        if(findMyLocationAsyncTaskInProgress == false) {
	    			    	        	FindMyLocationAsyncTask findMyLocationAsyncTask = new FindMyLocationAsyncTask();
	    			    	        	findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
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
		    			    	Log.v(TAG, "selected floor i "+i+"  "+floorList.get(i).floor_name);
					    		selectFloorButton.setText(floorList.get(i).campus_name+"\n"+floorList.get(i).building_name+" : "+floorList.get(i).floor_name);
					    		floorListIndex = i;
					    		// download the floorplan
					    		floorPlan = null;
					    		floorPlanView.initialize();
					    		Log.v(TAG, "url for floorplan "+floorList.get(i).floor_img_path);
					    		DownloadFloorplanAsyncTask downloadFloorplanAsyncTask = new DownloadFloorplanAsyncTask();
					    		downloadFloorplanAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, floorList.get(i).floor_img_path);
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
							targetHashMac = null;
							pickTargetButton.setText("showing all devices");
							Log.v(TAG, "showing all devices");
						}
						if(which == 1){
							// selected show my MAC
							showAllMacs = false;
							targetHashMac = null;
							pickTargetButton.setText("showing this device");
							Log.v(TAG, "showing this device");
						}
						if(which == 2){
							// selected to select target MAC by touch
							pickTargetButton.setText("touch a target");
							showAllMacs = true;
							targetHashMac = null;
							waitingToTouchTarget = true;
						}
						if(which == 3){
							// selected to enter MAC address
							AlertDialog.Builder macBuilder = new AlertDialog.Builder(context);
							macBuilder.setTitle("Enter target's MAC address  (11:22:33:AA:BB:CC)");
							final EditText input = new EditText(context);
				        	if(targetHashMac != null) { input.setText(targetHashMac); }
							macBuilder.setView(input);
							macBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						        public void onClick(DialogInterface dialog, int whichButton) {
						            Editable value = input.getText();
						            if(value.toString() != null) { 
						            	Log.v(TAG, "entered target MAC "+targetHashMac);
						            	showAllMacs = false;
						            	targetHashMac = value.toString().toUpperCase(Locale.US);
					            		//targetHashMac = null;
						            	pickTargetButton.setText("showing "+targetHashMac);
						            }
						            else { 
						            	Log.w(TAG, "entered target MAC but it was null");
						            	showAllMacs = true;
						            	//targetMac = null;
						            	targetHashMac = null;
						            	pickTargetButton.setText("showing all devices");
						            	return; 
						            }

						        }
							});
							macBuilder.show();
			    	        if(findMyLocationAsyncTaskInProgress == false) {
			    	        	FindMyLocationAsyncTask findMyLocationAsyncTask = new FindMyLocationAsyncTask();
			    	        	findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, targetHashMac);
			    	        }
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
    
    private void setUpFloorPlanView(){
    	floorPlanView.thisFloorPlan = null;
    	floorPlanView.invalidate();
    	if(floorListIndex != -1 && floorPlan != null){

    	}
    }
    
    private OnClickListener settingsOnClickListener = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    			AlertDialog.Builder builder = new AlertDialog.Builder(context);
    			builder.setTitle("current settings");
    			final String[] titles = {"ALE host address  ", "ALE username  ", "ALE password  " };
    			final String[] values = {aleHost, aleUsername, "*password*" };
    			CharSequence[] targetList = {titles[0]+values[0], titles[1]+values[1], titles[2]+values[2]};
    			builder.setItems(targetList, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, final int which) {
							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							final EditText input = new EditText(context);
							if(values[which] != null) { input.setText(values[which]); }
							if(which == 2) { input.setText(null); }
							builder.setView(input);
							builder.setTitle(titles[which]);
							builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						        public void onClick(DialogInterface dialog, int whichButton) {
						            Editable value = input.getText();
						            if(value.toString() != null && value.toString().length() > 0) { 
						            	Log.v(TAG, "entered new "+titles[which]+"  _"+value.toString()+"_");
						            	if(which == 0) aleHost = value.toString();
						            	if(which == 1) aleUsername = value.toString();
						            	if(which == 2) alePassword = value.toString();
						            	initializeConfigViews();
						            }
						            else { Log.w(TAG, "entered new "+titles[which]+" but it was null"); }
						        }
							});
							builder.show();
					}
    			});
    			builder.show();
    		} 		
    };
    
    
    private void readSharedPreferences(){
    	Log.i(TAG, "reading shared preferences");
    	SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
    	aleHost = sharedPreferences.getString("hostAddress", aleHost);
    	aleHostTextView.setText(aleHost);
    	aleUsername = sharedPreferences.getString("userid", aleUsername);
    	aleUsernameTextView.setText(aleUsername);
    	alePassword = sharedPreferences.getString("password", alePassword);
    	showHistory = sharedPreferences.getBoolean("showHistory",  false);
    }
    
    private void saveSharedPreferences(){
    	Log.i(TAG, "saving shared preferences");
    	SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
    	editor.putString("hostAddress", aleHost);
    	editor.putString("userid", aleUsername);
    	editor.putString("password", alePassword);
    	editor.putBoolean("showHistory", showHistory);
    	editor.commit();
    }
    
    
    private void initialize(){
		try{
			zmqSubscriber.interrupt();
			zmqStatusString = "ZMQ restarting with "+aleHost;
			Log.v(TAG, "interrupting ZMQ thread");
		} catch (Exception e) { Log.e(TAG, "initialize() exception interrupting ZMQ thread "+e); }
    	showAllMacs = true;
//    	targetMac = null;
    	targetHashMac = null;
    	if(showAllMacs) { pickTargetButton.setText("showing all devices"); }
    	else { pickTargetButton.setText("showing one device"); }
    	if(showHistory){ trackHistoryButton.setText("showing history"); }
    	else { trackHistoryButton.setText("not showing history"); }
		zmqMessageCounter = 0;
		zmqMessagesForMyMac = 0;
		zmqLastSeq = 0;
		zmqMissedSeq = 0;		
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		myMac = wifiInfo.getMacAddress().toUpperCase(Locale.US);
		zmqHandler = new Handler(){
	    	@Override
	    	public void handleMessage(Message msg){ zmqMessageReceived(msg); }
	    }; 
	    aleAllPositionHistoryMap = new HashMap<String, ArrayList<PositionHistoryObject>>(500);
		alePositionHistoryList = new ArrayList<PositionHistoryObject>();
        if(findMyLocationAsyncTaskInProgress == false) {
        	FindMyLocationAsyncTask findMyLocationAsyncTask = new FindMyLocationAsyncTask();
        	findMyLocationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myMac);
        }
        aleDiscoveryAsyncTaskInProgress = false;
        downloadFloorplanAsyncTaskInProgress = false;
    }
    
    public void setViewsAndListeners(){
		aleHostTextView = (TextView) findViewById(R.id.aleHostTextView);
		//aleHostTextView.setOnFocusChangeListener(configListener);
		aleHostTextView.setOnClickListener(settingsOnClickListener);
		aleUsernameTextView = (TextView) findViewById(R.id.aleUsernameTextView);
		//aleUsernameTextView.setOnFocusChangeListener(configListener);
		aleUsernameTextView.setOnClickListener(settingsOnClickListener);
		alePasswordTextView = (TextView) findViewById(R.id.alePasswordTextView);
		//alePasswordTextView.setOnFocusChangeListener(configListener);
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
		floorPlanView = (FloorPlanView) findViewById(R.id.FloorPlanView);
    }
    
    public void initializeConfigViews(){
    	aleHostTextView.setText(aleHost);
    	aleUsernameTextView.setText(aleUsername);
    	alePasswordTextView.setText("*password*");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig){
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.activity_main); 
    	setViewsAndListeners();
    	initializeConfigViews();
    	if(showHistory) { trackHistoryButton.setText("showing history"); }
    	else { trackHistoryButton.setText("not showing history"); }
    	showAllMacs = true;
    }
    
    
    @Override
    public void onStart(){
    	super.onStart();
    	Log.i(TAG, "onStart");
    	readSharedPreferences();
    	aleDiscoveryAsyncTaskInProgress = false;
    	downloadFloorplanAsyncTaskInProgress = false;
    	findMyLocationAsyncTaskInProgress = false;
    	if(floorList != null) { floorList.clear(); }
    	floorListIndex = -1;
    	initialize();
    }
    
	@Override
	public void onStop(){
		super.onStop();
		Log.i(TAG, "onStop");
		saveSharedPreferences();
		try{
			zmqSubscriber.interrupt();
			zmqStatusString = "ZMQ restarting with "+aleHost;
			Log.v(TAG, "interrupting ZMQ thread");
		} catch (Exception e) { Log.e(TAG, "onStop() exception interrupting ZMQ thread "+e); }
 
	}
}
