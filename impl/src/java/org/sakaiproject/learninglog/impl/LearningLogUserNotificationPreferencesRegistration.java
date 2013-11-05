package org.sakaiproject.learninglog.impl;

import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.UserNotificationPreferencesRegistrationImpl;

public class LearningLogUserNotificationPreferencesRegistration extends UserNotificationPreferencesRegistrationImpl  {
	
	public ResourceLoader getResourceLoader(String location) {
		return new ResourceLoader(location);
	}
}
