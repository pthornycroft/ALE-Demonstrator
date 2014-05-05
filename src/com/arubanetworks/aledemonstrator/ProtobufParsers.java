package com.arubanetworks.aledemonstrator;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.util.Log;

public class ProtobufParsers {
	static String TAG = "ProtobufParsers";
	static long zmqLastSeq = 0;
	static int zmqMissedSeq = 0;

	public static void parseAleMessage(byte[] inp){			
		try {
			//AleMsg.event event = AleMsg.event.parseFrom(inp);
			AleMsg.nb_event event = AleMsg.nb_event.parseFrom(inp);
			if(event.hasLocation()){ 
				parseAleLocation(event.getLocation()); 
				if(event.hasTopicSeq()) { parseAleSequence(event.getTopicSeq()); }
			}
			if(event.hasPresence()){ parseAlePresence(event.getPresence()); }
			if(event.hasStation()){ parseAleStation(event.getStation()); }
			if(event.hasRssi()){ parseAleRssi(event.getRssi()); }
			if(event.hasDestination()){ parseAleDestination(event.getDestination()); }
			if(event.hasApplication()) { parseAleApplication(event.getApplication()); }
			if(event.hasVisibilityRec()) { parseAleVisibilityRec(event.getVisibilityRec()); }
			if(event.hasFloor()){ parseAleFloor(event.getFloor()); }
		} catch (Exception e) { Log.e(TAG, "Exception parsing input as event "+e); }	
	}
	
	public static void parseAleSequence(long seq){
		if(zmqLastSeq != 0 && seq - zmqLastSeq > 1){
			zmqMissedSeq = zmqMissedSeq + (int)(seq - zmqLastSeq);
			Log.w(TAG, "missed ZMQ Seq new "+seq+"  last "+zmqLastSeq+" missed "+(seq-zmqLastSeq-1));
		}
		MainActivity.zmqLastSeq = seq;
	}
	
	public static void parseAleFloorMessageBytes(byte[] in){
		try{
			Log.v(TAG, "parsing floor");
			AleMsg.floor floor = AleMsg.floor.parseFrom(in);
			parseAleFloor(floor);
		}catch (Exception e) { Log.e(TAG, "Exception parsing AleFloorMessage "+e); }
		
	}

	public static void parseAleLocation(AleMsg.location location) {
		try{
			float sta_location_x = 0;
			float sta_location_y = 0;
			int error = 0;
			String floorId = "not found";
			String buildingId = "not found";
			String campusId = "not found";
			String sta_eth_mac = "";
			String hashed_sta_eth_mac = "";
			int seqNum = 0;
			
			sta_location_x = location.getStaLocationX();
			sta_location_y = location.getStaLocationY();
			error = location.getErrorLevel();
			floorId = LookupTables.byteStringToHexString(location.getFloorId());
			buildingId = location.getBuildingId().toStringUtf8();
			campusId = location.getCampusId().toStringUtf8();
			sta_eth_mac = LookupTables.byteStringToStringForMac(location.getStaEthMac().getAddr());
			hashed_sta_eth_mac = LookupTables.byteStringToHexString(location.getHashedStaEthMac());
			//Log.v(TAG, "parseAleLocationApi staEthMac "+staEthMac+" hashMac  "+hashed_sta_eth_mac+" my hash "+MainActivity.myHashMac+" xy "+sta_location_x+" "+sta_location_y+" floorId "+floorId);
			//if(MainActivity.myMac.equals("")) MainActivity.myMac = staEthMac;
			if(hashed_sta_eth_mac.equals("") && sta_eth_mac != null) { hashed_sta_eth_mac = sta_eth_mac; }
			if(hashed_sta_eth_mac.equalsIgnoreCase(MainActivity.myHashMac)) { 					
				Log.i(TAG, "That was ours "+hashed_sta_eth_mac+"  x_"+sta_location_x+"  y_"+sta_location_y+" site "+floorId); 
//				MainActivity.sbLog.append("\n" + DateFormat.getDateTimeInstance().format(new Date()) + "    New ALE reading for my MAC\n" +
//						"ALE x,y "+sta_location_x+", "+sta_location_y);
				MainActivity.site_xAle = sta_location_x;
				MainActivity.site_yAle = sta_location_y; 
//				MainActivity.floorIdAle = floorId;
				PositionHistoryObject newObject = new PositionHistoryObject(new Date(), 0, 0, sta_location_x, sta_location_y, -99, false, error, 
						floorId, buildingId, campusId, sta_eth_mac, hashed_sta_eth_mac, "ft", "XX", "XX", 0, null);
				MainActivity.alePositionHistoryList.add(0, newObject);
				MainActivity.zmqMessagesForMyMac++;
			} 

			// add the position history object to the device's array in the position hash map
			PositionHistoryObject newObject = new PositionHistoryObject(new Date(), 0, 0, sta_location_x, sta_location_y, -99, false, error, 
					floorId, buildingId, campusId, sta_eth_mac, hashed_sta_eth_mac, "ft", "XX", "XX", 0, null);
			//Log.i(TAG, "new positionHistoryObject showAllMacs eth _"+sta_eth_mac+"_ hash _"+hashed_sta_eth_mac+"_  x_"+sta_location_x+"  y_"+sta_location_y+" site "+floorId);
			if(MainActivity.aleAllPositionHistoryMap.containsKey(hashed_sta_eth_mac) && !floorId.equals("not found") &&
					MainActivity.floorListIndex != -1 && floorId.equals(MainActivity.floorList.get(MainActivity.floorListIndex).floor_id)){
				if(MainActivity.aleAllPositionHistoryMap.get(hashed_sta_eth_mac).size() > 100) { 
					Log.w(TAG, "position history array was over 500 "+hashed_sta_eth_mac);
					MainActivity.aleAllPositionHistoryMap.get(hashed_sta_eth_mac).remove(0); 
				}
				MainActivity.aleAllPositionHistoryMap.get(hashed_sta_eth_mac).add(newObject);
			}
			else {
				ArrayList<PositionHistoryObject> newList = new ArrayList<PositionHistoryObject>();
				newList.add(newObject);
				MainActivity.aleAllPositionHistoryMap.put(hashed_sta_eth_mac, newList);
			}

			addToEventLogMap(hashed_sta_eth_mac, "LOCATION x_"+sta_location_x+"_ y_"+sta_location_y+"_");
			
		} catch (Exception e) { 
			Log.e(TAG, "Exception parsing Location protobuf event "+e);
			MainActivity.zmqStatusString = "could not parse zmq location "+e;
		} 
	}
	
