package net.floodlightcontroller.flowaudit;

import java.util.Objects;

import org.json.JSONObject;

import net.floodlightcontroller.flowaudit.DataPacket.IPAddress;

public class DataPacket {
	public static class DataItem{
		public String dpid;
		public String port;
		public double ifinpkts;
		public double ifoutpkts;
		public double portdiscard;
	}
	public static class Topology {
	   public String src_switch;
	   public String src_port;
	   public String dst_switch;
	   public String dst_port;
	   
	   public String toString() {
		   return "Topology : {src_siwtch :" + this.src_switch + ", src_port :" + this.src_port + ", dst_switch :" + this.dst_switch + ", dst_port :" + this.dst_port + "}";  
	   }
	}
	public static class IPAddress implements Cloneable{
		public String ip;
		public int cidr;
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj.getClass() != this.getClass()) {
				return false;
			}
			IPAddress otherObj = (IPAddress)obj;
			if (this.cidr == otherObj.cidr && Objects.equals(this.ip, otherObj.ip)) {
				return true;
			}
			return false;
		}
		
		public Object clone() throws CloneNotSupportedException {
			
			return (IPAddress)super.clone();
		}
		public IPAddress(String address){
			String[] destString = address.split("/");
			if(destString.length == 1) {
				ip = destString[0];
				cidr = 0;
			}
			else{
				ip = destString[0];
				String mask = transBinary(destString[1]);
				cidr = mask.lastIndexOf("1")+1;
			}
		} 
		public String transBinary(String address){
			String [] binary = address.split("\\.");
			for(int i=0;i<binary.length;i++){
				binary[i]=Integer.toBinaryString(Integer.parseInt(binary[i]));
				int num = 8-binary[i].length();
				String str0 = "";
				for(int j=0;j<num;j++){
					str0=str0.concat("0");
				}
				binary[i]=str0.concat(binary[i]);
			}
			String binaryIP = binary[0];
			binaryIP = binaryIP.concat(binary[1]).concat(binary[2]).concat(binary[3]);
			return binaryIP;
		}
		public boolean isoverlap(IPAddress other){
			boolean bool = false;
			if((cidr == 0) && (other.cidr == 0)){
				if(ip.equals(other.ip)) {bool = true;}
			}else{
				String thisbinary = transBinary(ip).substring(0, cidr);
				String otherbinary = transBinary(other.ip).substring(0, cidr);
				if(thisbinary.equals(otherbinary)) {bool = true;}
			}
			return bool;
		}
		
		public String toString() {
			return ip + "/" + cidr;
		}
	}
	public static class Flow implements Cloneable{
		public int priority; 
		public String in_port;   
		public String eth_src;
		public String eth_dst;  
		public String eth_type; 
		public String vlan_vid; 
		public String vlan_priority;
		public IPAddress ipv4_src;
		public IPAddress ipv4_dst;
		public String ip_proto;
		public String ip_tos_bit;
		public String src_port;            //(TCP/UDP) port
		public String dst_port;			//(TCP/UDP) port
		public String action;   //暂定String，"output=3"}}
		
		public Flow() {}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Flow flowObj = (Flow)obj;
			if (this.priority == flowObj.priority && Objects.equals(this.in_port, flowObj.in_port) && Objects.equals(this.eth_src, flowObj.eth_src)
					&& Objects.equals(this.eth_dst, flowObj.eth_dst) && Objects.equals(this.eth_type, flowObj.eth_type) && Objects.equals(this.vlan_vid, flowObj.vlan_vid)
					&& Objects.equals(this.vlan_priority, flowObj.vlan_priority) && Objects.equals(this.ipv4_src, flowObj.ipv4_src) && Objects.equals(this.ipv4_dst, flowObj.ipv4_dst)
					&& Objects.equals(this.ip_proto, flowObj.ip_proto) && Objects.equals(this.ip_tos_bit, flowObj.ip_tos_bit) && Objects.equals(this.src_port, flowObj.src_port) 
					&& Objects.equals(this.dst_port, flowObj.dst_port) && Objects.equals(this.action, flowObj.action)) {
				return true;
			}
			return false;
		}
		
		public Object clone() throws CloneNotSupportedException {
			
			Flow newFlow = (Flow) super.clone();
			if (this.ipv4_src != null) {
				newFlow.ipv4_src = (IPAddress)newFlow.ipv4_src.clone();
			}
			if (this.ipv4_dst != null) {
				newFlow.ipv4_dst = (IPAddress)newFlow.ipv4_dst.clone();
			}
			return newFlow;
		}
		
		public String toString() {
			String result = "";
			result += "priority : " + this.priority + ", ";
			if (this.in_port != null) {
				result += "in_port : " + this.in_port + ", ";
			}
			if (this.eth_src != null) {
				result += "eth_src : " + this.eth_src + ", ";
			}
			if (this.eth_dst != null) {
				result += "eth_dst : " + this.eth_dst + ", ";
			}
			if (this.eth_type != null) {
				result += "eth_type : " + this.eth_type + ", ";
			}
			if (this.vlan_vid != null) {
				result += "vlan_vid : " + this.vlan_vid+ ", ";
			}
			if (this.vlan_priority != null) {
				result += "vlan_priority : " + this.vlan_priority + ", ";
			}
			if (this.ipv4_src != null) {
				result += "ipv4_src : " + this.ipv4_src + ", ";
			}
			if (this.ipv4_dst != null) {
				result += "ipv4_dst : " + this.ipv4_dst + ", ";
			}
			if (this.ip_proto != null) {
				result += "ip_proto : " + this.ip_proto + ", ";
			}
			if (this.ip_tos_bit != null) {
				result += "ip_tos_bit : " + this.ip_tos_bit + ", ";
			}
			if (this.src_port != null) {
				result += "src_port : " + this.src_port + ", ";
			}
			if (this.dst_port != null) {
				result += "dst_port : " + this.dst_port + ", ";
			}
			if (this.action != null) {
				result += "action : " + this.action + "";
			}
			
			return result;
		}
		
		public void addValue(JSONObject jsonMatch) throws Exception{
			if (jsonMatch.has("in_port")) {
				this.in_port = jsonMatch.get("in_port").toString();
			} else {
				this.in_port = null;
			}
			if (jsonMatch.has("eth_dst")) {
				this.eth_dst = jsonMatch.get("eth_dst").toString();
			} else {
				this.eth_dst = null;
			}
			if (jsonMatch.has("eth_type")) {
				this.eth_type = jsonMatch.get("eth_type").toString();
			} else {
				this.eth_type = null;
			}
			if (jsonMatch.has("eth_src")) {
				this.eth_src = jsonMatch.get("eth_src").toString();
			} else {
				this.eth_src = null;
			}
			if (jsonMatch.has("vlan_vid")) {
				this.vlan_vid = jsonMatch.get("vlan_vid").toString();
			} else {
				this.vlan_vid = null;
			}
			if (jsonMatch.has("vlan_pcp")) {
				this.vlan_priority = jsonMatch.get("vlan_pcp").toString();
			} else {
				this.vlan_priority = null;
			}
			if (jsonMatch.has("ip_ecn")) {
				this.ip_tos_bit = jsonMatch.get("ip_ecn").toString();
			} else {
				this.ip_tos_bit = null;
			}
			if (jsonMatch.has("tcp_src") || jsonMatch.has("udp_src")) {
				this.src_port = jsonMatch.get("src_port").toString();
			} else {
				this.src_port = null;
			}
			if (jsonMatch.has("tcp_dst") || jsonMatch.has("udp_dst")) {
				this.dst_port = jsonMatch.get("dst_port").toString();
			} else {
				this.dst_port = null;
			}
			/*
			 * we can use IPAddress class in floodlight
			 * 
			 */
			if (jsonMatch.has("ipv4_src")) {
				this.ipv4_src = new IPAddress(jsonMatch.get("ipv4_src").toString());
			} else {
				this.ipv4_src = null;
			}
			if (jsonMatch.has("ipv4_dst")) {
				this.ipv4_dst = new IPAddress(jsonMatch.get("ipv4_dst").toString());
			} else {
				this.ipv4_dst = null;
			}
			if (jsonMatch.has("ip_proto")) {
				this.ip_proto = jsonMatch.get("ip_proto").toString();
			} else {
				this.ip_proto = null;
			}
		}
	}
}
