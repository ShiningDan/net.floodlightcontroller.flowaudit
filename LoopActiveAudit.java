package net.floodlightcontroller.flowaudit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.flowaudit.DataPacket.Flow;
import net.floodlightcontroller.flowaudit.DataPacket.Topology;

public class LoopActiveAudit extends SwitchResourceBase {
	
	public static long bTime = 0;
	public static long eTime = 0;
	
	private static final String httpPort = "httpPort";
	
	protected static Logger log = 
			LoggerFactory.getLogger(LoopActiveAudit.class);
	
	@Get("json")
	public String retrieve(){
		return "this url is used for loop active detect using POST, please send info";
	}
	
	/**
	 * Takes a request and check if the flow will be caught by black hole
	 * @param fmJson The  Request in JSON format.
	 * @return A string status message
	 * @throws Exception 
	 */
	@Post
	public String hasLoop(String fmJson) throws Exception{
		
//		String result = "";
		JSONObject result = null;
//		result = fmJson;
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
		List<Topology> topolist = getTopo("127.0.0.1");
		
		// start audit from dpid[0]
		int dpidIndex = 0;
		List<SwitchFlow> routes = new ArrayList<SwitchFlow>();
		
		
		result = this.traverseToFindLoop(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist, routes);
		System.out.println(result.toString());
		return result.toString();
	}
	
