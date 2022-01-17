package de.freesoccerhdx.simplesocket;



import de.freesoccerhdx.simplesocket.client.SimpleSocketClient;
import de.freesoccerhdx.simplesocket.server.SimpleSocketServer;

public class SimpleSocket {

	public static void main(String[] args) {
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
