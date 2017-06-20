package net.floodlightcontroller.flowaudit;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class FlowAuditRoutable implements RestletRoutable {

	
	
	@Override
	public Restlet getRestlet(Context context) {
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
