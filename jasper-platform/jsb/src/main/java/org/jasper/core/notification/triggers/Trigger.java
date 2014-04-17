package org.jasper.core.notification.triggers;

import com.google.gson.JsonElement;

public interface Trigger{
		
	public boolean evaluate(JsonElement response);

}
