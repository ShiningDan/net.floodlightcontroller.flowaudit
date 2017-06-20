package net.floodlightcontroller.flowaudit;

import java.util.Objects;

import net.floodlightcontroller.flowaudit.DataPacket.Flow;

public class SwitchFlow {
	
	String switchId;
	Flow flow;
	
	public SwitchFlow() {}
	
	public SwitchFlow(String switchId, Flow flow) {
		this.switchId = switchId;
		this.flow = flow;
	}
	
	public boolean equals (Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		SwitchFlow other = (SwitchFlow) obj;
		if (Objects.equals(this.switchId, other.switchId) && Objects.equals(this.flow, other.flow)) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		return "SwitchFlow: { SwitchID: " + this.switchId + ", Flow: " + this.flow +  " }";
	}

}
