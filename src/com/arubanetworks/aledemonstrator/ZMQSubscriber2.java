package com.arubanetworks.aledemonstrator;
/*
 * 
 * This class is for tinkering with the ZMQ client... still some problems there
 * one... the broken pipe crash
 * 
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jeromq.ZContext;
import org.jeromq.ZMQ;
import org.jeromq.ZMQException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

// following the example at https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/interrupt.java
// referenced here https://github.com/zeromq/jeromq/issues/116
public class ZMQSubscriber2 extends Thread {
	private String TAG = "ZMQSubscriber2Thread";
	private final Handler uiThreadHandler;
	private String[] zmqFilter;
	ZContext context = new ZContext();
///	ZMQ.Socket socket = context.socket(ZMQ.REP);
	ZMQ.Socket socket = context.createSocket(ZMQ.SUB);
	
	public ZMQSubscriber2(Handler uiThreadHandler, String[] zmqFilter) {
		this.uiThreadHandler = uiThreadHandler;
		this.zmqFilter = zmqFilter;
	}

	public void run(){
		try{
			String progress = " ";
			
			// test reachability
//			if(!testReachabilityOfServer()) { progress = MainActivity.aleHost+" unreachable"; }
//    		else { progress = MainActivity.aleHost+" opened socket"; }	
//			Log.v(TAG, "ZMQ server "+MainActivity.aleHost+" "+progress);
//	   		sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, progress.getBytes("UTF-8"));

			String connectString = "tcp://"+MainActivity.aleHost+":7779";
			try{
				socket.connect(connectString);
			} catch (Exception e1) { Log.e(TAG, "Exception 2 "+e1); }

			String filterString = "";
			for(int i=0; i<zmqFilter.length; i++){
				try{
					socket.subscribe(zmqFilter[i].getBytes());
				} catch (Exception e2) { Log.e(TAG, "Exception 3 "+e2); }
				filterString = filterString + zmqFilter[i]+" ";
			}		

			byte[] msg = null;

			while (!Thread.currentThread().isInterrupted()){

				Log.i(TAG, "starting loop ");
				String topic = new String(socket.recv(0));
				msg = socket.recv(0);
				while(socket.hasReceiveMore()){
					Log.v(TAG, "multi-part zmq message ");
					byte[] moreBytes = socket.recv(0);
					byteConcat(msg, moreBytes);
				}
				sendMessage(topic, msg);

			}
			
			Log.v(TAG, "8");  // this never seems to trigger.  When it does, GC problems
			socket.close();
			Log.v(TAG, "9");
			context.destroy();
					
		} catch (Exception e) { Log.e(TAG, "Exception 5 "+e); }
		
	}
	

    @Override
    public void interrupt(){
		for(int i=0; i<zmqFilter.length; i++){
			Log.v(TAG, "unsubscribing "+i);
			socket.unsubscribe(zmqFilter[i]);
		}
    	try{
    		Log.v(TAG, "10");
//    		socket.close();  // if this is in, there's endless GC activity
    	} catch (Exception e) { Log.e(TAG, "Exception 10 closing socket "+e); } 
    	try{
    		Log.v(TAG, "11");
    		//this.interrupt();  // this causes loops, obviously
    	} catch (Exception e) { Log.e(TAG, "Exception 11 interrupt zmqSubscriber "+e); }
    	try{
    		Log.v(TAG, "12");
//    		context.destroy();  // doesn't seem to add anything
    	} catch (Exception e) { Log.e(TAG, "Exception 12 context term zmqSubscriber "+e); }
    	Log.w(TAG, "destroyed zmqsubscriber");
    	super.interrupt();   	
    }
    
    private byte[] byteConcat(byte[] a, byte[] b){
    	byte[] result = new byte[a.length + b.length];
    	System.arraycopy(a,  0,  result,  0,  a.length);
    	System.arraycopy(b,  0,  result,  a.length,  b.length);
    	return result;
    }	
    
    private void sendMessage(String key, byte[] content){
		Bundle bundle = new Bundle();
		bundle.putByteArray(key, content);
		Message message = Message.obtain(uiThreadHandler);
		message.setData(bundle);
		uiThreadHandler.sendMessage(message);
    }  
    
    private boolean testReachabilityOfServer(){
    	boolean success = false;
	    java.net.Socket sock = new java.net.Socket();
    	try {
    	    SocketAddress sockaddr = new InetSocketAddress(MainActivity.aleHost, 7779);
    	    Log.i(TAG, "testing reachability to "+sockaddr.toString());
    	    int timeoutMs = 2000;   // 2 seconds
    	    sock.connect(sockaddr, timeoutMs);
    	    success = true;
    	    Log.i(TAG, "successful reachability to "+sockaddr.toString());
    	    sock.close();
    	}catch(Exception e){
    		Log.e(TAG, MainActivity.aleHost+" address not reachable after 2sec "+e);
    	}
    	return success;
    }



}
