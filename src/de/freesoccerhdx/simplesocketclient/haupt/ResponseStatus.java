package de.freesoccerhdx.simplesocketclient.haupt;

public enum ResponseStatus {
	

	SERVER_REACHED(0), // SERVER was reached and sends Message A) to Client back or B) to target C) nothing
	TARGET_REACHED(1); // Target was reached (Server or Client)


	
	
	private int id;
	
	ResponseStatus(int i) {
		id = i;
	}
	
	public int getID() {
		return id;
	}
	
	
}
