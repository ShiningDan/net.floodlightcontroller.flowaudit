package net.floodlightcontroller.flowaudit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.flowaudit.DataPacket.DataItem;
import net.floodlightcontroller.flowaudit.DataPacket.Topology;

public class BlackholePassiveDetec extends SwitchResourceBase {

	protected static Logger log = 
			LoggerFactory.getLogger(BlackholePassiveDetec.class);
	
	public static double DISCARD_SHRESHOLD = 0.4;
	
	@Get("json")
	public Map<String, Map<String, String>> retrieve(){
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		try {
			List<Topology> topolist = getTopo("127.0.0.1");
			List<String> dpidlist = this.getDpid("127.0.0.1");
			List<String> sflowMetric = this.getSFlowMetric("127.0.0.1");
			List<DataItem> itemlist = this.getItem(this.getIndex(sflowMetric), sflowMetric);
			for (int i = 0; i < dpidlist.size(); i++) {
				Map<String, String> IDmap = new HashMap<String, String>();
				double sumInpkts = 0;
				double sumOutpkts = 0;
				for (int j = 0; j < itemlist.size(); j++) {
					if (itemlist.get(j).dpid.equals(dpidlist.get(i))) {
						// find the port of this switch
						
						String dstDpid = null;
						String dstPort = null;
						String port = itemlist.get(j).port;
						if (itemlist.get(j).ifinpkts > 5) {
							sumInpkts = sumInpkts + itemlist.get(j).ifinpkts;
						}
						if (itemlist.get(j).ifoutpkts > 5) {
							sumOutpkts = sumOutpkts + itemlist.get(j).ifoutpkts;
						}
						double ifoutpkts = itemlist.get(j).ifoutpkts;
						double dstIfinpkts = -1;
						for (int k = 0; k < topolist.size(); k++) {
							// find the port against this port
							
							if ((topolist.get(k).src_switch.equals(dpidlist.get(i))) && (topolist.get(k).src_port.equals(port))) {
								dstDpid = topolist.get(k).dst_switch;
								dstPort = topolist.get(k).dst_port;
							}
						}
						
						for (int k = 0; k < itemlist.size(); k++) {
							if ((itemlist.get(k).dpid.equals(dstDpid)) && (itemlist.get(k).port.equals(dstPort))) {
								dstIfinpkts = itemlist.get(k).ifinpkts;
								break;
							}
						}
						
						itemlist.get(j).portdiscard = 1 - dstIfinpkts/ifoutpkts;
						if (dstIfinpkts == -1) {
							IDmap.put(port, "this port might be a link to host.");
						} else if ((itemlist.get(j).ifoutpkts == 0) && (dstIfinpkts == 0)) {
							IDmap.put(port, "no packet pass.");
						} else if (itemlist.get(j).portdiscard < this.DISCARD_SHRESHOLD) {
							IDmap.put(port, Double.toString(itemlist.get(j).portdiscard));
							IDmap.put(port + ":dstIfinpkts", String.valueOf(dstIfinpkts));
							IDmap.put(port + ":ifoutpkts", String.valueOf(ifoutpkts));
						} else {
							IDmap.put(port, itemlist.get(j).portdiscard + ": backhole");
							IDmap.put(port + ":dstIfinpkts", String.valueOf(dstIfinpkts));
							IDmap.put(port + ":ifoutpkts", String.valueOf(ifoutpkts));
						}
					}
				}
				
				if ((sumInpkts == 0) && (sumOutpkts == 0)) {
					IDmap.put("switch_discard", "no flow on switch");
				} else {
					IDmap.put("switch_discard", Double.toString(1 - sumOutpkts/sumInpkts));
					IDmap.put("sumOutpkts", String.valueOf(sumOutpkts));
					IDmap.put("sumInpkts", String.valueOf(sumInpkts));
				}
				map.put(dpidlist.get(i), IDmap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return map;
		}
		
		
		
		
//		return "this url is used for black hole active detect using POST, please send info";
	}
	
	
}
