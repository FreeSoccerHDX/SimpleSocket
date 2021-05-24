package de.freesoccerhdx.simplesocket.haupt;

import java.io.File;
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
		
		
		try {
			String jsondata3 = "{\"Waehrung\":\"EURO\",\"Inhaber\":{\"Alter\":42,\"maennlich\":true,\"Hobbys\":[\"Reiten\",\"Golfen\",\"Lesen\"],\"Kinder\":[[[]]],\"Partner\":null,\"Name\":\"Mustermann\",\"Vorname\":\"Max\"},\"Deckung\":2000000.0,\"Nummer\":\"1234-5678-9012-3456\",\"Herausgeber\":\"Xema\"}";
			
			String jsondata2 = "{\r\n"
					+ "\"Herausgeber\": \"Xema\",\r\n"
					+ "\"Nummer\": \"1234-5678-9012-3456\",\r\n"
					+ "\"Deckung\": 2e+6,\r\n"
					+ "\"Waehrung\": \"EURO\",\r\n"
					+ "\"Inhaber\":\r\n"
					+ "{\r\n"
					+ "\"Name\": \"Mustermann\",\r\n"
					+ "\"Vorname\": \"Max\",\r\n"
					+ "\"maennlich\": true,\r\n"
					+ "\"Hobbys\": [\"Reiten\", \r\n"
					+ "\"Golfen\", \"Lesen\"],\r\n"
					+ "\"Alter\": 42,\r\n"
					+ "\"Kinder\": [[[]]],\r\n"
					+ "\"Partner\": null\r\n"
					+ "}\r\n"
					+ "}";
			
			String jsondata = "{\r\n"
					+ "	  \"Herausgeber\": \"Xema\",\r\n"
					+ "	  \"Nummer\": \"1234-5678-9012-3456\",\r\n"
					+ "	  \"Deckung\": 2e+6,\r\n"
					+ "	  \"Waehrung\": \"EURO\",\r\n"
					+ "	  \"Inhaber\":\r\n"
					+ "	  {\r\n"
					+ "	    \"Name\": \"Mustermann\",\r\n"
					+ "	    \"Vorname\": \"Max\",\r\n"
					+ "	    \"maennlich\": true,\r\n"
					+ "	    \"Hobbys\": [\"Reiten\", \"Golfen\", \"Lesen\"],\r\n"
					+ "	    \"Alter\": 42,\r\n"
					+ "	    \"Kinder\": [[[]]],\r\n"
					+ "	    \"Partner\": null\r\n"
					+ "	  }\r\n"
					+ "	}";
					
			JSON customjson = JSON.parseJSON(jsondata);
					
			System.out.println("JSON: " + customjson.toJSON());
			
		//	customjson.saveToFile(new File("jsondata_text.json"));
			
			JSON newjson = JSON.openJSONFile(new File("jsondata_text.json"));
			System.out.println("JSON: " + newjson.toJSON());
			
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		/*
		String in = "87.101.114.32.100.97.115.32.108.105.101.115.116.32.100.115.116.32.100.111.111.102.33";
		System.out.println("before: "+ in);
		in = in.replace("."," ");
		System.out.println("after: "+ in);
		String out = "";
		
		String[] insplit = in.split(" ");
		System.out.println(insplit.length);
		for(String s : insplit) {
			int i = Integer.parseInt(s);
			char c = (char) i;
			System.out.println(""+c);
			String a = c+"";
			out += a;
		}
		System.out.println("out: "+out);
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		System.out.println(" ");
		*/
		/*
		String in = "Wer kann das lesen ?";
		String out = "";
		for(char c : in.toCharArray()) {
			int i = c;
			out += i+".";
		}
		System.out.println(out);
		*/
		
		/*
		 * 
		String a = "\"";
		String b = "\\\"";
		
		for(char c : a.toCharArray()) {
			System.out.println("A: '" + c + "'" + (c=='\"'));
		}
		for(char c : b.toCharArray()) {
			System.out.println("B: '" + c + "'" + (c=='\"'));
		}
		 * */
		
		
		JSON json = new JSON();
	//	JSONObject json2 = new JSONObject();
		List<String> customdata = new ArrayList<>();
	//	customdata.add("alles");
		json.put("firstlist", customdata);
		
		json.put("string", "stri\"ng");
	//	json2.put("string", "stri\"ng");
		
		json.put("boolean", true);
	//	json2.put("boolean", true);
		
	//	json.put("int", 12345e+5);
	//	json2.put("int", 123456);
		
		json.put("double", .0555d);
	//	json2.put("double", .0555d);
		
		json.put("float", 0.56f);
	//	json2.put("float", 0.56f);
		
		List<Object> list2 = new ArrayList<>();
		list2.add("Opfer");
		List<Object> list = new ArrayList<>();
		list.add("list_entry1");
		list.add("list_entry2");
		list.add("list_\"entry3");
		list.add(list2);
		
//		json.put("empty_string", "");
		json.put("null?", null);
		
		//list.add(list);
		
		json.put("list", list);
//		json2.put("list", list);
		
		HashMap<String, Object> map = new HashMap<>();
		map.put("number1", 123456);
		map.put("string1", "hallo");
		
		json.put("map", map);
		
		
		HashMap<String, Object> map2 = new HashMap<>();
		json.put("map2", map2);
		
	//	json2.put("map", map);
		
		String json_str = json.toJSON();
	//	String json_str2 = json2.toJSONString();
		
	/*
		System.out.println(" ");
		System.out.println("Start:");
		System.out.println((json_str));
		System.out.println(" ");
		System.out.println(" ");
		
		JSONParsing parsing = new JSONParsing(json_str);
		
		
		JSON newjson = parsing.createJSONObject();
		
		System.out.println(" ");
		System.out.println("End:");
		System.out.println((newjson.toJSON()));
		System.out.println(" ");
		System.out.println(" ");
		
		
		System.out.println(" ");
		System.out.println("Is the same before and after... ?");
		System.out.println("b: " + newjson.toJSON().equals(json.toJSON()));
		System.out.println(" ");
		System.out.println(" ");
		try {
			System.out.println((new JSONParser().parse(json_str)));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(" ");
		*/
		
	//	System.out.println(escape(json_str2));
	
//		System.out.println(" ");
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
