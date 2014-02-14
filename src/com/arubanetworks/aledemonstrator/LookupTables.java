package com.arubanetworks.aledemonstrator;

import android.util.Log;

import com.google.protobuf.ByteString;

public class LookupTables {
	
	public static String byteStringToHexString(ByteString byteStr){
		String result = "";
		for(int i=0; i<byteStr.size(); ++i){
			result += String.format("%02X", byteStr.byteAt(i));
		}
		return result;
	}
	
	public static String byteStringReversedToHexString(ByteString byteStr){
		String result = "";
		for(int i=byteStr.size()-1; i>=0; i--){
			result += String.format("%02X", byteStr.byteAt(i));
		}
		return result;
	}

	
	public static String byteStringToStringForMac(ByteString byteStr){
		String result = "";
		for(int i=0; i<byteStr.size(); ++i){
			if(i !=0 ) result += ":";
			result += String.format("%02X", byteStr.byteAt(i));
		}
		return result;
	}
	
	public static String hexToString(String hex){
	    StringBuilder sb = new StringBuilder();
	    for (int count = 0; count < hex.length() - 1; count += 2){
	        String output = hex.substring(count, (count + 2));    //grab the hex in pairs
	        int decimal = Integer.parseInt(output, 16);    //convert hex to decimal
	        sb.append((char)decimal);    //convert the decimal to character
	    }
	    return sb.toString();
	}
	
	// Parses IP address in dot-numbered format
	public static String aleStringToIpAddr(String in) {
		if(in == null || in == "") { return ""; }
		long l = Long.parseLong(in, 16);
        return ((l >>  24 ) & 0xFF) + "." +((l >> 16 ) & 0xFF) + "." +((l >> 8 ) & 0xFF) + "." + (l & 0xFF);
    }

	
	
	
}
