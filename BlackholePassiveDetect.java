package net.floodlightcontroller.flowaudit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

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
	
	private static Stack<Map<Topology, Double>> linkLossHistory = new Stack<Map<Topology, Double>>();
	private static Stack<Map<String, Double>> switchLossHistory = new Stack<Map<String, Double>>();

	protected static Logger log = 
			LoggerFactory.getLogger(BlackholePassiveDetect.class);
	
	@Get("json")
	public Map<String, Map<String, String>> retrieve(){
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
						linkloss = (linkInputPacketsGrowth - linkOutputPacketsGrowth) / linkInputPacketsGrowth;
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
			
			System.out.println(prevLinkInputPackets );
			System.out.println(prevLinkOutputPackets );
			System.out.println(linkLossHistory.size() );
			
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
						for (int i = 0; i < topolist.size(); i++) {
							Topology topo = topolist.get(i);
							if (topo.src_port.equals(portNumber) && topo.src_switch.equals(switchId)) {
								switchInputPackets += pc.receivePackets;
								switchOutputPackets += pc.transmitPackets;
								break;
							}
						}
					}
				}
//				System.out.println(switchId + " switchInputPackets: " + switchInputPackets + " switchOutputPackets: " + switchOutputPackets);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			return map;
		}
		
	}
	
	public static void addToLinkLossHistory (Map<Topology, Double> linkLoss) {
		// linkLossHistory size is 10
		if (linkLossHistory.size() < 2 ) {
			linkLossHistory.push(linkLoss);
		} else {
			linkLossHistory.pop();
			linkLossHistory.push(linkLoss);
		}
	}
	
	public static void addToSwitchLossHistory (Map<String, Double> switchLoss) {
		// switchLossHistory size is 10
		if (switchLossHistory.size() <10 ) {
			switchLossHistory.push(switchLoss);
		} else {
			switchLossHistory.pop();
			switchLossHistory.push(switchLoss);
		}
	}
	
}
