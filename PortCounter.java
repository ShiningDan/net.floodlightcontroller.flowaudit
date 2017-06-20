package net.floodlightcontroller.flowaudit;

import org.json.JSONException;
import org.json.JSONObject;

public class PortCounter {

	String portNumber;
	int receivePackets;
	int transmitPackets;
	int receiveBytes;
	int transmitBytes;
	int receiveDropped;
	int transmitDropped;
	int receiveErrors;
	int transmitErrors;
	int receiveFrameErrors;
	int receiveOverrunErrors;
	int receiveCRCErrors;
	int collisions;
	int durationSec;
	int durationNsec;
	
	public PortCounter() {};
	
	public PortCounter(JSONObject pcLoop) throws JSONException {
		this.portNumber = pcLoop.getString("portNumber");
		this.receivePackets = pcLoop.getInt("receivePackets");
		this.transmitPackets = pcLoop.getInt("transmitPackets");
		this.receiveBytes = pcLoop.getInt("receiveBytes");
		this.transmitBytes = pcLoop.getInt("transmitBytes");
		this.receiveDropped = pcLoop.getInt("receiveDropped");
		this.transmitDropped = pcLoop.getInt("transmitDropped");
		this.receiveErrors = pcLoop.getInt("receiveErrors");
		this.transmitErrors = pcLoop.getInt("transmitErrors");
		this.receiveFrameErrors = pcLoop.getInt("receiveFrameErrors");
		this.receiveOverrunErrors = pcLoop.getInt("receiveOverrunErrors");
		this.receiveCRCErrors = pcLoop.getInt("receiveCRCErrors");
		this.collisions = pcLoop.getInt("collisions");
		this.durationSec = pcLoop.getInt("durationSec");
		this.durationNsec = pcLoop.getInt("durationNsec");
	}
}
