package de.freesoccerhdx.simplesocket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SocketMessage {

	private static ArrayList<String> createArrayList(JSONArray tracejson){
		ArrayList<String> arrayList = new ArrayList(tracejson.length());
		Iterator<Object> traceIterator = tracejson.iterator();
		while(traceIterator.hasNext()){
			arrayList.add((String) traceIterator.next());
		}

		return arrayList;
	}

	
	public String message;
	public String channelid;
	public List<String> targets;
	public ArrayList<String> trace;
	public JSONObject json;

	private SocketMessage(){
	}

	public SocketMessage(JSONObject json, JSONArray trace, String channelid, JSONArray targets, String msg) {
		this.message = msg;
		this.channelid = channelid;
		this.targets = createArrayList(targets);
		this.trace = createArrayList(trace);
		this.json = json;
	}
	
	@Override
	public String toString() {
		return "ServerMessage[trace='"+trace+"',id='"+channelid+"',targets="+targets+",message='"+message+"']";
	}
	
}
