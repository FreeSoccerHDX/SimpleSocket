package de.freesoccerhdx.simplesocket.client;

public abstract class ClientListener {

	public abstract void recive(SimpleSocketClient ssc, String channel, String source_name, String message);
	
}
