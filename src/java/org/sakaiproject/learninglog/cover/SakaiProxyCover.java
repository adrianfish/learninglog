/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/
package org.sakaiproject.learninglog.cover;

import org.sakaiproject.learninglog.SakaiProxy;

public class SakaiProxyCover {
	
    private static SakaiProxy m_instance = null;
    
    public static SakaiProxy getInstance() {
    	
    	if (m_instance == null) {
    		m_instance = new SakaiProxy();
    	}
            
        return m_instance;
    }
    
	public static String getServerUrl() {
		return getInstance().getServerUrl();
	}
	
	public static String getPortalUrl() {
		return getInstance().getPortalUrl();
	}
	
	public static String getLearningLogPageId(String siteId) {
		return getInstance().getLearningLogPageId(siteId);
	}
	
	public static String getLearningLogToolId(String siteId) {
		return getInstance().getLearningLogToolId(siteId);
	}
}
