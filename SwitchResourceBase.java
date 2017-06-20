/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.flowaudit;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.flowaudit.DataPacket.DataItem;
import net.floodlightcontroller.flowaudit.DataPacket.Topology;

import org.json.JSONArray;
import org.json.JSONObject;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.ver13.OFMeterSerializerVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.protocol.OFFeaturesRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Base class for server resources related to switches
 * @author readams
 *
 */
public class SwitchResourceBase extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(SwitchResourceBase.class);

	public enum REQUESTTYPE {
		OFSTATS,
		OFFEATURES
	}

	@Override
	protected void doInit() throws ResourceException {
		super.doInit();

	}

	/**
	 * Use for requests that originate from the REST server that use their context to get a
	 * reference to the switch service.
	 * @param switchId
	 * @param statType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<OFStatsReply> getSwitchStatistics(DatapathId switchId,
			OFStatsType statType) {
		IOFSwitchService switchService = (IOFSwitchService) getContext().getAttributes().get(IOFSwitchService.class.getCanonicalName());

		IOFSwitch sw = switchService.getSwitch(switchId);
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;
		Match match;
		if (sw != null) {
			OFStatsRequest<?> req = null;
			switch (statType) {
			case FLOW:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildFlowStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case AGGREGATE:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildAggregateStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case PORT:
				req = sw.getOFFactory().buildPortStatsRequest()
				.setPortNo(OFPort.ANY)
				.build();
				break;
			case QUEUE:
				req = sw.getOFFactory().buildQueueStatsRequest()
				.setPortNo(OFPort.ANY)
				.setQueueId(UnsignedLong.MAX_VALUE.longValue())
				.build();
				break;
			case DESC:
				// pass - nothing todo besides set the type above
				req = sw.getOFFactory().buildDescStatsRequest()
				.build();
				break;
			case GROUP:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupStatsRequest()				
							.build();
				}
				break;

			case METER:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterStatsRequest()
							.setMeterId(OFMeterSerializerVer13.ALL_VAL)
							.build();
				}
				break;

			case GROUP_DESC:			
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupDescStatsRequest()			
							.build();
				}
				break;

			case GROUP_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupFeaturesStatsRequest()
							.build();
				}
				break;

			case METER_CONFIG:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterConfigStatsRequest()
							.build();
				}
				break;

			case METER_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterFeaturesStatsRequest()
							.build();
				}
				break;

			case TABLE:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableStatsRequest()
							.build();
				}
				break;

			case TABLE_FEATURES:	
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableFeaturesStatsRequest()
							.build();		
				}
				break;
			case PORT_DESC:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildPortDescStatsRequest()
							.build();
				}
				break;
			case EXPERIMENTER: //TODO @Ryan support new OF1.1+ stats types			
			default:
				log.error("Stats Request Type {} not implemented yet", statType.name());
				break;
			}

			try {
				if (req != null) {
					future = sw.writeStatsRequest(req);
					values = (List<OFStatsReply>) future.get(10, TimeUnit.SECONDS);
				}
			} catch (Exception e) {
				log.error("Failure retrieving statistics from switch " + sw, e);
			}
		}
		return values;
	}

	protected List<OFStatsReply> getSwitchStatistics(String switchId, OFStatsType statType) {
		return getSwitchStatistics(DatapathId.of(switchId), statType);
	}

	protected OFFeaturesReply getSwitchFeaturesReply(DatapathId switchId) {
		IOFSwitchService switchService =
				(IOFSwitchService) getContext().getAttributes().
				get(IOFSwitchService.class.getCanonicalName());

		IOFSwitch sw = switchService.getSwitch(switchId);
		Future<OFFeaturesReply> future;
		OFFeaturesReply featuresReply = null;
		OFFeaturesRequest featuresRequest = sw.getOFFactory().buildFeaturesRequest().build();
		if (sw != null) {
			try {
				future = sw.writeRequest(featuresRequest);
				featuresReply = future.get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.error("Failure getting features reply from switch" + sw, e);
			}
		}

		return featuresReply;
	}

	protected OFFeaturesReply getSwitchFeaturesReply(String switchId) {
		return getSwitchFeaturesReply(DatapathId.of(switchId));
	}	
	
	
	// get port count
	public static Map<String, PortCounter> getPortCounter(String ip, String port) {
		
		try {
			Map<String, PortCounter> switchPortCounter= new HashMap<String, PortCounter>();
			InputStreamReader intopo = new InputStreamReader(new URL("http://" + ip + ":" + port + "/wm/core/switch/all/port/json").openStream());
			StringBuilder input = new StringBuilder();
			int ch;
			while((ch = intopo.read()) != -1) {
				input.append((char) ch);
			}
			String jsonText = input.toString();
			JSONObject json = new JSONObject(jsonText);
			Iterator<String> iter = json.keys();
			while(iter.hasNext()) {
				String switchId = iter.next();
				JSONArray switchCounter = json.getJSONObject(switchId).getJSONArray("port_reply");
				for (int i = 0; i < switchCounter.length(); i++) {
					JSONArray portCounter = switchCounter.getJSONObject(i).getJSONArray("port");
					for (int j = 0; j < portCounter.length(); j++) {
						JSONObject pcLoop = portCounter.getJSONObject(j);
						PortCounter pc = new PortCounter(pcLoop);
						switchPortCounter.put(switchId, pc);
					}
				}
				
			}
			return switchPortCounter;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	//get topology
	public static List<Topology> getTopo(String ip, String port) {
		
		try {
			List<Topology> topolist = new ArrayList<Topology>();
			InputStreamReader intopo = new InputStreamReader(new URL("http://" + ip + ":" + port + "/wm/topology/links/json").openStream());
			StringBuilder input = new StringBuilder();
			int ch;
			while((ch = intopo.read()) != -1) {
				input.append((char) ch);
			}
			String jsonText = input.toString();
			jsonText = "{\"topology\":" + jsonText + "}";
//			System.out.println(jsonText);
			JSONObject json = new JSONObject(jsonText);
			JSONArray array = json.getJSONArray("topology");
			for (int i = 0; i < array.length(); i++) {
				JSONObject subObject = (JSONObject) array.get(i);
				Topology topo1 = new Topology();
				topo1.src_switch = subObject.get("src-switch").toString();
				topo1.dst_switch = subObject.get("dst-switch").toString();
				topo1.src_port = subObject.get("src-port").toString();
				topo1.dst_port = subObject.get("dst-port").toString();
				topolist.add(topo1);
				Topology topo2 = new Topology();
				topo2.src_switch = subObject.get("dst-switch").toString();
				topo2.dst_switch = subObject.get("src-switch").toString();
				topo2.src_port = subObject.get("dst-port").toString();
				topo2.dst_port = subObject.get("src-port").toString();
				topolist.add(topo2);
			}
			return topolist;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	public static List<String> getDpid(String ip, String port) {
		
		try {
			List<String> switchID = new ArrayList<String>();
			InputStreamReader intopo = new InputStreamReader(new URL("http://" + ip + ":" + port + "/wm/topology/links/json").openStream());
			StringBuilder input = new StringBuilder();
			int ch;
			while((ch = intopo.read()) != -1) {
				input.append((char) ch);
			}
			String jsonText = input.toString();
			jsonText = "{\"topology\":" + jsonText + "}";
//			System.out.println(jsonText);
			JSONObject json = new JSONObject(jsonText);
			JSONArray array = json.getJSONArray("topology");
			for (int i = 0; i < array.length(); i++) {
				JSONObject subObject = (JSONObject)array.get(i);
				String srcSwitch = subObject.get("src-switch").toString();
				String dstSwitch = subObject.get("dst-switch").toString();
				if (!switchID.contains(srcSwitch)) {
					switchID.add(srcSwitch);
				}
				if (!switchID.contains(dstSwitch)) {
					switchID.add(dstSwitch);
				}
			}
			return switchID;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static List<String> getSFlowMetric(String ip, String port) {
		
		try {
			List<String> list = new ArrayList<String>();
			InputStreamReader intopo = new InputStreamReader(new URL("http://" + ip + ":" + port + "/metric/" + ip + "/json").openStream());
			StringBuilder input = new StringBuilder();
			int ch;
			while((ch = intopo.read()) != -1) {
				input.append((char) ch);
			}
			String jsonText = input.toString();
			String[] array = jsonText.split(",");
			List<String> metric = new ArrayList<String>();
			for (int i = 0; i < array.length; i++) {
				metric.add(array[i]);
			}
			return metric;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static List<String> getIndex(List<String> metric) throws Exception {
		if (metric == null) {
			throw new Exception("metric is null");
		}
		
		List<String> indexes = new ArrayList<String>();
		for (int i = 0;i < metric.size(); i++) {
			int dotIndex = metric.get(i).toString().indexOf(".");
			int quoteIndex = metric.get(i).toString().indexOf("\"");
			String index = metric .get(i).toString().substring(quoteIndex + 1, dotIndex);
			indexes.add(index);
		}
		Set<String> h = new HashSet<String>(indexes);
		indexes.clear();
		indexes.addAll(h);
		return indexes;
	}
	
	
	public static List<DataItem> getItem(List<String> index, List<String> metric) {
		
		try {
			List<DataItem> itemList = new ArrayList<DataItem>();
			for (int i = 0; i < index.size(); i++) {
				DataItem item = new DataItem();
				for (int j = 0; j < metric.size(); j++) {
					if (metric.get(j).contains(index.get(i) + ".of_port")) {
						item.port = metric.get(j).substring(metric.get(j).indexOf("\": \"") + 4, metric.get(j).length() - 1);
					}
					if (metric.get(j).contains(index.get(i) + ".of_dpid")) {
						item.dpid = metric.get(j).substring(metric.get(j).length() - 17, metric.get(j).lastIndexOf("\""));
					}
					if (metric.get(j).contains(index.get(i) + ".ifinpkts")) {
						double ifinpkts = Double.valueOf(metric.get(j).substring(metric.get(j).lastIndexOf(":") + 2, metric.get(j).length()));
						if (ifinpkts < 5) {
							item.ifinpkts = Math.round(ifinpkts);
						} else {
							item.ifinpkts = ifinpkts;
						}
					}
					if (metric.get(j).contains(index.get(i) + ".ifoutpkts")) {
						double ifoutpkts = Double.valueOf(metric.get(j).substring(metric.get(j).lastIndexOf(":") + 2, metric.get(j).length()));
						if (ifoutpkts < 5) {
							item.ifoutpkts = Math.round(ifoutpkts);
						} else {
							item.ifoutpkts = ifoutpkts;
						}
					}
				}
				itemList.add(item);
			}
			for (int i = 0; i < itemList.size(); i++) {
				if (itemList.get(i).port.equals("65534")) {
					itemList.remove(i);
					--i;
				}
			}
			
			return itemList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
