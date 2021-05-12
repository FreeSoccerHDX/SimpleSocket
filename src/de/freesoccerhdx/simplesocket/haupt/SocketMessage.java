package de.freesoccerhdx.simplesocket.haupt;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;


public class SocketMessage {

	
	public String message;
	public String channelid;
	public List<String> targets;
	public ArrayList<String> trace;
	public JSON json;
	
	public SocketMessage(JSON json, ArrayList<String> trace, String channelid, List<String> targets, String msg) {
		this.message = msg;
		this.channelid = channelid;
		this.targets = targets;
		this.trace = trace;
		this.json = json;
	}
	
	@Override
	public String toString() {
		return "ServerMessage[trace='"+trace+"',id='"+channelid+"',targets="+targets+",message='"+message+"']";
	}
	
}