	public static void parseAlePresence(AleMsg.presence presence){
		try{
			String sta_eth_mac = "";
			boolean associated = false;
			String hashed_sta_eth_mac = "";
		
			sta_eth_mac = LookupTables.byteStringToStringForMac(presence.getStaEthMac().getAddr());
			associated = presence.getAssociated();
			hashed_sta_eth_mac = LookupTables.byteStringToHexString(presence.getHashedStaEthMac());
			if(hashed_sta_eth_mac.equals("") && sta_eth_mac != null) { hashed_sta_eth_mac = sta_eth_mac; }
		
			//Log.v(TAG, "parseAlePresence sta_eth_mac _"+sta_eth_mac+"_ hashed_sta_eth_mac _"+hashed_sta_eth_mac+"_  associated _"+associated+"_");
			
			addToEventLogMap(hashed_sta_eth_mac, "PRESENCE associated_"+associated+"_");
			
		} catch (Exception e) { Log.e(TAG, "Exception parsing Presence protobuf event "+e); } 
	}

	public static void parseAleRssi(AleMsg.rssi rssi){
		try{
			String sta_eth_mac = "";
			String radio_mac = "";
			int rssi_val = -99;
			boolean associated = false;
			String hashed_sta_eth_mac = "";
		
			sta_eth_mac = LookupTables.byteStringToStringForMac(rssi.getStaEthMac().getAddr());
			radio_mac = LookupTables.byteStringToStringForMac(rssi.getRadioMac().getAddr());
			rssi_val = rssi.getRssiVal();
			associated = rssi.getAssociated();
			hashed_sta_eth_mac = LookupTables.byteStringToHexString(rssi.getHashedStaEthMac());
			if(hashed_sta_eth_mac.equals("") && sta_eth_mac != null) { hashed_sta_eth_mac = sta_eth_mac; }

			//Log.v(TAG, "parseAleRssi sta_eth_mac _"+sta_eth_mac+"_ hashed_sta_eth_mac _"+hashed_sta_eth_mac+"_ rssi_val _"+rssi_val+"_ radio mac _"+radio_mac+"_ associated _"+associated+"_");
			
			addToEventLogMap(hashed_sta_eth_mac, "RSSI radio_mac _"+radio_mac+"_ rssi_val _"+rssi_val+"_");

		} catch (Exception e) { Log.e(TAG, "Exception parsing Rssi protobuf event "+e); } 
	}
		
