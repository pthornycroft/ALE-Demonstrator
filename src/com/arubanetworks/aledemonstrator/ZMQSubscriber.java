package com.arubanetworks.aledemonstrator;

import org.jeromq.*;
import org.jeromq.ZMQ.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//public class ZMQSubscriber implements Runnable {
public class ZMQSubscriber extends Thread {
	private String TAG = "ZMQSubscriberThread";
    private final Handler uiThreadHandler;
    private String[] zmqFilter;
    final ZContext zContext = new ZContext();
    ZMQ.Socket socket;
    
    public ZMQSubscriber(Handler uiThreadHandler, String[] zmqFilter) { 
    	this.uiThreadHandler = uiThreadHandler; 
    	this.zmqFilter = zmqFilter;
    }
 
    @SuppressWarnings("deprecation")
	@Override
    public void run() {
    	try{
    		String progress = " ";
    		if(!testReachabilityOfServer()) { progress = MainActivity.aleHost+" unreachable"; }
    		else { progress = MainActivity.aleHost+" opened socket"; }
			Log.v(TAG, "ZMQ server "+MainActivity.aleHost+"  "+progress);
    		sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, progress.getBytes("UTF-8"));  		
    		
    		String connectString = "tcp://"+MainActivity.aleHost+":7779";
    		Context context = ZMQ.context(1);
    		socket = zContext.createSocket(ZMQ.SUB);
			
			boolean success = socket.connect(connectString);
			
			String filterString = "";
			for(int i=0; i<zmqFilter.length; i++){
				socket.subscribe(zmqFilter[i]);
				filterString = filterString + zmqFilter[i]+" ";
			}

			if(success){
				Log.v(TAG, "connected "+connectString+"  filters "+filterString);
				sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, ("connected "+connectString+"\nfilters "+filterString).getBytes("UTF-8")); 
			}
			
			byte[] msg = null;
			
	        while(!Thread.currentThread().isInterrupted()) {
        		String topic = new String(socket.recv(0));
        		msg = socket.recv(0);
        		while(socket.hasReceiveMore()){
        			Log.v(TAG, "multi-part zmq message ");
        			byte[] moreBytes = socket.recv(0);
        			byteConcat(msg, moreBytes);
	        	}
        		sendMessage(topic, msg);
	        }
	        socket.close();
	        context.term();
	        
    	} catch (ZMQException e)    { 
    		Log.e(TAG, "ZMQException "+e);
    		sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, e.toString().getBytes());
    	} catch (Exception e) { 
    		Log.e(TAG, "Exception "+e); 
    		sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, e.toString().getBytes());
    	}
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
    	try {
    	    SocketAddress sockaddr = new InetSocketAddress(MainActivity.aleHost, 7779);
    	    Log.i(TAG, "testing reachability to "+sockaddr.toString());
    	    java.net.Socket sock = new java.net.Socket();
    	    int timeoutMs = 2000;   // 2 seconds
    	    sock.connect(sockaddr, timeoutMs);
    	    success = true;
    	    Log.i(TAG, "successful reachability to "+sockaddr.toString());
    	}catch(Exception e){
    		Log.e(TAG, MainActivity.aleHost+" address not reachable after 2sec "+e);
    	}
    	return success;
    }
    
    @Override
    public void interrupt(){
    	try{
    		socket.close();
    	} finally{
    		String s = "Closed socket";
    		sendMessage(MainActivity.ZMQ_PROGRESS_MESSAGE, s.getBytes());
    		super.interrupt();
    	}
    }


}