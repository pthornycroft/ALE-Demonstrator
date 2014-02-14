package com.arubanetworks.aledemonstrator;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class DownloadFloorplanAsyncTask extends AsyncTask <String, Integer, Bitmap> {
	static String TAG = "DownloadFloorplanAsyncTask";
	
	protected Bitmap doInBackground(String... params) {
		
		Bitmap result = downloadFloorplanJpg("http://"+MainActivity.aleHost+params[0]);
		
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "DownloadFloorplanAsyncTask starting");
		MainActivity.downloadFloorplanAsyncTaskInProgress = true;
		MainActivity.httpStatusString = "Starting floorplan image download";
	}
	
	public void onPostExecute(Bitmap result){
		Log.i(TAG, "DownloadFloorplanAsyncTask finished");
		MainActivity.downloadFloorplanAsyncTaskInProgress = false;
		if(result != null){
			MainActivity.floorPlan = result;
			MainActivity.httpStatusString = "Successful floorplan image download";
		}
		else { MainActivity.httpStatusString = "Unsuccessful floorplan image download"; }
	}


  	//Create a URL and open the connection to get the floorplan bitmap
  	private Bitmap downloadFloorplanJpg(String url){
  		Bitmap result = null;
		URL urlFloorPlan = null;
		try {  
			urlFloorPlan = new URL(url);
		} catch (MalformedURLException e) { Log.e(TAG, "  downloadFloorplanJpg exception malformed URL for urlFloorPlan " + e); }
		
		try {
			result = getURLImage(urlFloorPlan);				
		} catch (Exception e) { Log.e(TAG, "  extractFloorPlan exception in getURLImage(urlFloorPlan)"+e); }
		
		return result;
  	}
	  	
	
	private Bitmap getURLImage (URL imageURL){  
		Bitmap bmImage = null;
			
		try{
			HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
			connection.setRequestProperty("Authorization", "Basic "+Base64.encodeToString((MainActivity.aleUsername+":"+MainActivity.alePassword).getBytes(), Base64.NO_WRAP));
			if(connection.getResponseCode() != 200) { Log.e(TAG, "Exception opening connection "+connection.getResponseCode()+"  "+connection.getResponseMessage()); }
			// prints the outgoing headers for troubleshooting
			/*Map<String, List<String>> mapOut = connection.getRequestProperties();
			for(Entry<String, List<String>> entry : mapOut.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "outgoing http image header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			} */
				
			// prints the incoming headers for troubleshooting
			/*Map<String, List<String>> mapIn = connection.getHeaderFields();
			for(Entry<String, List<String>> entry : mapIn.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "incoming http image header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			}	*/			
			
			InputStream is = connection.getInputStream();
			BufferedInputStream insImageURL = new BufferedInputStream(is, 8196);
			Log.v(TAG, "http "+imageURL.toString()+" result code was "+connection.getResponseCode()+"  response message "+connection.getResponseMessage());				
			// test the size of the image to see if we need to compress it
			final BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inPurgeable = true;
			bmOptions.inInputShareable = true;
			bmOptions.inJustDecodeBounds = true;			
			BitmapFactory.decodeStream(insImageURL, null, bmOptions);
			Log.v(TAG, "original image dimensions "+bmOptions.outWidth+"  "+bmOptions.outHeight);
			bmOptions.inSampleSize = calculateInSampleSize(bmOptions, 1024, 1024);
			bmOptions.inJustDecodeBounds = false;
			insImageURL.close();
			
			// now we have to start a new connection to get a new input stream, and use the inSampleSize compression factor (factor 4 means 25%)
			connection = (HttpURLConnection) imageURL.openConnection();
			connection.setRequestProperty("Authorization", "Basic "+Base64.encodeToString((MainActivity.aleUsername+":"+MainActivity.alePassword).getBytes(), Base64.NO_WRAP));
			is = connection.getInputStream();
			insImageURL = new BufferedInputStream(is, 8196);
			bmImage = BitmapFactory.decodeStream(insImageURL, null, bmOptions);  // this is where it crashed out of memory sometimes with detailed floorplan
			insImageURL.close();
			int bmWd = bmImage.getWidth();
			int bmHt = bmImage.getHeight();
			bmImage = Bitmap.createScaledBitmap(bmImage, bmWd/1, bmHt/1, false);
			Log.v(TAG, "scaled image dimensions "+bmWd+"  "+bmHt);
		} catch (Exception e) { 
			Log.e(TAG, "Exception in getURLImage " + MainActivity.aleUsername + "  " + MainActivity.alePassword + "  url was " + imageURL.toString() + "\n" + e); 
		}
		return bmImage;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }
    Log.v(TAG, "calculate sample size "+inSampleSize);
    return inSampleSize;
}

	
	
}

