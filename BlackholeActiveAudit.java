package net.floodlightcontroller.flowaudit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.flowaudit.DataPacket.Flow;
import net.floodlightcontroller.flowaudit.DataPacket.Topology;

import java.util.ArrayList;
import java.util.List;

import org.json.*;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;



public class BlackholeActiveAudit extends SwitchResourceBase {
	
	protected static Logger log = 
			LoggerFactory.getLogger(BlackholeActiveAudit.class);
	
	
	@Get("json")
	public String retrieve(){
		return "this url is used for black hole active detect using POST, please send info";
	}
	
	
	/**
	 * Takes a request and check if the flow will be caught by black hole
	 * @param fmJson The  Request in JSON format.
	 * @return A string status message
	 * @throws Exception 
	 */
	@Post
	public String hasBlackhole(String fmJson) throws Exception{
		String result = "";
		result = fmJson;
		
		List<SwitchFlow> switchflows = new ArrayList<SwitchFlow>();
		
		try {
			JSONObject fmJSONObj = new JSONObject(fmJson);
			JSONArray flowEntries = fmJSONObj.getJSONArray("flowentries");
			for (int i = 0; i < flowEntries.length(); i++) {
				SwitchFlow sf = new SwitchFlow();
				Flow request = new Flow();
				sf.switchId = flowEntries.getJSONObject(i).getString("dpid");
				JSONObject matchJSONObj = flowEntries.getJSONObject(i).getJSONObject("match");
				request.addValue(matchJSONObj);
				request.priority = flowEntries.getJSONObject(i).getInt("priority");
				request.action = flowEntries.getJSONObject(i).getString("action");
				sf.flow = request;
				switchflows.add(sf);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "the input format do not match with JSON format";
		}
		
		// parse request
		List<Topology> topolist = getTopo("127.0.0.1", "8080");
		
		// start audit from dpid[0]
		int dpidIndex = 0;
		result = traverseToFindBlackHole(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist);
		
		return result;
	}
	
	private String traverseToFindBlackHole(List<SwitchFlow> switchflows, int dpidIndex, String srcSwitch, List<Topology> topolist) throws Exception{
		String result = "";
		
		List<Flow> flowlist = null;
		RequestAudit ra;
		Flow request;
		if (dpidIndex != -1) {
			flowlist = new FlowList(switchflows.get(dpidIndex).switchId).getFlowList();
			request = switchflows.get(dpidIndex).flow;
			ra = new RequestAudit(request);

		} else {
			flowlist = new FlowList(srcSwitch).getFlowList();
			request = (Flow)switchflows.get(0).flow.clone();
			request.priority = -1;
			ra = new RequestAudit(request);
		}
		
		
		// get flow match result
		Flow highPriFlow = ra.matchFlow(flowlist);
		
		if (highPriFlow.priority == -1) {
			System.out.println("--------------  no flow entry or request matches the flow on switch: " + srcSwitch + " -------------------");
			return "no flow entry or request matches the flow on switch: " + srcSwitch;
		}
		
		// if the return highPriFlow is request, then go on traverse dpid
		if (highPriFlow == request) {
			System.out.println("-------------------pass switch: " + switchflows.get(dpidIndex).switchId + " as request: " + request + "-----------------");
			
			// if request ends the flow
			if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
				
				String srcPort = highPriFlow.action.split("=")[1];
				
				for (int i = 0; i < topolist.size(); i++) {
					if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
						String nextSwitch = topolist.get(i).dst_switch;
						
						if (++dpidIndex < switchflows.size()) {
							if (nextSwitch.equals(switchflows.get(dpidIndex).switchId)) {
								return traverseToFindBlackHole(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist);
							}
						}
						
						return traverseToFindBlackHole(switchflows, -1, nextSwitch, topolist);
					}
				}
				
			}else {
				System.out.println("---------------flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end -----------------");
				return "flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end.";
			}
		}
		
		// if the switch has higher priority flow entry and return highPriFlow is not the request, then traverse follows the siwtch flow entry
		
		// check higher priority flow entry is drop or null
		if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
			
			String srcPort = highPriFlow.action.split("=")[1];
			
			for (int i = 0; i < topolist.size(); i++) {
				if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
					String nextSwitch = topolist.get(i).dst_switch;
					System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
					return traverseToFindBlackHole(switchflows, -1, nextSwitch, topolist);
				}
			}
			System.out.println("-------------------- flows follow by the switch: " + srcSwitch + " and the flow entry " + highPriFlow + " and end ---------------------");
			return "flows follow by the switch: " + srcSwitch + " and the flow entry " + highPriFlow + " and end.";
		} else {
			System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
			System.out.println("---------------------flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch + "-----------------");
			return "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch;
		}
	}
	
}