	private JSONObject traverseToFindLoop (List<SwitchFlow> switchflows, int dpidIndex, String srcSwitch, List<Topology> topolist, List<SwitchFlow> routes) throws Exception{
//		String result = "";
		JSONObject result = new JSONObject();
		
		if (bTime == 0) {
			bTime = System.nanoTime();
		} else {
			eTime = System.nanoTime();
			System.out.println("time to cal is: " + (eTime - bTime));
			bTime = 0;
			eTime = 0;
		}
		
		List<Flow> flowlist = null;
		RequestAudit ra;
		Flow request;
		SwitchFlow highPriSwitchflow = new SwitchFlow();
		if (dpidIndex != -1) {
			flowlist = new FlowList(switchflows.get(dpidIndex).switchId, "127.0.0.1", port).getFlowList();
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
			flowlist = new FlowList(srcSwitch, "127.0.0.1", port).getFlowList();
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
		
//		System.out.println("-----------------------");
//		System.out.println("flowlist:" + flowlist);
//		System.out.println("-----------------------");
		
		System.out.println("-----------------------");
		System.out.println("routes:" + routes);
		System.out.println("-----------------------");
		
		// get flow match result
		Flow highPriFlow = ra.matchFlow(flowlist);
		
		if (highPriFlow.priority == -1) {
			System.out.println("--------------  no flow entry or request matches the flow on switch: " + srcSwitch + " -------------------");
			routes.add(highPriSwitchflow);
			result.put("result", false);
			result.put("reason",  "no flow entry or request matches the flow on switch: " + srcSwitch);
			result.put("routes", routes.toString());
			return result;
//			return "no flow entry or request matches the flow on switch: " + srcSwitch;
		}
		
		highPriSwitchflow.flow = highPriFlow;
		
		
		// if the return highPriFlow is request, then go on traverse dpid
		if (highPriFlow == request) {
			System.out.println("-------------------pass switch: " + switchflows.get(dpidIndex).switchId + " as request: " + request + "-----------------");
			
			// check in the routes
			if (routes.contains(highPriSwitchflow)) {
				routes.add(highPriSwitchflow);
				System.out.println("----------------- flows follow by the switch " + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop ----------------");
				result.put("result", true);
				result.put("reason", "flows follow by the switch " + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop");
				result.put("routes", routes.toString());
				return result;
//				return ("flows follow by the swich" + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop : " + routes);
			}
			// highPriSwitchflow not in the routes
			routes.add(highPriSwitchflow);
			
			// if request ends the flow
			if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
				
				String srcPort = highPriFlow.action.split("=")[1];
				
				for (int i = 0; i < topolist.size(); i++) {
					if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
						// get next switch
						String nextSwitch = topolist.get(i).dst_switch;
						
						if (++dpidIndex < switchflows.size()) {
							if (nextSwitch.equals(switchflows.get(dpidIndex).switchId)) {
								// if the nextSwitch is the same with the next switch as request
								
								return traverseToFindLoop(switchflows, dpidIndex, switchflows.get(dpidIndex).switchId, topolist, routes);
							}
						}
						// all the request switches has been transmitted, or the nextSwitch is not the next switch as request
						
						return traverseToFindLoop(switchflows, -1, nextSwitch, topolist, routes);
					}
				}
				
				System.out.println("---------------flows follow by the switch : " + srcSwitch + " and the request: " + highPriFlow + " and may leads to a host ---------------");
				result.put("result", false);
				result.put("reason", "flows follow by the switch : " + srcSwitch + " and the request: " + highPriFlow + " and may leads to a host");
				result.put("routes", routes.toString());
				return result;
//				return ("flows follow by the switch : " + srcSwitch + " and the request: " + highPriFlow + " and may leads to a host");
				
			} else {
				System.out.println("---------------flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end -----------------");
				result.put("result", false);
				result.put("reason", "flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end.");
				result.put("routes", routes.toString());
				return result;
				
//				return "flows follow by the switch : " + srcSwitch + " and the request: " + request + " and end.";
			}
		}
			
		// if the return highPriFlow is not the request, follow the highPriFlow leads
		
		System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
		// check in the routes
		if (routes.contains(highPriSwitchflow)) {
			routes.add(highPriSwitchflow);
			System.out.println("----------------- flows follow by the switch " + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop ----------------");
			result.put("result", true);
			result.put("reason", "flows follow by the switch " + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop");
			result.put("routes", routes.toString());
			return result;
			
//			return ("flows follow by the swich" + highPriSwitchflow.switchId + " and request: " + highPriSwitchflow.flow + "and catch in a loop : " + routes);
		}
		// highPriSwitchflow not in the routes
		routes.add(highPriSwitchflow);
		
		if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
			
			String srcPort = highPriFlow.action.split("=")[1];
			
			for (int i = 0; i < topolist.size(); i++) {
				if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
					String nextSwitch = topolist.get(i).dst_switch;
					
					return traverseToFindLoop(switchflows, -1, nextSwitch, topolist, routes);
				}
			}
			
			System.out.println("---------------flows follow by the switch : " + srcSwitch + " and the flowentry : " + highPriFlow + " and may leads to a host ---------------");
			result.put("result", false);
			result.put("reason", "flows follow by the switch : " + srcSwitch + " and the flowentry: " + highPriFlow + " and may leads to a host");
			result.put("routes", routes.toString());
			return result;
			
//			return ("flows follow by the switch : " + srcSwitch + " and the flowentry: " + highPriFlow + " and may leads to a host");
			
		} else {
			
			System.out.println("---------------------flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch + "-----------------");
			result.put("result", false);
			result.put("reason", "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch);
			result.put("routes", routes.toString());
			return result;
			
//			return "flows end with a black hole and the cause flow entry is : " + highPriFlow + "on the switch: " + srcSwitch;
		}
			
			
			
//			// check if the switch flow tumple passed before
//			boolean loopFlag = false;
//			for (int i = 0; i < routes.size(); i++) {
//				if (routes.get(i).equals(route)) {
//					loopFlag = true;
//					break;
//				}
//			}
//			routes.add(route);
//			if (loopFlag) {
//				System.out.println("--------------- loop detect, the routes of loop is : " + routes + "--------------------");
//				return "flows follow by the routes: " + route + " and get into loops";
//			}
//			++dpidIndex;
//			
//			// if loop not detect, go on traverse dpid
//			if (dpidIndex < dpid.size()) {
//				
//				// traverse next dpid
//				return traverseToFindLoop(dpid, dpidIndex, request, dpid.get(dpidIndex), topolist, routes);
//			} else {
//				
//				return "flows follow by the request : " + request + " and end.";
//			}
//		}
		
		// if the switch has higher priority flow entry and return highPriFlow is not the request, then traverse follows the siwtch flow entry
		
//		if (highPriFlow.action != null && highPriFlow.action.contains("output")) {
//			
//			System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
//			
//			// check if the switch flow tumple has passed before
//			boolean loopFlag = false;
//			for (int j = 0; j < routes.size(); j++) {
//				if (routes.get(j).equals(route)) {
//					loopFlag = true;
//					break;
//				}
//			}
//			routes.add(route);
//			if (loopFlag) {
//				System.out.println("--------------- loop detect, the routes of loop is : " + routes + "--------------------");
//				return "flows follow by the routes: " + route + " and get into loops";
//			}
//			
//			String srcPort = highPriFlow.action.split("=")[1];
//			
//			for (int i = 0; i < topolist.size(); i++) {
//				if ((topolist.get(i).src_switch.equals(srcSwitch)) && (topolist.get(i).src_port.equals(srcPort))) {
//					String nextSwitch = topolist.get(i).dst_switch;
//					
//					return traverseToFindLoop(dpid, -1, request, nextSwitch, topolist, routes);
//				}
//			}
//		
//			return "flows follow by the flow entry : " + highPriFlow + " and end.";
//		} else {
//			System.out.println("-------------------pass switch: " + srcSwitch + " as flow entry: " + highPriFlow + "-----------------");
//			return "flows end with a black hole and the cause flow entry is : " + highPriFlow;
//		}
		
	}

}
