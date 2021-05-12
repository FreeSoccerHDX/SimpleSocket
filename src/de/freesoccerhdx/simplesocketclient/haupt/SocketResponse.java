package de.freesoccerhdx.simplesocketclient.haupt;

public abstract class SocketResponse {

	public abstract void response(SimpleSocketClient ssc, ResponseStatus status, String source_client_name);
	
}
