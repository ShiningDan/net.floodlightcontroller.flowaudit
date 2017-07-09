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
//		String result = "";
//		result = fmJson;
		
		JSONObject result = null;
		
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

		List<SwitchFlow> routes = new ArrayList<SwitchFlow>();
		result = traverseToFindBlackHole(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist, routes);
		
		return result.toString();
	}
	
	private JSONObject traverseToFindBlackHole(List<SwitchFlow> switchflows, int dpidIndex, String srcSwitch, List<Topology> topolist, List<SwitchFlow> routes) throws Exception{
//		String result = "";
		
		JSONObject result = new JSONObject();
		
		List<Flow> flowlist = null;
		RequestAudit ra;
		Flow request;
		SwitchFlow highPriSwitchflow = new SwitchFlow();
		if (dpidIndex != -1) {
			flowlist = new FlowList(switchflows.get(dpidIndex).switchId).getFlowList();
			// add routes exist flows
			for (int i = 0; i < routes.size(); i++) {
				if (routes.get(i).switchId.equals(switchflows.get(dpidIndex).switchId)) {
					flowlist.add(routes.get(i).flow);
				}
			}
			request = switchflows.get(dpidIndex).flow;
			ra = new RequestAudit(request);
			highPriSwitchflow.switchId = switchflows.get(dpidIndex).switchId;

		} else {
			flowlist = new FlowList(srcSwitch).getFlowList();
			// add routes exist flows
			for (int i = 0; i < routes.size(); i++) {
				if (routes.get(i).switchId.equals(srcSwitch)) {
					flowlist.add(routes.get(i).flow);
				}
			}
			request = (Flow)switchflows.get(0).flow.clone();
			request.priority = -1;
			for (int i = 0; i < switchflows.size(); i++) {
				if (switchflows.get(i).switchId.equals(srcSwitch)) {
					request = (Flow)switchflows.get(i).flow.clone();
					dpidIndex = i;
					break;
				}
			}
			ra = new RequestAudit(request);
			highPriSwitchflow.switchId = srcSwitch;
		}
		
		
		// get flow match result
		Flow highPriFlow = ra.matchFlow(flowlist);
		
		if (highPriFlow.priority == -1) {
			System.out.println("--------------  no flow entry or request matches the flow on switch: " + srcSwitch + " -------------------");
			routes.add(highPriSwitchflow);
			result.put("result", false);
			result.put("reason", "no flow entry or request matches the flow on switch: " + srcSwitch);
			result.put("routes", routes.toString());
			return result;
			
//			return "no flow entry or request matches the flow on switch: " + srcSwitch;
		}
		
		highPriSwitchflow.flow = highPriFlow;
		
		// if the return highPriFlow is request, then go on traverse dpid
		if (highPriFlow == request) {
			System.out.println("-------------------pass switch: " + switchflows.get(dpidIndex).switchId + " as request: " + request + "-----------------");
			
			routes.add(highPriSwitchflow);
			
			// if request ends the flow
			if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
				
				String srcPort = highPriFlow.action.split("=")[1];
				
				for (int i = 0; i < topolist.size(); i++) {
					if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
						String nextSwitch = topolist.get(i).dst_switch;
						
						if (++dpidIndex < switchflows.size()) {
							if (nextSwitch.equals(switchflows.get(dpidIndex).switchId)) {
								return traverseToFindBlackHole(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist, routes);
							}
						}
						
						return traverseToFindBlackHole(switchflows, -1, nextSwitch, topolist, routes);
					}
				}
				
			}else {
				System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
				System.out.println("---------------------flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch + "-----------------");
				System.out.println(routes);
				result.put("result", true);
				result.put("reason", "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch);
				result.put("routes", routes.toString());
				return result;
				
//				return "flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end.";
			}
		}
		
		// if the switch has higher priority flow entry and return highPriFlow is not the request, then traverse follows the siwtch flow entry
		
		// check higher priority flow entry is drop or null
		routes.add(highPriSwitchflow);
		
		if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
			
			String srcPort = highPriFlow.action.split("=")[1];
			
			for (int i = 0; i < topolist.size(); i++) {
				if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
					String nextSwitch = topolist.get(i).dst_switch;
					System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
					return traverseToFindBlackHole(switchflows, -1, nextSwitch, topolist, routes);
				}
			}
			System.out.println("-------------------- flows follow by the switch: " + srcSwitch + " and the flow entry " + highPriFlow + " and end ---------------------");
			result.put("result", false);
			result.put("reason", "flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end.");
			result.put("routes", routes.toString());
			return result;
//			return "flows follow by the switch: " + srcSwitch + " and the flow entry " + highPriFlow + " and end.";
		} else {
			System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
			System.out.println("---------------------flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch + "-----------------");
			
			result.put("result", true);
			result.put("reason", "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch);
			result.put("routes", routes.toString());
			return result;
//			return "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch;
		}
	}
	
}