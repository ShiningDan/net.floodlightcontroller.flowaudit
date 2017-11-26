package net.floodlightcontroller.flowaudit;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class FlowAuditRoutable implements RestletRoutable {

	private String port = "";
	
	FlowAuditRoutable(String port) {
		this.port = port;
	}
	
	@Override
	public Restlet getRestlet(Context context) {
		context.getAttributes().put("port", this.port);
		Router router = new Router(context);
		router.attach("/backhole/active/json", BlackholeActiveAudit.class);
		router.attach("/backhole/passive/json", BlackholePassiveDetect.class);
		router.attach("/loop/active/json", LoopActiveAudit.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/flow/audit";
	}

}
