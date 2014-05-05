package com.arubanetworks.aledemonstrator;

import android.util.Log;

public class BluetoothBeacon {

    /**
     * Less than half a meter away
     */
    public static final int PROXIMITY_IMMEDIATE = 1;
    /**
     * More than half a meter away, but less than four meters away
     */
    public static final int PROXIMITY_NEAR = 2;
    /**
     * More than four meters away
     */
    public static final int PROXIMITY_FAR = 3;
    /**
     * No distance estimate was possible due to a bad RSSI value or measured TX power
     */
    public static final int PROXIMITY_UNKNOWN = 0;

    private static final String TAG = "Beacon";

    private String proximityUuid;
    private String bid;
    private int rssi;
    private int major;
    private int minor;
    private int measuredPwr;

    public BluetoothBeacon() {}

    /**
     * Construct an iBeacon from a Bluetooth LE packet collected by Android's Bluetooth APIs
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @return An instance of a <code>Scanned</code>
     */
    public static BluetoothBeacon fromScanData(byte[] scanData, int rssi) {

        //Log.d(TAG, "Scan data: " + bytesToHex(scanData));

        if (!smellsLikeABeacon(scanData))
            return null;

        BluetoothBeacon beacon = new BluetoothBeacon();

        // extract the UUID from the relevant portion of the scan data
        byte[] proximityUuidBytes = new byte[16];
        System.arraycopy(scanData, 9, proximityUuidBytes, 0, 16);
        beacon.proximityUuid = bytesToUUIDString(proximityUuidBytes);

        // extract the major/minor as a single BID from the scan data
        byte[] bidBytes = new byte[8];
        System.arraycopy(scanData, 25, bidBytes, 0, 8);
        beacon.bid = bytesToBID(bidBytes);
        beacon.major = (bidBytes[0] & 0xff) * 0x100 + (bidBytes[1] & 0xff);
        beacon.minor = (bidBytes[2] & 0xff) * 0x100 + (bidBytes[3] & 0xff);
        
        beacon.measuredPwr = (bidBytes[4]);
 
        beacon.rssi = rssi;

        return beacon;
    }

    /**
     * A 16-byte UUID typically unique to a particular "fleet" of Beacons.
     */
    public String getProximityUuid() {
        return proximityUuid;
    }

    /**
     * The Beacon ID, like "1b:c3:54:fe"
     */
    public String getBid() {
        return bid;
    }
    
    /**
     * The Beacon Major int, 0 - 65535
     */
    public int getMajor() {
        return major;
    }
    
    /**
     * The Beacon Minor int, 0 - 65535
     */
    public int getMinor() {
        return minor;
    }
    
    /**
     * The Beacon calibrated transmit power, expected level at 1 metre distance
     */
    public int getMeasuredPwr() {
        return measuredPwr;
    }

    /**
     * Received signal strength in decibels of the specified beacon.
     */
    public int getRssi() { return rssi; }

    @Override
    public String toString() {
        return "Beacon <UUID=" + proximityUuid + " BID=" + bid + " rssi=" + rssi + ">";
    }

    private static boolean smellsLikeABeacon(byte[] scanData) {
        // "Spec" (reverse-engineered): http://stackoverflow.com/questions/18906988/what-is-the-ibeacon-bluetooth-profile

        // look for the "Apple's fixed iBeacon advertising prefix" - note that we don't inspect byte 2, as
        // our beacons broadcast a different byte than the "spec" and they still work with the iOS API.
        return ((int)scanData[0] & 0xff) == 0x02 &&
                ((int)scanData[1] & 0xff) == 0x01 &&
                ((int)scanData[3] & 0xff) == 0x1a &&
                ((int)scanData[4] & 0xff) == 0xff &&
                ((int)scanData[5] & 0xff) == 0x4c;
    }

    //
    // Utility methods
    //

    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String bytesToUUIDString(byte[] bytes) {
        String hex = bytesToHex(bytes);
        return hex.substring(0, 8) + "-"
                + hex.substring(8, 12) + "-"
                + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-"
                + hex.substring(20, 32);
    }

    private static String bytesToBID(byte[] bytes) {
        String hex = bytesToHex(bytes);
        return hex.substring(0, 2) + ":"
                + hex.substring(2, 4) + ":"
                + hex.substring(4, 6) + ":"
                + hex.substring(6, 8);
    }

}
