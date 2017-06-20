package net.floodlightcontroller.flowaudit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.flowaudit.DataPacket.Topology;

public class BlackholePassiveDetect extends SwitchResourceBase {

	protected static Logger log = 
			LoggerFactory.getLogger(BlackholePassiveDetect.class);
	
	@Get("json")
	public Map<String, Map<String, String>> retrieve(){
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		
		try {
			List<Topology> topolist = getTopo("127.0.0.1", "8080");
			List<String> dpidlist = this.getDpid("127.0.0.1", "8080");
			Map<String, PortCounter> portCounter = this.getPortCounter("127.0.0.1", "8080");
			Iterator<Entry<String, PortCounter>> iter = portCounter.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<String, PortCounter> entry = iter.next();
				String switchId = entry.getKey();
				PortCounter siwtchPortCounter = entry.getValue();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			return map;
		}
		
	}
	
}
