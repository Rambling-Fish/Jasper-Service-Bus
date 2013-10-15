package org.jasper.core.notification.triggers;

import org.apache.jena.atlas.json.JsonArray;


public interface Trigger {
		
	public boolean evaluate(JsonArray array);


}
