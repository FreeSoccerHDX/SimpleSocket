package de.freesoccerhdx.simplesocket.haupt;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSON extends HashMap<String,Object> implements Map<String,Object>{

	/**
	 * 
	 */
	
	private static String escape(String s){
		  return s.replace("\\", "\\\\")
		          .replace("\t", "\\t")
		          .replace("\b", "\\b")
		          .replace("\n", "\\n")
		          .replace("\r", "\\r")
		          .replace("\f", "\\f")
		          .replace("\'", "\\'")
		          .replace("\"", "\\\"");
	}
	
	private static final long serialVersionUID = -142307832709111567L;


	public JSON(Map map) {
		super(map);
	}
	
	public JSON() {
		super();
	}
	
	
	@Override
	public String toString() {
		return toJSON();
	}
	
	public String toJSON() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("{");
		
		
		String[] keyset = keySet().toArray(new String[] {});
		for(String key : keyset) {
			
			buffer.append("\"");
			
			buffer.append(key);
			
			buffer.append("\"");
			buffer.append(":");
			
			// TODO: Write Values
			
			buffer.append(toJSONValue(get(key)));
			
			if(!keyset[keyset.length-1].equals(key)) {
				buffer.append(",");
			}
			
		}
		
		
		buffer.append("}");
		
		
		return buffer.toString();
	}
	
	
	/*
	{
	  "Herausgeber": "Xema",
	  "Nummer": "1234-5678-9012-3456",
	  "Deckung": 2e+6,
	  "Waehrung": "EURO",
	  "Inhaber":
	  {
	    "Name": "Mustermann",
	    "Vorname": "Max",
	    "maennlich": true,
	    "Hobbys": ["Reiten", "Golfen", "Lesen"],
	    "Alter": 42,
	    "Kinder": [],
	    "Partner": null
	  }
	}
	 * */
	
	private String toJSONValue(Object value) {
		
		StringBuffer buffer = new StringBuffer();
		
		if(value == null) {
			
			buffer.append("null");
			
		}else if(value instanceof String) {
			
			buffer.append("\"");		
			buffer.append(escape((String) value));
			buffer.append("\"");
			
		}else if(value instanceof Double) {
			
			buffer.append(value.toString());
			
		}else if(value instanceof Float) {
		
			buffer.append(value.toString());
			
		}else if(value instanceof Number) {
			
			buffer.append(value.toString());
			
		}else if(value instanceof Boolean) {
			
			buffer.append(value.toString());
			
		}else if(value instanceof List) {
			List list = (List) value;
			
			buffer.append("[");
			
			for(Object obj : list) {
				
				buffer.append(toJSONValue(obj));
				
				if(!list.get(list.size()-1).equals(obj)) {
					buffer.append(",");
				}
			}
			
			buffer.append("]");

		}else if(value instanceof Map) {
			
			JSON json = new JSON((Map) value);
			
			buffer.append(json.toJSON());
			
		}else {
			throw new IllegalArgumentException("Unknown translate type: " + value);
		}
		
		
		
		return buffer.toString();
	}
	
	// return new json
	private static String skip(String skip, String json) {
		
		boolean skipping = true;
		
		while(skipping) {
			
			if(json.startsWith(skip)){
				json = json.substring(skip.length()); 
				skipping = false;
			}else if(json.startsWith(" ")) {
				json = json.substring(1);	
			}
			
			if(json.length() == 0) {
				return "";
			}
		}
		
		
		return json;
	}
	
	private static String[] readUntil(String until, String json) {
		
		String read = "";
		
		while(!json.startsWith(until)) {
			read += json.substring(0,1);
			json = json.substring(1);
			
			if(json.length() == 0) {
				return new String[] {read,json}; 
			}
		}
		
		
		return new String[] {read,json};
	}
	
	
	// {"boolean":true,"string":"string","double":1.0555,"float":0.56,"list":["list_entry1","list_entry2"],"map":{"string1":"hallo","number1":123456},"int":123456}
	public static JSON parseJSON(String json) {
		JSON j = new JSON();
		
		String newjson = skip("{",json);
		
		if(newjson.length() == 0) {
			// ERROR
		}else {
			json = newjson; 
			// OK
		}
		
				
		
		boolean haskeys = true;
		
		while(haskeys) { // "my_key":"value";
			newjson = skip("\"",json);
			
			if(newjson.length() == 0) {
				// ERROR
			}else {
				json = newjson;
				
				String[] key_data = readUntil("\"", json); // 0->read-data; 1->new-json
				String readdata = key_data[0];
				newjson = key_data[1];
				
				if(newjson.length() == 0) {
					// ERROR
				}else {
					json = newjson;
					String key_name = readdata;
					System.out.println("key_name: " + key_name);
				//	System.out.println("json: " + json);
					
					newjson = skip("\"",json);
					
					if(newjson.length() == 0) {
						// ERROR
						System.err.println("error 1");
					}else {
						json = newjson;
						newjson = skip(":",json);
						
						if(newjson.length() == 0) {
							// ERROR
							System.err.println("error 2");
						}else {
							json = newjson;
							
							if(json.startsWith("\"")) {
			// String
								json = skip("\"",json);
								
								String[] value_data = readUntil("\",",json);
								readdata = value_data[0];
								newjson = value_data[1];
								
								if(newjson.length() == 0) {
									// ERROR
									System.err.println("error 3 string");
								}else {
									json = newjson;
									json = skip("\"",json);
									
									System.out.println("value: " + readdata);
									System.out.println("json: " + json);
									
									j.put(key_name, readdata);
									
									newjson = skip(",",json);
									
									if(newjson.length() == 0) {
										
										if(newjson.startsWith("}")) {
											// END
											haskeys = false;
										}else {
											// ERROR
											System.err.println("error 4");
										}
									}else {
										json = newjson;
										// start from beginning;
									}
								}
								
								
							}else if(json.startsWith("[")) {
			// list
								System.out.println("listjson: " + json);
								
								json = skip("[",json);
								
								
								
								
							}else if(json.startsWith("{")) {
			// map
							}else if(json.startsWith("true") || json.startsWith("false")) {
			// boolean					
								String[] value_data = readUntil(",",json);
								readdata = value_data[0];
								newjson = value_data[1];
								
								if(newjson.length() == 0) {
									// ERROR
									System.err.println("error 3 boolean");
								}else {
									json = newjson;
									System.out.println("value: " + readdata);
									System.out.println("json: " + json);
									j.put(key_name, readdata);
									
									newjson = skip(",",json);
									
									if(newjson.length() == 0) {
										
										if(newjson.startsWith("}")) {
											// END
											haskeys = false;
										}else {
											// ERROR
											System.err.println("error 4");
										}
									}else {
										json = newjson;
										// start from beginning;
									}
									
								}
								
							}else {
								System.out.println("json: " + json);
								
								if(json.startsWith("0") 
										|| json.startsWith("1") 
										|| json.startsWith("2") 
										|| json.startsWith("3")
										|| json.startsWith("4")
										|| json.startsWith("5")
										|| json.startsWith("6")
										|| json.startsWith("7")
										|| json.startsWith("8")
										|| json.startsWith("9")
										) {
			// number				
									// read until "," or ] or } ???
									
									String[] c = new String[] {",\"","]","}"};
									boolean found = false;
									
									for(int i = 0; i < c.length; i++) {
										String[] value_data = readUntil(""+c[i], json);
										
									
										if(value_data[1].length() == 0) {
											// char is not the end of this
										}else {
											readdata = value_data[0];
											newjson = value_data[1];
											found = true;
											break;
										}
									}
									
									if(found) {
										json = newjson;
										System.out.println("json: " + json);
										System.out.println("value: " + readdata);
										
										Object realobj = null;
										
										if(readdata.contains(".")) {
											realobj = Double.parseDouble(readdata);
										}else {
											realobj = Integer.parseInt(readdata);
										}
										
										j.put(key_name, realobj);
										
										
										if(json.startsWith(",")) {
											json = skip(",", json);
										}else if(json.startsWith("]")) {
											// end list
										}else if(json.startsWith("}")) {
											// end all
											haskeys = false;
										}
										
										
									}else {
										// ERROR 
										System.err.println("error 3 number");
									}
									
									
									
								}
								

							}
							
						}
						
					}
					
					
				//	break;
					
				}
				
				
			}
			
		}
		
		
		
		return j;
	}
	
	
	// ["list_entry1","list_entry2","list_\"entry3",["Opfer"]],"map":{"string1":"hallo","number1":123456},"int":123456}
	private static String[] readList(String json) {
		
		
		List<Object> list = new ArrayList<>();
		
		int pos = -1;
		int open = 1;
		
		boolean in_string = false;
		json = skip("[",json);
		
		char[] chars = json.toCharArray();
		
		for(char c : chars) {
			pos++;
			
			if(!in_string) {
				if(c == '[') {
					open ++;
				}
				if(c == ']') {
					open --;
				}
			}
			
			if(c == '"') {
				if(!in_string) {
					in_string = true;
				}else {
					in_string = false;
				}
			}
			
			if(open == 0) {
				break;
			}
		}
		
		
		
		
		
		
		return new String[0];
	}
	
}


























