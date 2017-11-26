package net.floodlightcontroller.flowaudit;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.floodlightcontroller.flowaudit.DataPacket.Flow;
import net.floodlightcontroller.flowaudit.DataPacket.IPAddress;

public class FlowList {
	
	
	List<Flow> flowlist = new ArrayList<Flow>();
	String ip;
	String port;
	
	public FlowList(String dpid, String ip, String port) {
		this.ip = ip;
		this.port = port;
		this.GetFlow(dpid, this.ip, this.port);
	}
	
	public List<Flow> getFlowList() {
		return this.flowlist;
	}
	
	private void GetFlow(String dpid, String ip, String port) {
		try {
			
			InputStreamReader in = new InputStreamReader(new URL("http://" + ip + ":" + port + "/wm/core/switch/" + dpid + "/flow/json").openStream());
			StringBuilder input = new StringBuilder();
			int ch;
			while((ch = in.read()) != -1) {
				input.append((char) ch);
			}
			String jsonText = input.toString();
			JSONObject jsonFlows = new JSONObject(jsonText);
			JSONArray flowArrays = jsonFlows.getJSONArray("flows");
			
			for (int i = 0; i < flowArrays.length(); i++) {
				JSONObject jsonFlow = new JSONObject(flowArrays.get(i).toString());
				JSONObject jsonMatch = new JSONObject(jsonFlow.get("match").toString());
				Flow flow = new Flow();
				if (jsonFlow.has("priority")) {
					if (Integer.parseInt(jsonFlow.get("priority").toString()) == 0) {
						break;
					}
					flow.priority = Integer.parseInt(jsonFlow.get("priority").toString());
				}
				flow.addValue(jsonMatch);
				if (jsonFlow.has("instructions")) {
					JSONObject jsonInstru = new JSONObject(jsonFlow.get("instructions").toString());
					if (jsonInstru.has("instruction_apply_actions")) {
						JSONObject jsonAction = new JSONObject(jsonInstru.get("instruction_apply_actions").toString());
						if (jsonAction.has("actions")) {
							flow.action = jsonAction.get("actions").toString();
						} else {
							flow.action = "drop";
						}
					} else if (jsonInstru.has("none")){
						flow.action = "drop";
					}
				} else {
					flow.action = "drop";
				}
				flowlist.add(flow);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
