package de.freesoccerhdx.simplesocket.haupt;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.List;


import de.freesoccerhdx.simplesocketclient.haupt.SimpleSocketClient;
import de.freesoccerhdx.simplesocketserver.haupt.SimpleSocketServer;

public class SimpleSocket {

	public static String escape(String s){
		  return s.replace("\\", "\\\\")
		          .replace("\t", "\\t")
		          .replace("\b", "\\b")
		          .replace("\n", "\\n")
		          .replace("\r", "\\r")
		          .replace("\f", "\\f")
		          .replace("\'", "\\'")
		          .replace("\"", "\\\"");
	}
	
	public static void main(String[] args) {
		
		String a = "\"";
		String b = "\\\"";
		
		for(char c : a.toCharArray()) {
			System.out.println("A: '" + c + "'" + (c=='\"'));
		}
		for(char c : b.toCharArray()) {
			System.out.println("B: '" + c + "'" + (c=='\"'));
		}
		
		/*
		JSON json = new JSON();
		JSONObject json2 = new JSONObject();
		
		
		json.put("string", "stri\"ng");
		json2.put("string", "stri\"ng");
		
		json.put("boolean", true);
		json2.put("boolean", true);
		
		json.put("int", 123456);
		json2.put("int", 123456);
		
		json.put("double", .0555d);
		json2.put("double", .0555d);
		
		json.put("float", 0.56f);
		json2.put("float", 0.56f);
		
		List<Object> list2 = new ArrayList<>();
		list2.add("Opfer");
		List<Object> list = new ArrayList<>();
		list.add("list_entry1");
		list.add("list_entry2");
		list.add("list_\"entry3");
		list.add(list2);
		
		//list.add(list);
		
		json.put("list", list);
		json2.put("list", list);
		
		HashMap<String, Object> map = new HashMap<>();
		map.put("number1", 123456);
		map.put("string1", "hallo");
		
		json.put("map", map);
		json2.put("map", map);
		
		String json_str = json.toJSON();
		String json_str2 = json2.toJSONString();
		
	
		System.out.println(escape(json_str));
		System.out.println(" ");
		System.out.println(escape(json_str2));
	
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		/*
		System.out.println("  ");
		System.out.println(json_str);
		System.out.println("  ");
		System.out.println(json_str2);
		System.out.println("  ");
		
		
		JSON.parseJSON(json_str);
		*/
		
		if(args.length >= 1) {
			String servertype = args[0];
			
			if(servertype.equals("-server")) {
				SimpleSocketServer.main(new String[0]);
			}else if(servertype.equals("-client")) {
			
				if(args.length >= 2) {
					String clientname = args[1];
					
					SimpleSocketClient.main(new String[] {clientname});
					
				}else {
					System.err.println("Usage: "
							+ "\n Client: java -jar SimpleSocket.java -client <name>"
							+ "\n Server: java -jar SimpleSocket.java -server");
				}
				
			}else {
				System.err.println("Usage: "
						+ "\n Client: java -jar SimpleSocket.java -client <name>"
						+ "\n Server: java -jar SimpleSocket.java -server");
			}
			
		}else {
			System.err.println("Usage: "
					+ "\n Client: java -jar SimpleSocket.java -client <name>"
					+ "\n Server: java -jar SimpleSocket.java -server");
		}
	}
	
}
