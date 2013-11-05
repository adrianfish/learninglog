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

package org.sakaiproject.learninglog.api;

import java.util.Date;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.util.BaseResourceProperties;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringEscapeUtils;

public class Comment implements Entity {
	
    @Getter @Setter
	private String id = "";
    
    @Getter @Setter
    private long modifiedDate = -1L;
    
    @Getter @Setter
    private String creatorId;
    
    @Getter @Setter
    private String creatorDisplayName;
    
    @Getter @Setter
    private String postId;

    @Getter @Setter
    private String siteId;
    
    @Getter
    private String content = "";

    @Getter
    private long createdDate = -1L;

    @Setter
    private String url;

    public Comment() {
    	this("");
    }
    
    public Comment(String text) {
    	this(text,new Date().getTime());
    }
    
    public Comment(String text, long createdDate) {
        setContent(text);
        this.createdDate = createdDate;
        modifiedDate = createdDate;
    }
    
    /**
     * If the supplied is different to the current, sets the modified date to
     * the current date so ... be careful!
     * 
     * @param text
     */
    public void setContent(String text) {
    	setContent(text,true);
    }
    
    public void setContent(String text,boolean modified) {
    	if(!this.content.equals(text) && modified)
    		modifiedDate = new Date().getTime();
    	
		this.content = StringEscapeUtils.unescapeHtml(text.trim());
    }
    
	public void setCreatedDate(long createdDate) {
		
		this.createdDate = createdDate;
		this.modifiedDate = createdDate;
	}

    /** START ENTITY IMPL */

    public String getReference() {
        return Constants.REFERENCE_ROOT + Entity.SEPARATOR + siteId  + Entity.SEPARATOR + "comments" + Entity.SEPARATOR + id;
    }

    public String getReference(String rootProperty) {
        return getReference();
    }

    public String getUrl() {
        return url;
    }

    public String getUrl(String rootProperty) {
        return url;
    }

    public ResourceProperties getProperties() {

        ResourceProperties rp = new BaseResourceProperties();
        rp.addProperty("id", getId());
        return rp;
    }

    public Element toXml(Document doc, Stack stack) {
        return null;
    }
}
