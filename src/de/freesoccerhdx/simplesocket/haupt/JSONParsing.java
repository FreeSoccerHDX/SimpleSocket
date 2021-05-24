package de.freesoccerhdx.simplesocket.haupt;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class JSONParsing {

	private static int STATUS_START = 0;
	
	private static int STATUS_START_KEY = 1;
	private static int STATUS_READ_KEY = 2;
	private static int STATUS_END_KEY = 3;
	
	private static int STATUS_START_VALUE = 4;
	private static int STATUS_READ_VALUE = 5;
	private static int STATUS_END_VALUE = 6;
	
	
	private static int DATA_TYPE_BOOLEAN = 0;
	private static int DATA_TYPE_NUMBER = 1;
	private static int DATA_TYPE_STRING = 2;
	private static int DATA_TYPE_LIST = 3;
	private static int DATA_TYPE_JSON = 4;
	private static int DATA_TYPE_NULL = 5;
	
	
	
	private String data;
	public JSONParsing(String data) {
		this.data = data;
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
	 */
	
	
	public JSON createJSONObject() {
		
		boolean b = false;
		
		if(data.startsWith("{") && data.endsWith("}")) {
			b = true;
		}else {
			throw new IllegalArgumentException("JSON-Element does not start with { or does not end with }");
		}
		
		if(!b) return null;
		
		
		
		JSON json = new JSON();
		
		int newdatatype = -1;
		String last_key = "";
		String info = "";
		int status = STATUS_START;
		
		boolean leading_backslash = false;
		
		
		boolean instring = false;
		int open_brackets = 0;
		
		while(data.length() > 0) {
			boolean skipleer = false;
			
			//String[] a = data.split("",2);
			String s = data.substring(0, 1);
			data = data.substring(1);
			
		//	System.out.println("data: " + data);
			
			if(!instring && (char)s.charAt(0) == 13) {
				// weird unicode char				
			}else if(!instring && (char)s.charAt(0) == 10) {
				// weird unicode char			
			}else if(!instring && (char)s.charAt(0) == 9) {
				// weird unicode char
			}else if(s.equals("\\") && !leading_backslash) {
				leading_backslash = true;
			}else {
			
				if(status == STATUS_START) {
					if(s.equals("{")) {
						if(data.startsWith("}")) {
							break; // empty map lol
						}
						status = STATUS_START_KEY;
						skipleer = true;
					}else {
						throw new IllegalArgumentException("An JSON-String has to start with '{'");
					}
				}else if(status == STATUS_START_KEY){
					if(s.equals("\"")) {
						status = STATUS_READ_KEY;
					}else {
						
						char ch = s.charAt(0);
						int charid = ch;
						System.out.println("char: '" + charid +"'");
						System.out.println("restdata: " + data);
						throw new IllegalArgumentException("An Key has to start with '\"' and not '"+ch+"'");
					}
				}else if(status == STATUS_READ_KEY) {
					info += s;
					if(data.startsWith("\"") && !leading_backslash) { // mit dem starten und davor nicht \\ hatten ?
						// end reading key
						last_key = info;
						info = "";
						status = STATUS_END_KEY;
						skipleer = true;
					}
					
					
				}else if(status == STATUS_END_KEY) {
					if(s.equals("\"")) {
						status = STATUS_START_VALUE;
						skipleer = true;
					}else {
						throw new IllegalArgumentException("An Key has to end with '\"'");
					}
				}else if(status == STATUS_START_VALUE) {
					if(s.equals(":")) {
						status = STATUS_READ_VALUE;
						
						// remove leading spaces and other shit
						data = skipleer(data);
						
						// try to find out new data	type
						
						int datatype = calcNewDataType(data);
						
						if(datatype != -1) {
							newdatatype = datatype;
						}else {
							throw new IllegalArgumentException("Couldn't find the Datatype of the key: "+last_key);
						}
						
						
					}else {
						throw new IllegalArgumentException("After the Key, ':' has to be there");
					}
				}else if(status == STATUS_READ_VALUE) {
					
					boolean end = false;
					
					
					if(newdatatype == DATA_TYPE_NUMBER || newdatatype == DATA_TYPE_NULL || newdatatype == DATA_TYPE_BOOLEAN) {
						//end if: , or } is following
						data = skipleer(data);
						if(data.startsWith(",") || data.startsWith("}")) {
						//if(s.equals(",") || s.equals("}")) { // wenn json ende oder: argument zu ende -> neuer key folgt
							end = true;
							info += s;
						}
						
					}else if(newdatatype == DATA_TYPE_STRING) {
						//end if: \" and not leading_backslash
						if(s.equals("\"") && !leading_backslash) {
							
							
							//System.err.println("info="+info);
							//System.err.println("data="+data);
							if(instring) {
								instring = false;
								info = info.split("",2)[1];
								end = true;
							}else {
								instring = true;
							}
						}
						
					}else if(newdatatype == DATA_TYPE_LIST) {
						//end if: ] and not in string -> count open '['
						
						if(!instring) {
							if(s.equals("[")) {
								open_brackets += 1;
							}
							
							if(s.equals("]")) { // possible end
								open_brackets -= 1;
								
							}
							if(open_brackets == 0) {
								end = true;
								info += s;
							}
						}
						
						
						if(s.equals("\"")) {
							if(!leading_backslash) {
								instring = !instring;
							}
						}
						
						
					}else if(newdatatype == DATA_TYPE_JSON) {
						// end if: } and not in string -> count open '{'
						
						if(!instring) {
							if(s.equals("{")) {
								open_brackets += 1;
							}
							
							if(s.equals("}")) { // possible end
								open_brackets -= 1;
								
							}
							if(open_brackets == 0) {
								end = true;
								info += s;
							}
						}
						
						
						if(s.equals("\"")) {
							if(!leading_backslash) {
								instring = !instring;
							}
						}
					}
					
					
					if(end) {
						
						Object obj = getObject(info, newdatatype);
						json.put(last_key, obj);
						last_key = "";
						info = "";
						status = STATUS_END_VALUE;
					}else {
						if(leading_backslash) {
							if(newdatatype == DATA_TYPE_LIST || newdatatype == DATA_TYPE_JSON) {
								info += "\\";
							}
						}
						info += s;
					}
					
				}else if(status == STATUS_END_VALUE) {
				//	System.out.println("End of Value: "+data);
					// TODO: Check if generell ending of json element or if starting again with key
					
					if(data.length() < 2) {
						break; // finished ?
					}else {
						status = STATUS_START_KEY;
						skipleer = true;
					}
					
				}
				
						
					
				if(skipleer) {
					data = skipleer(data);
				}

				leading_backslash = false;
			}
			
		}
		
		return json;
	}
	
	private static String skipleer(String data) {
		// https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
		
		
		while(data.startsWith(" ") || data.charAt(0) == 9 || data.charAt(0) == 10 || data.charAt(0) == 13 || data.charAt(0) == 32 || data.startsWith("\n") || data.startsWith("\r")) {
			if(data.length() < 2) break;
			
			data = data.substring(1);
		}
		
		return data;
	}
	
	
	private static Object getObject(String data, int dt) {
		
		if(dt == DATA_TYPE_BOOLEAN) {
			
			return Boolean.valueOf(data);
			
		}else if(dt == DATA_TYPE_STRING) {
		
			return (String) data;
			
		}else if(dt == DATA_TYPE_NUMBER) {
			
			try {
				int i = Integer.parseInt(data);
				return i;
			}catch(NumberFormatException pex) {
				Double d = Double.parseDouble(data);
				return d;
			}
			
			
			
		}else if(dt == DATA_TYPE_NULL) {
			return null;
			
		}else if(dt == DATA_TYPE_JSON) {
			return (new JSONParsing(data).createJSONObject());
			
		}else if(dt == DATA_TYPE_LIST) {
		//	System.err.println("DATA_TYPE_LIST: " + data);
			// uff
			// ["list_entry1","list_entry2","list_"entry3",["Opfer"]]
			boolean instring = false;
			boolean leading_backslash = false;
			int status = STATUS_START;
			int newdatatype = -1;
			int open_brackets = 0;
			String info = "";
			
			List<Object> objects = new ArrayList<>();
			
			
			while(data.length() > 0) {
			//	System.out.println("rd: " + data);
			
				String s = data.substring(0,1);
				
				data = data.substring(1);
				if(s.equals("\\")) {
					leading_backslash = true;
				}else {
					
					if(status == STATUS_START) {
						if(s.equals("[")) {
							
							if(data.startsWith("]")) {
								break; // empty list ... lol
							}else {
								status = STATUS_READ_VALUE;
								data = skipleer(data);
								newdatatype = calcNewDataType(data);
								if(newdatatype == -1) {
									throw new IllegalArgumentException("Couldnt find the Data-Type in the JSON-Array");
								}
							}
						}else {
							throw new IllegalArgumentException("An JSON-Array has to start with '['");
						}
					}else if(status == STATUS_READ_VALUE) {
						boolean end = false;
						if(newdatatype == DATA_TYPE_NUMBER || newdatatype == DATA_TYPE_NULL || newdatatype == DATA_TYPE_BOOLEAN) {
							//end if: , or } is following
							if(data.startsWith(",") || data.startsWith("]")) {
							//if(s.equals(",") || s.equals("}")) { // wenn json ende oder: argument zu ende -> neuer key folgt
								end = true;
								info += s;
							}
							
						}else if(newdatatype == DATA_TYPE_STRING) {
							//end if: \" and not leading_backslash
					//		System.out.println("str: s="+ s + " data="+data + " backslash="+leading_backslash);
							if(s.equals("\"") && !leading_backslash) {
								
								
								//System.err.println("info="+info);
								//System.err.println("data="+data);
								if(instring) {
									instring = false;
									info = info.split("",2)[1];
									end = true;
								}else {
									instring = true;
								}
							}
							
						}else if(newdatatype == DATA_TYPE_LIST) {
							//end if: ] and not in string -> count open '['
							
							if(!instring) {
								if(s.equals("[")) {
									open_brackets += 1;
								}
								
								if(s.equals("]")) { // possible end
									open_brackets -= 1;
									
								}
								if(open_brackets == 0) {
									end = true;
									info += s;
								}
							}
							
							
							if(s.equals("\"")) {
								if(!leading_backslash) {
									instring = !instring;
								}
							}
							
							
						}else if(newdatatype == DATA_TYPE_JSON) {
							// end if: } and not in string -> count open '{'
							
							if(!instring) {
								if(s.equals("{")) {
									open_brackets += 1;
								}
								
								if(s.equals("}")) { // possible end
									open_brackets -= 1;
									
								}
								if(open_brackets == 0) {
									end = true;
									info += s;
								}
							}
							
							
							if(s.equals("\"")) {
								if(!leading_backslash) {
									instring = !instring;
								}
							}
						}
						
						
						if(end) {
					//		System.out.println("end of value");
							Object obj = getObject(info, newdatatype);
							objects.add(obj);
							info = "";
							status = STATUS_END_VALUE;
							data = skipleer(data);
						}else {
							info += s;
						}
					}else if(status == STATUS_END_VALUE) {
						if(data.length() > 0) {
							status = STATUS_READ_VALUE;
						//	System.out.println("restdata: " + data);
						//	System.out.println("first char "+ ((int)data.charAt(0)));
							data = skipleer(data);
						//	System.out.println("first char "+ ((int)data.charAt(0)));
						//	boolean b = data.startsWith(" ") || data.charAt(0) == 9 || data.charAt(0) == 10 || data.charAt(0) == 13 || data.charAt(0) == 32 || data.startsWith("\n") || data.startsWith("\r");
						//	System.out.println("starts with skip: " + b);
						//	System.out.println("restdata2: " + data);
							newdatatype = calcNewDataType(data);
						}
					}
					
					leading_backslash = false;
				}
			}
			return objects;
		}
		
		//System.out.println("Translator: Type="+dt+", Data="+data);
		
		return null;
	}
	
	private static int calcNewDataType(String data) {
		if(data.startsWith("[")) {
			return DATA_TYPE_LIST;
		}else if(data.startsWith("{")) {
			return DATA_TYPE_JSON;
			
		}else if(data.startsWith("null")) {
			return DATA_TYPE_NULL;
		}else if(data.startsWith("\"")) {
			return DATA_TYPE_STRING;
		}else if(data.startsWith("true") || data.startsWith("false")) {
			return DATA_TYPE_BOOLEAN;
		}else if(data.startsWith("0") || data.startsWith("1") || data.startsWith("2") 
				|| data.startsWith("3") || data.startsWith("4") || data.startsWith("5") 
				|| data.startsWith("6") || data.startsWith("7") || data.startsWith("8")
			|| data.startsWith("9")) {
			
			return DATA_TYPE_NUMBER;
		}
		
		
		return -1;
	}
	
	
}
