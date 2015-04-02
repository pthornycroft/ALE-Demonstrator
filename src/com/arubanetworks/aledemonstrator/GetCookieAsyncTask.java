package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

// cookie login Feb15
public class GetCookieAsyncTask extends AsyncTask <String, Integer, Boolean> {
		static String TAG = "GetCookieAsyncTask";
		static String COOKIE_HEADER = "Set-Cookie";
		int TIMEOUT_VALUE = 20000;
		
		protected Boolean doInBackground(String... params) {
			Boolean result = false;
			String args = "/api/j_spring_security_check";

			try{
				//CustomVerifier customVerifier = new CustomVerifier();
				//customVerifier.trustAllHosts();
				URL url = new URL("https://"+MainActivity.aleHost+":"+MainActivity.alePort+args);
				String postBody =  "j_username="+MainActivity.aleUsername+"&j_password="+MainActivity.alePassword;
				Log.v(TAG, "login URL get protocol "+url.getProtocol()+" host "+url.getHost()+" port "+url.getPort()+" file "+url.getFile());
				HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setHostnameVerifier(MainActivity.customVerifier);
				connection.setConnectTimeout(TIMEOUT_VALUE);
				connection.setReadTimeout(TIMEOUT_VALUE);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 4.3; Nexus 7 Build/JWR66D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.111 Safari/537.36");
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setInstanceFollowRedirects(false);

				// prints the outgoing headers for troubleshooting
				Map<String, List<String>> outMap = connection.getRequestProperties();
				for(Entry<String, List<String>> entry : outMap.entrySet()) { 
					for(int i=0; i<entry.getValue().size(); i++){
						Log.v(TAG, "login transmitted headers "+entry.getKey()+"  "+entry.getValue().get(i));
					}
				}
							
				connection.connect();
				
				try {
					DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
					dataOutputStream.writeBytes( postBody );    
					dataOutputStream.flush();
					dataOutputStream.close();
				} catch (Exception e) { 
					Log.e(TAG, "login Exception writing output stream "+e); 
					MainActivity.httpStatusString1 = e.toString();
				}
				
				Log.v(TAG, "login responseCode "+connection.getResponseCode()+"  responseMessage "+connection.getResponseMessage()+"  location "+connection.getHeaderField("Location"));
				
				// prints the incoming headers for troubleshooting
				/*Map<String, List<String>> inMap = connection.getHeaderFields();
				for(Entry<String, List<String>> entry : inMap.entrySet()) { 
					for(int i=0; i<entry.getValue().size(); i++){
						Log.v(TAG, "login received headers "+entry.getKey()+"  "+entry.getValue().get(i));
					}	
				} */
				
				long expires = checkCookies(connection.getHeaderFields(), TAG);
				if(expires > 0) { MainActivity.cookieExpires = expires; }
				
				try{
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
					StringBuilder builder = new StringBuilder();
					String line;
					while ((line = in.readLine()) != null) {
						builder.append(line);	                
					}
					String outputString = builder.toString();
					Log.v(TAG, "login response body "+outputString);
				} catch (Exception e) {
					Log.d(TAG, "login there wasn't any returned body");
				} finally {
					connection.disconnect();
				}
				
			} catch (Exception e) { 
				Log.e(TAG, "login exception "+e);
				MainActivity.httpStatusString1 = "login exception";
				MainActivity.httpStatusString2 = e.toString();
			}
			
			MainActivity.printCookies();
		
			return result;
		}
		
		public void onPreExecute(){
			Log.i(TAG, "getCookieAsyncTask login starting");
			//MainActivity.httpStatusString1 = "attempting login for cookie";
			MainActivity.getCookieAsyncTaskInProgress = true;
		}
		
		public void onPostExecute(Boolean result){
			Log.i(TAG, "getCookieAsyncTask login finished");
			//MainActivity.httpStatusString1 = "login attempt finished";
			MainActivity.getCookieAsyncTaskInProgress = false;
		}
		
		public static long checkCookies(Map<String, List<String>> inMap, String origin) {
			long expires = 0;
			try {
				for(Entry<String, List<String>> entry : inMap.entrySet()) { 
					for(int i=0; i<entry.getValue().size(); i++){
						//Log.v(TAG, origin+" Header "+entry.getKey()+"  "+entry.getValue().get(i));
						if(entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) {
							Log.d(TAG, origin+" Cookie "+entry.getKey()+" i "+i+"  "+entry.getValue().get(i));
							// calculate when the cookie will expire so we will know to get another one
							String maxAge = entry.getValue().get(i);
							maxAge = entry.getValue().get(i).substring((entry.getValue().get(i).indexOf("Max-Age=") + 8), entry.getValue().get(i).length());
							maxAge = maxAge.substring(0, maxAge.indexOf(";"));
							int maxAgeInt = Integer.parseInt(maxAge);
							expires = System.currentTimeMillis() + (maxAgeInt * 1000);
							Log.d(TAG, "cookie expires "+expires+"  time now "+System.currentTimeMillis()+"  diff "+(expires - System.currentTimeMillis()));
						}
					}
				}
			} catch (Exception e) { Log.e(TAG, "Exception checking cookie expiry "+e); }
			return expires;
		}

		
}