	public static void parseAleStation(AleMsg.station station){
		try{
			String sta_eth_mac = "";
			String username = "";
			String role = "";
			String bssid = "";
			String device_type = "";
			String sta_ip_address = "";
			String hashed_sta_eth_mac = "";
			String hashed_sta_ip_address = "";
		
			sta_eth_mac = LookupTables.byteStringToStringForMac(station.getStaEthMac().getAddr());
			username = station.getUsername();
			role = station.getRole();
			bssid = LookupTables.byteStringToStringForMac(station.getBssid().getAddr());
			device_type = station.getDeviceType();
			sta_ip_address = LookupTables.aleStringToIpAddr((LookupTables.byteStringToHexString(station.getStaIpAddress().getAddr())));
			hashed_sta_eth_mac = LookupTables.byteStringToHexString(station.getHashedStaEthMac());
			hashed_sta_ip_address = LookupTables.byteStringToHexString((station.getHashedStaIpAddress()));
			if(hashed_sta_eth_mac.equals("") && sta_eth_mac != null) { hashed_sta_eth_mac = sta_eth_mac; }
		
			//Log.v(TAG, "parseAleStation sta_eth_mac _"+sta_eth_mac+"_ hashed_sta_eth_mac _"+hashed_sta_eth_mac+"_ username _"+username+"_ role _"+role+"_ bssid _"+bssid+
			//		"_ device_type _"+device_type+"_ sta_ip_address _"+sta_ip_address+"_ hashed_sta_ip_address _"+hashed_sta_ip_address+"_");
			
			addToEventLogMap(hashed_sta_eth_mac, "STATION username _"+username+"_ role _"+role+"_ bssid _"+bssid+"_ device_type _"+device_type+"_");
	
		} catch (Exception e) { Log.e(TAG, "Exception parsing Station protobuf event "+e); } 
	}
	
	public static void parseAleDestination(AleMsg.destination destination){
		try{
			
			/*Map map = destination.getAllFields();
			Iterator entries = map.entrySet().iterator();
			String result = "";
			while (entries.hasNext()){
				Entry thisEntry = (Entry) entries.next();
				Object key = thisEntry.getKey();
				Object value = thisEntry.getValue();
				result = result + "___"+value.toString();
			}
			Log.v(TAG, "parseAleDestination "+result);
			*/
			String dest_ip = "";
			String dest_name = "";
			String dest_alias_name = "";
					
			dest_name = destination.getDestName();
			dest_alias_name = destination.getDestAliasName();
			
			//Log.v(TAG, "parseAleDestination dest_ip _"+dest_ip+"_ dest_name _"+dest_name+"_ dest_alias_name _"+dest_alias_name+"_");

		} catch (Exception e) { Log.e(TAG, "Exception parsing Destination protobuf event "+e); } 
	}
	
	public static void parseAleApplication(AleMsg.application application){
		try{
			
			int app_id = 0;
			String app_name = "";
					
			app_id = application.getAppId();
			app_name = application.getAppName();
			
			//Log.v(TAG, "parseAleApplication app_id _"+app_id+"_ app_name _"+app_name+"_");

		} catch (Exception e) { Log.e(TAG, "Exception parsing Application protobuf event "+e); } 
	}
	
	public static void parseAleVisibilityRec(AleMsg.visibility_rec visibility_rec){
		try{
			
			/*Map map = visibility_rec.getAllFields();
			Iterator entries = map.entrySet().iterator();
			String result = "";
			while (entries.hasNext()){
				Entry thisEntry = (Entry) entries.next();
				Object key = thisEntry.getKey();
				Object value = thisEntry.getValue();
				result = result + "___"+value.toString();
			}
			Log.v(TAG, "parseAleVisibilityRec "+result); */
			
			String client_ip = "";
			String dest_ip = "";
			int ip_proto = 0;
			int app_id = 0;		
			long tx_pkts = 0;
			long tx_bytes = 0;
			long rx_pkts = 0;
			long rx_bytes = 0;
			String hashed_client_ip = "";
			String device_mac = "";
			String hashed_device_mac = "";
			String app_name = "";
			if(hashed_device_mac.equals("") && device_mac != null) { hashed_device_mac = device_mac; }
			client_ip = LookupTables.aleStringToIpAddr((LookupTables.byteStringToHexString(visibility_rec.getClientIp().getAddr())));
			dest_ip = LookupTables.aleStringToIpAddr((LookupTables.byteStringToHexString(visibility_rec.getDestIp().getAddr())));
			ip_proto = visibility_rec.getIpProto().getNumber();
			app_id = visibility_rec.getAppId();
			tx_pkts = visibility_rec.getTxPkts();
			tx_bytes = visibility_rec.getTxBytes();
			rx_pkts = visibility_rec.getRxPkts();
			rx_bytes = visibility_rec.getRxBytes();
			hashed_client_ip = LookupTables.byteStringToHexString((visibility_rec.getHashedClientIp()));
			device_mac = LookupTables.byteStringToStringForMac(visibility_rec.getDeviceMac().getAddr());
			hashed_device_mac = LookupTables.byteStringToHexString(visibility_rec.getHashedDeviceMac());
			app_name = visibility_rec.getAppName();
			
			//Log.v(TAG, "parseAleVisibility_rec client_ip _"+client_ip+"_ dest_ip _"+dest_ip+"_ ip_proto _"+ip_proto+"_ app_id _"+app_id+"_ tx_pkts _"+tx_pkts+
			//		"_ tx_bytes _"+tx_bytes+"_ rx_pkts _"+rx_pkts+"_ rx_bytes _"+rx_bytes+"_ hashed_client_ip _"+hashed_client_ip+
			//		"_ device_mac _"+device_mac+"_ hashed_device_mac _"+hashed_device_mac+"_ app_name _"+app_name+"_");

		} catch (Exception e) { Log.e(TAG, "Exception parsing Visibility_rec protobuf event "+e); } 
	}
	
