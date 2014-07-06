package com.arubanetworks.aledemonstrator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class FloorPlanView extends View {
	String TAG = "FloorPlanView";
	Bitmap thisFloorPlan = null;
	AleFloor thisFloor = null;
	DecimalFormat dec = new DecimalFormat("#.#");
	float scaleFactor = (float) 0.2;
	Rect frameRect = new Rect(0, 0, 1, 1);
	int rectWidthInt;
	int rectHeightInt;
	final Paint myMacPaint = new Paint();
	final Paint myAlePaint = new Paint();
	final Paint myGridPaint = new Paint();
	final Paint myMapPaint = new Paint();
	final Paint myHairlinePaint = new Paint();
	
	float site_xAle = 0;;
	float site_yAle = 0;
	boolean showHistory;
	boolean showAllMacs;
	boolean showLabels;
	String targetHashMac;
	boolean waitingToTouchTarget;
	
	float lastTouchX;
	float lastTouchY;
	float lastDownX;
	float lastDownY;
	long action_downStartTime;
	float deltaOriginX = 0;
	float deltaOriginY = 0;
	float originX = 0;
	float originY = 0;
	float viewWidth = 0;
	float viewHeight = 0;
	ScaleGestureDetector mScaleDetector;
	float mScaleFactor = 1.f;

	
	public FloorPlanView(Context context, AttributeSet attrs) { 
			super(context, attrs);
			mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev){
		mScaleDetector.onTouchEvent(ev);
		floorPlanOnTouchListener.onTouch(this, ev);			
		return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        if(MainActivity.floorPlan != null && MainActivity.floorList != null && MainActivity.floorList.size() > 0 && 
        		MainActivity.floorListIndex != -1 && MainActivity.floorList.get(MainActivity.floorListIndex) != null){        	
	        thisFloor = MainActivity.floorList.get(MainActivity.floorListIndex);
	        thisFloorPlan = MainActivity.floorPlan;
	        site_xAle = MainActivity.site_xAle;
	        site_yAle = MainActivity.site_yAle;
//	        aleAllPositionHistoryMap = MainActivity.aleAllPositionHistoryMap;
//	        alePositionHistoryList = MainActivity.alePositionHistoryList;
	        showHistory = MainActivity.showHistory;
	        showAllMacs = MainActivity.showAllMacs;
	        targetHashMac = MainActivity.targetHashMac;
	        waitingToTouchTarget = MainActivity.waitingToTouchTarget;
        	viewWidth = this.getWidth();
        	viewHeight = this.getHeight();
        	//Log.i(TAG, "onDraw starting with thisFloor.floor_id "+thisFloor.floor_id+" index "+MainActivity.floorListIndex);
        	// Log.i(TAG, "onDraw starting with origin "+originX+" "+originY+"  moved "+deltaOriginX+" "+deltaOriginY+" scaleFactor "+mScaleFactor+" canvasSize "
			//	+canvas.getWidth()+" "+canvas.getHeight()+" viewSize "+viewWidth+" "+viewHeight);

        	// Scale the canvas for pinch-zoom to mScaleFactor
        	canvas.save();
			canvas.scale(mScaleFactor, mScaleFactor);
		
			// Move the canvas
			calculateNewOrigin();
			canvas.translate(originX, originY);
			
			myMacPaint.setColor(Color.BLUE);
			myMacPaint.setStyle(Paint.Style.FILL);
			myMacPaint.setStrokeWidth(4);
			myMacPaint.setTextSize(20);
		
			myAlePaint.setColor(Color.RED);
			myAlePaint.setStyle(Paint.Style.FILL);
			myAlePaint.setStrokeWidth(4);
			myAlePaint.setTextSize(36);
			
			myGridPaint.setColor(Color.LTGRAY);
			myGridPaint.setStyle(Paint.Style.STROKE);
			myGridPaint.setStrokeWidth(1);
			
			myHairlinePaint.setColor(Color.RED);
			myHairlinePaint.setStyle(Paint.Style.STROKE);
			myHairlinePaint.setStrokeWidth(3);
		
			//Size the bitmap and build a rectangle to put it in, on the canvas
			float frameScaleFloat = scaleBitmap(canvas.getWidth(), canvas.getHeight(), thisFloorPlan.getWidth(), thisFloorPlan.getHeight());
			rectWidthInt = sizeRect(thisFloorPlan.getWidth(), frameScaleFloat);
			rectHeightInt = sizeRect(thisFloorPlan.getHeight(), frameScaleFloat);
			frameRect.set(0, 0, rectWidthInt, rectHeightInt);
			
			canvas.drawBitmap(thisFloorPlan, null, frameRect, null);
			//scaleFactor = (float) (rectHeightInt/MainActivity.siteHeight);
			scaleFactor = (float) (rectHeightInt/thisFloor.floor_img_length);
			
			// if we have a grid size, draw a grid superimposed and put a legend at upper left
			if(MainActivity.trackMode != MainActivity.MODE_TRACK && thisFloor.grid_size > 0){
				for(float i=0; i<thisFloor.floor_img_width; i = (i+thisFloor.grid_size)){
					canvas.drawLine(i*scaleFactor, 0, i*scaleFactor, rectHeightInt, myGridPaint);
				}
				for(float i=0; i<thisFloor.floor_img_length; i = (i+thisFloor.grid_size)){
					canvas.drawLine(0, i*scaleFactor, rectWidthInt, i*scaleFactor, myGridPaint);
				}
				canvas.drawText("grid "+thisFloor.grid_size+" "+thisFloor.units, 20, -10, myAlePaint);
			}
			
			// if we have a fingerprint map, print it as a colour overlay with a bit of transparency
			if(MainActivity.trackMode != MainActivity.MODE_TRACK && thisFloor.grid_size > 0 && thisFloor.fingerprintMapList != null && thisFloor.fingerprintMapList.size() > 0){
				for (int i=0; i<thisFloor.fingerprintMapList.size(); i++){
					int col = thisFloor.fingerprintMapList.get(i).satisfactory;
					if(col > 0){
						if(col > 4) { myMacPaint.setColor(Color.GREEN); }
						else if (col > 2) { myMacPaint.setColor(Color.YELLOW); }
						else if (col > 0) { myMacPaint.setColor(Color.RED); }
						myMacPaint.setAlpha(100);
						float x = thisFloor.fingerprintMapList.get(i).locationX - (thisFloor.fingerprintMapList.get(i).locationX % thisFloor.grid_size);
						float y = thisFloor.fingerprintMapList.get(i).locationY - (thisFloor.fingerprintMapList.get(i).locationY % thisFloor.grid_size);
						//Log.v(TAG, "grid survey point "+thisFloor.grid_size+"  "+thisFloor.fingerprintMapList.get(i).locationX+" "+x+"  "+thisFloor.fingerprintMapList.get(i).locationY+"  "+y);
						//myMacPaint.setARGB(100, 153, 255, 153);
						canvas.drawRect(x *scaleFactor, y * scaleFactor, (x+thisFloor.grid_size) * scaleFactor, (y+thisFloor.grid_size) * scaleFactor, myMacPaint);
					}
				}
				myMacPaint.setColor(Color.BLUE);
				//myMacPaint.setAlpha(255);
			}
			
			// paint the MAC address just off the floorplan
			if (MainActivity.trackMode == MainActivity.MODE_TRACK && MainActivity.myMac != null) {
				canvas.drawText("MAC "+MainActivity.myMac, 20, -10, myAlePaint);
			}
			
			// if we are in survey fingerprint mode, put up the gridlines and update the central x,y coordinates
			if(MainActivity.trackMode != MainActivity.MODE_TRACK){
				canvas.drawLine((viewWidth/mScaleFactor)/2-originX, -originY, (viewWidth/mScaleFactor)/2-originX, (viewHeight/mScaleFactor)-originY, myHairlinePaint);
				canvas.drawLine(-originX, (viewHeight/mScaleFactor)/2-originY, (viewWidth/mScaleFactor)-originX, (viewHeight/mScaleFactor/2)-originY, myHairlinePaint);
				MainActivity.surveyPointX = (-originX+(viewWidth/mScaleFactor/2))/scaleFactor;
				MainActivity.surveyPointY = (-originY+(viewHeight/mScaleFactor/2))/scaleFactor;
				//canvas.drawRect((viewWidth/mScaleFactor)/2-originX-1, -originY, (viewWidth/mScaleFactor)/2-originX+1, (viewHeight/mScaleFactor)-originY, myAlePaint);
				//canvas.drawLine(-originX, (viewHeight/mScaleFactor)/2-originY-1, (viewWidth/mScaleFactor)-originX, (viewHeight/mScaleFactor/2)-originY+1, myAlePaint);
			}
			
			// Draw position and historical track of ALL ALE Positions if item enabled
			if(MainActivity.trackMode == MainActivity.MODE_TRACK && showAllMacs){
				// iterate over all the MACs we've tracked
				for(Map.Entry<String, ArrayList<PositionHistoryObject>> entry : MainActivity.aleAllPositionHistoryMap.entrySet()){
					// draw lines joining past positions for this MAC
					for ( int i = 0; i < entry.getValue().size() -1 ; i++){
						if(showHistory && entry.getValue().get(i).floorId.equals(thisFloor.floor_id) && entry.getValue().get(i).measuredX > 1){
							canvas.drawLine(entry.getValue().get(i).measuredX * scaleFactor, 
									entry.getValue().get(i).measuredY * scaleFactor,
									entry.getValue().get(i+1).measuredX * scaleFactor,
									entry.getValue().get(i+1).measuredY * scaleFactor,
									myAlePaint);
						}
						// if it's the latest entry, draw a square for the current position
						if(i == (entry.getValue().size()-2) && entry.getValue().get(i+1).floorId.equals(thisFloor.floor_id)){
							canvas.drawRect(entry.getValue().get(i+1).measuredX*scaleFactor -8, entry.getValue().get(i+1).measuredY*scaleFactor -8, 
									entry.getValue().get(i+1).measuredX*scaleFactor+8, entry.getValue().get(i+1).measuredY*scaleFactor+8, myAlePaint);
							// Draw label if enabled
							//if(showLabels){
							//	canvas.drawText(entry.getValue().get(i).ethAddr, entry.getValue().get(i+1).measuredX*scaleFactor -20, entry.getValue().get(i+1).measuredY*scaleFactor -10, myAlePaint);
							//}
						}
					}
				}
			}
			
			// Draw position and historical track of target MAC Position if target hash MAC not null
			if(MainActivity.trackMode == MainActivity.MODE_TRACK && targetHashMac != null){
				// iterate over all the MACs we've tracked to find the entry (hash mac) for ours
				ArrayList<PositionHistoryObject> historyList = MainActivity.aleAllPositionHistoryMap.get(targetHashMac);
				// draw lines joining past positions for this MAC
				if(historyList != null && historyList.size() > 0){
					for ( int i = 0; i < historyList.size(); i++){
						
						// draw lines
						if(i>0) {
							if(showHistory && historyList.get(i).floorId.equals(thisFloor.floor_id) && historyList.get(i).measuredX > 1){
								canvas.drawLine(historyList.get(i).measuredX * scaleFactor, 
										historyList.get(i).measuredY * scaleFactor,
										historyList.get(i-1).measuredX * scaleFactor,
										historyList.get(i-1).measuredY * scaleFactor,
										myAlePaint);
							}
						}
						
						// if it's the latest entry, draw a square for the current position
						if(i == historyList.size()-1 && historyList.get(i).floorId.equals(thisFloor.floor_id)){
							canvas.drawRect(historyList.get(i).measuredX*scaleFactor -8, historyList.get(i).measuredY*scaleFactor -8, 
									historyList.get(i).measuredX*scaleFactor+8, historyList.get(i).measuredY*scaleFactor+8, myAlePaint);
						}
						
					}
				}
			}
			
			// Draw my ALE position with a rect icon, provided we are on this floor
			if(MainActivity.trackMode != MainActivity.MODE_SURVEY && MainActivity.myFloorId != null && MainActivity.myFloorId.equals(thisFloor.floor_id)){
				canvas.drawRect(site_xAle*scaleFactor -8, site_yAle*scaleFactor -8, 
							(site_xAle*scaleFactor)+8, (site_yAle*scaleFactor)+8, myMacPaint);				
				// Draw label if enabled
				//if(showLabels){
				//	canvas.drawText(MainActivity.myMac, site_xAle*scaleFactor -20, site_yAle*scaleFactor -10, myMacPaint);
				//}
					
				// Draw my historical track of ALE Position if menu item enabled
				if(MainActivity.alePositionHistoryList.size() > 1 && showHistory  ){
					for ( int i = 0; i < MainActivity.alePositionHistoryList.size() - 1; i++){
						// check whether the object is on this floor or another
						if(MainActivity.alePositionHistoryList.get(i).measuredX > 1){
							canvas.drawLine(MainActivity.alePositionHistoryList.get(i).measuredX * scaleFactor, 
									MainActivity.alePositionHistoryList.get(i).measuredY * scaleFactor,
									MainActivity.alePositionHistoryList.get(i+1).measuredX * scaleFactor,
									MainActivity.alePositionHistoryList.get(i+1).measuredY * scaleFactor,
									myMacPaint);
						}
					}
				}
			}
			
			// If in survey mode, draw breadcrumbs for survey points in this run
			if(MainActivity.trackMode == MainActivity.MODE_SURVEY && MainActivity.floorList != null && MainActivity.floorListIndex != -1 && MainActivity.surveyHistoryList.size() > 0){
				for(int i=0; i<MainActivity.surveyHistoryList.size(); i++){
					if(MainActivity.surveyHistoryList.get(i).floorId.equals(MainActivity.floorList.get(MainActivity.floorListIndex).floor_id)){
						canvas.drawRect(MainActivity.surveyHistoryList.get(i).touchX*scaleFactor -8,  MainActivity.surveyHistoryList.get(i).touchY*scaleFactor -8,
								MainActivity.surveyHistoryList.get(i).touchX*scaleFactor +8, MainActivity.surveyHistoryList.get(i).touchY*scaleFactor +8, myMacPaint);
					}
				}
			}
			
		} else { 
			// if the bitmap has been reset to null, we just paint the canvas black because the floorplan is invalid
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		} 
		canvas.restore();
	}
	
	private float scaleBitmap(int winWidthSb, int winHeightSb, float bitWidthSb, float bitHeightSb){
		float widthScaleSbFloat = (winWidthSb / bitWidthSb);
		float heightScaleSbFloat = (winHeightSb / bitHeightSb);
		if(widthScaleSbFloat <= heightScaleSbFloat){return widthScaleSbFloat;}
		else {return heightScaleSbFloat;}
	}
	
	private int sizeRect(float bitmapSizeRect, float frameScaleSizeRect) {
		float newDimSizeRectFloat = bitmapSizeRect * frameScaleSizeRect;
		int newDimSizeRectInt = (int) newDimSizeRectFloat;
		return newDimSizeRectInt;
	}


	private void calculateNewOrigin(){
		if(deltaOriginX != 0 || deltaOriginY != 0){
			originX = originX + deltaOriginX;
			deltaOriginX = 0;
			if(originX + rectWidthInt < 50) originX = 50 - rectWidthInt;
			if(viewWidth/mScaleFactor - originX < 50) originX = viewWidth/mScaleFactor - 50;
			originY = originY + deltaOriginY;
			deltaOriginY = 0;
			if(originY + rectHeightInt < 100) originY = 100 - rectHeightInt;
			if(viewHeight/mScaleFactor - originY < 50) originY = viewHeight/mScaleFactor - 50;
		}
	}
	
	
	private OnTouchListener floorPlanOnTouchListener = new OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			final int action = event.getAction();
			switch (action){
				case MotionEvent.ACTION_DOWN:{
					action_downStartTime = System.currentTimeMillis();
					lastDownX = event.getX();
					lastDownY = event.getY();
					lastTouchX = lastDownX;
					lastTouchY = lastDownY;
					matchTouchToTarget(); 				
				}
				case MotionEvent.ACTION_MOVE:{
					float x = event.getX();
					float y = event.getY();
					deltaOriginX = (x - lastTouchX)/mScaleFactor;
					deltaOriginY = (y - lastTouchY)/mScaleFactor;
					lastTouchX = x;
					lastTouchY = y;
					break;
				}
				case MotionEvent.ACTION_UP:{
					long timeDiff = System.currentTimeMillis() - action_downStartTime;
				}
			}
			invalidate();
			return false;
		}
	};
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
		@Override
		public boolean onScale(ScaleGestureDetector detector){
			mScaleFactor *= detector.getScaleFactor();
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
			invalidate();
			return true;
		}
	}
	
	private void matchTouchToTarget(){
		
		if(MainActivity.trackMode == MainActivity.MODE_TRACK){
			// iterate over all the MACs we've tracked
			for(Map.Entry<String, ArrayList<PositionHistoryObject>> entry : MainActivity.aleAllPositionHistoryMap.entrySet()){			
				// we just look at the last entry, for the most recent position
				int i = entry.getValue().size() - 1;
				// have to check whether the entry is on the same floor
				try {
					if(thisFloor.floor_id.equals(entry.getValue().get(i).floorId)){
						float distX = Math.abs(entry.getValue().get(i).measuredX*scaleFactor - (lastDownX/mScaleFactor - originX));
						float distY = Math.abs(entry.getValue().get(i).measuredY*scaleFactor - (lastDownY/mScaleFactor - originY));
						if(distX < rectWidthInt/50 && distY < rectHeightInt/50){
							Log.i(TAG, "matchTouchToTarget "+entry.getKey()+" distX_"+distX+" distY_"+distY+" rectWidthInt_"+(rectWidthInt/50)+" rectHeightInt_"+(rectHeightInt/50));
							if(waitingToTouchTarget){ 
								MainActivity.targetHashMac = entry.getKey();
								Log.v(TAG, "set targetHashMac "+entry.getKey());
								MainActivity.showAllMacs = false;
								MainActivity.waitingToTouchTarget = false;
								MainActivity.pickTargetButtonText = "showing "+entry.getKey();
								MainActivity.pickTargetButton.setText(MainActivity.pickTargetButtonText);
								break;
							}
							Log.v(TAG, "matchTouchToTarget "+entry.getKey());
							launchTargetDialog(entry.getKey().toString(), entry.getValue().get(entry.getValue().size()-1).measuredX, entry.getValue().get(entry.getValue().size()-1).measuredY);
						}		
					}
				} catch (Exception e) { Log.e(TAG, "Exception in match touch to target "+e); }
			}
		} 
		
		else if (MainActivity.trackMode == MainActivity.MODE_SURVEY) {
			// we want to match the touch to a survey point
			List<PositionHistoryObject> surveyList = new ArrayList<PositionHistoryObject>(MainActivity.surveyHistoryList);
				for(int i=0; i<surveyList.size(); i++){
				// have to check whether the entry is on the same floor
				try {
					if(thisFloor.floor_id.equals(surveyList.get(i).floorId)){
						float distX = Math.abs(surveyList.get(i).touchX*scaleFactor - (lastDownX/mScaleFactor - originX));
						float distY = Math.abs(surveyList.get(i).touchY*scaleFactor - (lastDownY/mScaleFactor - originY));
						if(distX < rectWidthInt/50 && distY < rectHeightInt/50){
							Log.i(TAG, "matchTouchToTarget "+i+" distX_"+distX+" distY_"+distY+" rectWidthInt_"+(rectWidthInt/50)+" rectHeightInt_"+(rectHeightInt/50));
							launchSurveyTouchDialog(i, surveyList.get(i));
							break;
						}		
					}
				} catch (Exception e) { Log.e(TAG, "Exception in match touch to target "+e); }
			}
		}
		
	}
	
	private void launchTargetDialog(String hashed_sta_eth_mac, float x, float y){
		List<String> list = MainActivity.eventLogMap.get(hashed_sta_eth_mac);
		CharSequence[] text = list.toArray(new String[0]);
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
		Log.v(TAG, " "+"Events for target at "+x+"  "+y+"   MAC\n"+hashed_sta_eth_mac+" ");
		builder.setTitle("Events for target at "+String.format("%.2f", x)+"  "+String.format("%.2f", y)+"  MAC\n"+hashed_sta_eth_mac+" ");
		builder.setItems(text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {	
			}
		});
	builder.show();
	}
	
	private void launchSurveyTouchDialog(int index, final PositionHistoryObject pho){
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
		Log.v(TAG, "Launching survey touch dialog x "+String.format("%.2f", pho.touchX)+" y "+String.format("%.2f", pho.touchY));
		builder.setTitle("Survey Point "+index+"f x "+String.format("%.2f", pho.touchX)+"  "+String.format("%.2f", pho.touchY));
		CharSequence[] targetList = {"delete survey point", "keep survey point"};
		builder.setItems(targetList, new DialogInterface.OnClickListener() {		
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){
					MainActivity.deleteSurveyPointFromAle(pho);
					Log.v(TAG, "deleting survey point ");
					return;
				}
			}
		});
	builder.show();
	}
	
	public void initialize(){
		Log.v(TAG, "floorPlanView initialize");
		thisFloorPlan = null;
		thisFloor = null;
		scaleFactor = (float) 0.2;
		frameRect = new Rect(0, 0, 1, 1);
		deltaOriginX = 0;
		deltaOriginY = 0;
		originX = 0;
		originY = 0;
		viewWidth = 0;
		viewHeight = 0;
		mScaleFactor = 1.f;
	}


	
}	//End of FloorPlanView


