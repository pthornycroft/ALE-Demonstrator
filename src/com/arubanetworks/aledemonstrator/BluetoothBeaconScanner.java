package com.arubanetworks.aledemonstrator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;


public class BluetoothBeaconScanner {
    private String TAG = "BluetoothBeaconScanner";
//    BluetoothManager bluetoothManager;
//	BluetoothAdapter bluetoothAdapter;
	private Handler bluetoothScanIntervalHandler = new Handler();
	private int SCAN_INTERVAL = 60000; ///
	private Handler bluetoothScanDurationHandler = new Handler();
	private int SCAN_DURATION = 1000;
	public BluetoothAdapter.LeScanCallback bluetoothScanCallback;
	static final int REQUEST_ENABLE_BT = 1;

    
	Runnable bluetoothScanRunnable = new Runnable(){
		public void run(){
//			if(bluetoothManager == null) { 	bluetoothManager = (BluetoothManager) MainActivity.context.getSystemService(Context.BLUETOOTH_SERVICE); }
//			if(bluetoothAdapter == null) { bluetoothAdapter = bluetoothManager.getAdapter(); }
			if(bluetoothScanCallback == null){
				bluetoothScanCallback = new BluetoothAdapter.LeScanCallback() {
					@Override
					public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
						//Log.v(TAG, "bluetooth LeScanCallback device "+device+" rssi "+rssi+"  scan record length "+scanRecord.length);
						BluetoothBeacon beacon = BluetoothBeacon.fromScanData(scanRecord, rssi);
						if(beacon != null) {
							MainActivity.iBeaconsTotal++;
							if (MainActivity.bleBeaconResults.indexOf(beacon.getBid()) == -1) { 
								MainActivity.iBeaconsUnique++;
								MainActivity.bleBeaconResults += "\n   "+device.getAddress()+"  iBeacon UUID_"+beacon.getProximityUuid()+
										"  Major_"+beacon.getMajor()+"  Minor_"+beacon.getMinor()+"  measured power_"+beacon.getMeasuredPwr()+"  RSSI_"+beacon.getRssi();
								MainActivity.iBeaconJsonArray.put(JsonBuilders.formIBeaconJsonObject(device.getAddress(), beacon.getProximityUuid(), 
										beacon.getMajor(), beacon.getMinor(), beacon.getMeasuredPwr(), beacon.getRssi()));
							}
						} else {
					//	bleBeaconResults += "\n\n   Device_"+device.getAddress()+"  Name_"+device.getName()+"  Type_"+device.getType()+"  RSSI_"+rssi;
					//	bleBeaconResults += "  Class_"+device.getBluetoothClass().getMajorDeviceClass()+"  services "+hasBluetoothService(device)+" bond state "+device.getBondState();
						}
					}
				};
			}
			
			if(MainActivity.bluetoothEnabled == true && MainActivity.trackMode == false) {
				scanLeDevice(true);
			}
			bluetoothScanIntervalHandler.postDelayed(bluetoothScanRunnable, SCAN_INTERVAL);
		}
	};
	
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			bluetoothScanDurationHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.v(TAG, "bluetooth runnable stop leScan");
					MainActivity.bluetoothAdapter.stopLeScan(bluetoothScanCallback);
					if(MainActivity.bleBeaconResults != " "){
						Log.i(TAG, "iBeacons "+MainActivity.bleBeaconResults);
						Log.i(TAG, "iBeacons total "+MainActivity.iBeaconsTotal+"  unique "+MainActivity.iBeaconsUnique);
						MainActivity.bleBeaconResults = new String(" ");
						MainActivity.iBeaconJsonArray = null;
						MainActivity.iBeaconsTotal = 0;
						MainActivity.iBeaconsUnique = 0;
					}
				}
			}, SCAN_DURATION);
	
		Log.v(TAG, "bluetooth runnable start leScan");
		MainActivity.bluetoothAdapter.startLeScan(bluetoothScanCallback);
		} else {
			Log.w(TAG, "bluetooth scanLeDevice started with enable false "+enable);
			MainActivity.bluetoothAdapter.stopLeScan(bluetoothScanCallback);
		}
	}
	

	public String hasBluetoothService(BluetoothDevice device){
		String result =" _";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.AUDIO)) result += "A";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.CAPTURE)) result += "C";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.INFORMATION)) result += "I";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.LIMITED_DISCOVERABILITY)) result += "L";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.NETWORKING)) result += "N";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.OBJECT_TRANSFER)) result += "O";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.POSITIONING)) result += "P";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.RENDER)) result += "R";
		if(device.getBluetoothClass().hasService(BluetoothClass.Service.TELEPHONY)) result += "T";
		return result;
	}
	
	public void stopScanning(){
		try {
			bluetoothScanIntervalHandler.removeCallbacks(bluetoothScanRunnable);
		} catch (Exception e) { Log.e(TAG, "Exception stopping bluetooth runnable"); }
	}
	
}
