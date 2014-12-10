package org.sakaiproject.learninglog.tool.entityprovider;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.learninglog.api.LearningLogManager;
import org.sakaiproject.learninglog.api.SakaiProxy;
import org.sakaiproject.learninglog.api.BlogMember;

import lombok.Setter;

@Setter
public class LearningLogAuthorEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Outputable, Describeable, CollectionResolvable, ActionsExecutable {

	public final static String ENTITY_PREFIX = "learninglog-author";
	
	private LearningLogManager learningLogManager;
	private SakaiProxy sakaiProxy = null;

	protected final Logger LOG = Logger.getLogger(LearningLogAuthorEntityProvider.class);
	
	public void init() { }
	
	public boolean entityExists(String id) {

		if(LOG.isDebugEnabled()) LOG.debug("entityExists("  + id + ")");

		if (id == null) {
			return false;
		}
		
		if ("".equals(id)) {
			return false;
        }
		
		try {
			sakaiProxy.getUser(id);
			return true;
		} catch(Exception e) {
			LOG.error("Caught exception whilst getting user.",e);
			return false;
		}
	}

	/**
	 * No intention of implementing this. Forced to due to the fact that
	 * CollectionsResolvable extends Resolvable
	 */
	public Object getEntity(EntityReference ref) {

		if(LOG.isDebugEnabled()) LOG.debug("getEntity(" + ref.getId() + ")");
		
		LOG.warn("getEntity is unimplemented. Returning null ...");
		
		return null;
	}

	public Object getSampleEntity() {
		return new BlogMember();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}
	
	public String[] getHandledOutputFormats() {
	    return new String[] { Formats.JSON };
	}

	public List<BlogMember> getEntities(EntityReference ref, Search search) {

		List<BlogMember> authors = new ArrayList<BlogMember>();
		
		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		
        if (locRes != null) {

        	String location = locRes.getStringValue();
        	String context = new EntityReference(location).getId();
        
        	try {
        		authors = learningLogManager.getAuthors(context);
        	} catch (Exception e) {
        		LOG.error("Caught exception whilst getting posts.",e);
        	}
        }
        
		return authors;
	}
}
