package net.floodlightcontroller.flowaudit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Stack;

import org.json.JSONObject;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.flowaudit.DataPacket.Topology;
import net.floodlightcontroller.routing.Link;

public class BlackholePassiveDetect extends SwitchResourceBase {
	
	private static Map<String, Map<String, Integer>> prevLinkInputPackets = new HashMap<String, Map<String, Integer>>();
	private static Map<String, Map<String, Integer>> prevLinkOutputPackets = new HashMap<String, Map<String, Integer>>();
	private static Map<String, Integer> prevSwitchInputPackets = new HashMap<String, Integer>();
	private static Map<String, Integer> prevSwitchOutputPackets = new HashMap<String, Integer>();
	
	private static Queue<Map<Topology, Double>> linkLossHistory = new LinkedList<Map<Topology, Double>>();
	private static Queue<Map<String, Double>> switchLossHistory = new LinkedList<Map<String, Double>>();

	protected static Logger log = 
			LoggerFactory.getLogger(BlackholePassiveDetect.class);
	
	@Get("json")
	public String retrieve(){
		JSONObject result = new JSONObject();
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		
		try {
			List<Topology> topolist = getTopo("127.0.0.1", "8080");
			List<String> dpidlist = this.getDpid("127.0.0.1", "8080");
			Map<String, Map<String, PortCounter>> portCounter = this.getPortCounter("127.0.0.1", "8080");
//			System.out.println(topolist);
//			System.out.println(dpidlist);
//			System.out.println(portCounter);
			Map<Topology, Double> linkLoss = new HashMap<Topology, Double>();
			Map<String, Double> switchLoss = new HashMap<String, Double>();
			
			for (int i = 0; i < topolist.size(); i++) {
				Topology topo = topolist.get(i);
				int linkInputPackets = portCounter.get(topo.src_switch).get(topo.src_port).transmitPackets ;
				int linkOutputPackets = portCounter.get(topo.dst_switch).get(topo.dst_port).receivePackets ;
				
				double linkloss;
				
				int prevInputPackets = 0;
				if (prevLinkInputPackets.containsKey(topo.src_switch)) {
					
					Map<String, Integer> linksInputPackets = prevLinkInputPackets.get(topo.src_switch);
					if (linksInputPackets.containsKey(topo.src_port)) {
						prevInputPackets = linksInputPackets.get(topo.src_port);
						linksInputPackets.put(topo.src_port, linkInputPackets);
					} else {
						linksInputPackets.put(topo.src_port, linkInputPackets);
					}
				} else {
					
					prevLinkInputPackets.put(topo.src_switch, new HashMap<String, Integer>());
					prevLinkInputPackets.get(topo.src_switch).put(topo.src_port, linkInputPackets);
				}
				
				int prevOutputPackets = 0;
				if (prevLinkOutputPackets.containsKey(topo.src_switch)) {
					
					Map<String, Integer> linksOutputPackets = prevLinkOutputPackets.get(topo.src_switch);
					if (linksOutputPackets.containsKey(topo.src_port)) {
						prevOutputPackets = linksOutputPackets.get(topo.src_port);
						linksOutputPackets.put(topo.src_port, linkOutputPackets);
					} else {
						linksOutputPackets.put(topo.src_port, linkOutputPackets);
					}
				} else {
					
					prevLinkOutputPackets.put(topo.src_switch, new HashMap<String, Integer>());
					prevLinkOutputPackets.get(topo.src_switch).put(topo.src_port, linkOutputPackets);
				}
				
				
				if (prevInputPackets == 0 && prevOutputPackets == 0) {
					linkloss = 0.0;
				} else {
					int linkInputPacketsGrowth = linkInputPackets - prevInputPackets;
					int linkOutputPacketsGrowth = linkOutputPackets - prevOutputPackets;
					if (linkInputPacketsGrowth != 0) {
						linkloss = (float)(linkInputPacketsGrowth - linkOutputPacketsGrowth) / (float)linkInputPacketsGrowth;
					} else {
						linkloss = 0.0;
					}
					
				}
				
				if (linkloss < 0) {
					linkloss = 0.0;
				}
				linkLoss.put(topo, linkloss);
			}
			
			addToLinkLossHistory(linkLoss);		
			
			Iterator<Entry<String, Map<String, PortCounter>>> iter = portCounter.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<String, Map<String, PortCounter>> entry = iter.next();
				String switchId = entry.getKey();
				Iterator<Entry<String, PortCounter>> iterSwitchPC = entry.getValue().entrySet().iterator();
				int switchInputPackets = 0, switchOutputPackets = 0;
				while(iterSwitchPC.hasNext()) {
					Entry<String, PortCounter> switchPCEntry = iterSwitchPC.next();
					String portNumber = switchPCEntry.getKey();
					if (!portNumber.equals("local")) {
						PortCounter pc = switchPCEntry.getValue();
						// lldp packet should be removed
//						for (int i = 0; i < topolist.size(); i++) {
//							Topology topo = topolist.get(i);
//							if (topo.src_port.equals(portNumber) && topo.src_switch.equals(switchId)) {
								switchInputPackets += pc.receivePackets;
								switchOutputPackets += pc.transmitPackets;
//								break;
//							}
//						}
					}
				}
				System.out.println(switchId + " switchInputPackets: " + switchInputPackets + " switchOutputPackets: " + switchOutputPackets);
				
				int prevInputPackets = 0;
				if (prevSwitchInputPackets.containsKey(switchId)) {
					prevInputPackets = prevSwitchInputPackets.get(switchId);
					prevSwitchInputPackets.put(switchId, switchInputPackets);
				} else {
					prevSwitchInputPackets.put(switchId, switchInputPackets);
				}
				int prevOutputPackets = 0;
				if (prevSwitchOutputPackets.containsKey(switchId)) {
					prevOutputPackets = prevSwitchOutputPackets.get(switchId);
					prevSwitchOutputPackets.put(switchId, switchOutputPackets);
				} else {
					prevSwitchOutputPackets.put(switchId, switchOutputPackets);
				}
				
				double switchloss = 0.0;
				if (prevInputPackets == 0 && prevOutputPackets == 0) {
					switchloss = 0.0;
				} else {
					int switchInputPacketsGrowth = switchInputPackets - prevInputPackets;
					int switchOutputPacketsGrowth = switchOutputPackets - prevOutputPackets;
					if (switchInputPacketsGrowth == 0) {
						switchloss = 0.0;
					} else {
//						System.out.println(switchInputPacketsGrowth + " " + switchOutputPacketsGrowth);
						switchloss = (float)(switchInputPacketsGrowth - switchOutputPacketsGrowth) / (float)switchInputPacketsGrowth;
//						System.out.println(switchloss);
					}
				}
				if (switchloss < 0) {
					// if input < output, this may cause by lldp
					switchloss = 0.0;
				}
				switchLoss.put(switchId, switchloss);
			}
			addToSwitchLossHistory(switchLoss);
			
			System.out.println(prevSwitchInputPackets);
			System.out.println(prevSwitchOutputPackets);
			System.out.println(switchLossHistory);
			System.out.println("----------------------------------------");
			System.out.println(prevLinkInputPackets);
			System.out.println(prevLinkOutputPackets);
			System.out.println(linkLossHistory);
			
			result.put("switchLoss", switchLossHistory.toString());
			result.put("linkLoss", linkLossHistory.toString());
			System.out.println(result.toString());
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			return result.toString();
		}
		
	}
	
	public static void addToLinkLossHistory (Map<Topology, Double> linkLoss) {
		// linkLossHistory size is 10
		if (linkLossHistory.size() < 2 ) {
			linkLossHistory.add(linkLoss);
		} else {
			linkLossHistory.remove();
			linkLossHistory.add(linkLoss);
		}
	}
	
	public static void addToSwitchLossHistory (Map<String, Double> switchLoss) {
		// switchLossHistory size is 10
		if (switchLossHistory.size() <2 ) {
			switchLossHistory.add(switchLoss);
		} else {
			switchLossHistory.remove();
			switchLossHistory.add(switchLoss);
		}
	}
	
}
