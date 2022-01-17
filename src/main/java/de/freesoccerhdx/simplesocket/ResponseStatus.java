package de.freesoccerhdx.simplesocket;

public enum ResponseStatus {

	SERVER_REACHED(0), // SERVER was reached and sends Message A) to Client back or B) to target C) nothing
	TARGET_REACHED(1), // Target was reached (Server or Client)
	TARGET_NOT_FOUND(2); // TARGET(=Client) was not found by Server

	private int id;
	
	ResponseStatus(int i) {
		id = i;
	}
	
	public int getID() {
		return id;
	}
	
	
}
