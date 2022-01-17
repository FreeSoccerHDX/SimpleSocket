package de.freesoccerhdx.simplesocket.client;

import de.freesoccerhdx.simplesocket.ResponseStatus;

public abstract class SocketResponse {

	public abstract void response(SimpleSocketClient ssc, ResponseStatus status, String who);
	
}
