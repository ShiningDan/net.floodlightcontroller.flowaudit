package net.floodlightcontroller.flowaudit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.floodlightcontroller.flowaudit.DataPacket.Flow;
import net.floodlightcontroller.flowaudit.DataPacket.Topology;

public class RequestAudit {
	Flow request;
	
	public RequestAudit(Flow request) {
		this.request = request;
	}
	
	public RequestAudit() {
		
	}

	
	

	
	
	//get flows that has overlaps with request
	public Flow matchFlow(List<Flow> flowlist) {
		
		
		Flow result = null;
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request in_ports are all not * and did not match
			if ((flowlist.get(i).in_port != null) && (request.in_port != null) && (!flowlist.get(i).in_port.equals(request.in_port))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request eth_src are all not * and did not match
			if ((flowlist.get(i).eth_src != null) && (request.eth_src != null) && (!flowlist.get(i).eth_src.equals(request.eth_src))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request eth_type are all not * and did not match
			if ((flowlist.get(i).eth_type != null) && (request.eth_type != null) && (!flowlist.get(i).eth_type.equals(request.eth_type))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request vlan_vid are all not * and did not match
			if ((flowlist.get(i).vlan_vid != null) && (request.vlan_vid != null) && (!flowlist.get(i).vlan_vid.equals(request.vlan_vid))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request vlan_priority are all not * and did not match
			if ((flowlist.get(i).vlan_priority != null) && (request.vlan_priority != null) && (!flowlist.get(i).vlan_priority.equals(request.vlan_priority))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request ip_proto are all not * and did not match
			if ((flowlist.get(i).ip_proto != null) && (request.ip_proto != null) && (!flowlist.get(i).ip_proto.equals(request.ip_proto))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request .ip_tos_bit are all not * and did not match
			if ((flowlist.get(i).ip_tos_bit != null) && (request.ip_tos_bit != null) && (!flowlist.get(i).ip_tos_bit.equals(request.ip_tos_bit))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request in_ports are all not * and did not match
			if ((flowlist.get(i).src_port != null) && (request.src_port != null) && (!flowlist.get(i).src_port.equals(request.src_port))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request in_ports are all not * and did not match
			if ((flowlist.get(i).dst_port != null) && (request.dst_port != null) && (!flowlist.get(i).dst_port.equals(request.dst_port))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request ipv4_src are all not * and did not match
			if ((flowlist.get(i).ipv4_src != null) && (request.ipv4_src != null) && (!flowlist.get(i).ipv4_src.isoverlap(request.ipv4_src))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		for (int i = 0; i < flowlist.size(); i++) {  // flowlist and request ipv4_dst are all not * and did not match
			if ((flowlist.get(i).ipv4_dst != null) && (request.ipv4_dst != null) && (!flowlist.get(i).ipv4_dst.isoverlap(request.ipv4_dst))) {
				flowlist.remove(i);
				--i;
			}
		}
		
		
		if(request != null) {
			flowlist.add(request);
		}
		
		if (flowlist.size() > 0) {
			result = flowlist.get(0);
			for (int i = 1; i < flowlist.size(); i++) {
				if (flowlist.get(i).priority > result.priority) {
					result = flowlist.get(i);
				}
			}
		}
		
		return result;
	}
}
