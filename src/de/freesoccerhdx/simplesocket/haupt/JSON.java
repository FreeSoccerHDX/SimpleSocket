package de.freesoccerhdx.simplesocket.haupt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class JSON extends HashMap<String,Object> implements Map<String,Object>{

	public static JSON openJSONFile(File target) throws IOException {
		
		if(target != null) {
			if(target.exists()) {
				
				Scanner scan = new Scanner(target);
				
				try {
					
					String data = "";
					while(scan.hasNext()) {
						data += scan.nextLine();
					}
				
					JSON json = JSON.parseJSON(data);
					return json;
				}finally {
					scan.close();
				}
				
			}
		}
		
		
		return null;
	}
	
	
	
	
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


	public JSON(Map<String, Object> map) {
		super(map);
	}
	
	public JSON() {
		super();
	}
	
	public boolean saveToFile(File target) throws IOException {
		
		if(target != null) {
			if(!target.exists()) target.createNewFile();
			
			FileWriter writer = new FileWriter(target);
			
			try {
				writer.write(toJSON());
				return true;
			}finally{
				writer.close();
			}
			
			
		}
		
		return false;
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
	public static JSON parseJSON(String json) {
		return new JSONParsing(json).createJSONObject();
	}
	
}


