	public static void parseAleCampus(AleMsg.campus campus){
		try{
			String campus_id = "";
			String campus_name = "";
			
			campus_id = campus.getCampusId().toStringUtf8();
			campus_name = campus.getCampusName().toString();
			
			Log.v(TAG, "parseAleCampus campus_id _"+campus_id+"_ campus_name _"+campus_name+"_");
			
		} catch (Exception e) { Log.e(TAG, "Exception parsing Campus protobuf event "+e); } 
	}
	
	public static void parseAleBuilding(AleMsg.building building){
		try{
			String building_id = "";
			String building_name = "";
			String campus_id = "";
			
			building_id = building.getBuildingId().toStringUtf8();
			building_name = building.getBuildingName().toString();
			campus_id = building.getCampusId().toStringUtf8();
			
			//Log.v(TAG, "parseAleBuilding building_id _"+building_id+"_ building_name _"+building_name+"_ campus_id _"+campus_id+"_");
			
		} catch (Exception e) { Log.e(TAG, "Exception parsing Building protobuf event "+e); } 
	}
	
	public static void parseAleFloor(AleMsg.floor floor){
		try{
			String floorId = "XXXXXXXXXXXXXXXX";
			String floorName = "not found";
			float floorLatitude = 0;
			float floorLongitude = 0;
			float floorplanWidth = 0;
			float floorplanHeight = 0;
			String buildingId = "XXXXXXXXXXXXXXXX";
			String floor_img_path = "not found";
			
			floorId = floor.getFloorId().toStringUtf8();
			floorName = floor.getFloorName();
			floorLatitude = floor.getFloorLatitude();
			floorLongitude = floor.getFloorLongitude();
			floorplanWidth = floor.getFloorImgWidth();
			floorplanHeight = floor.getFloorImgLength();
			floor_img_path = LookupTables.byteStringToHexString(floor.getFloorImgPathBytes());
			buildingId = floor.getBuildingId().toStringUtf8();
			
			//Log.v(TAG, "floorId "+floorId+" name "+floorName+" lat "+floorLatitude+" long "+
			//		floorLongitude+" width "+floorplanWidth+" height "+floorplanHeight+
			//		" buildingId "+buildingId+" floor_img_path "+floor_img_path);
		} catch (Exception e) { Log.e(TAG, "Exception parsing Floor protobuf event "+e); } 
	}
	
	public static void addToEventLogMap(String hashed_sta_eth_mac, String event){
		String datestamp = DateFormat.getDateTimeInstance().format(new Date());
		if(!MainActivity.eventLogMap.containsKey(hashed_sta_eth_mac)){ MainActivity.eventLogMap.put(hashed_sta_eth_mac, new ArrayList<String>()); }
		MainActivity.eventLogMap.get(hashed_sta_eth_mac).add(0, datestamp+"  :  "+event);

		// trim the list if it's more than 500 entries
		if(MainActivity.eventLogMap.get(hashed_sta_eth_mac).size() > 100) {
			MainActivity.eventLogMap.get(hashed_sta_eth_mac).remove(MainActivity.eventLogMap.get(hashed_sta_eth_mac).size()-1);
			MainActivity.eventLogMap.get(hashed_sta_eth_mac).remove(MainActivity.eventLogMap.get(hashed_sta_eth_mac).size()-2);

		}
	}
	
	
}
