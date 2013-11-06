package org.sakaiproject.learninglog.tool.entityprovider;

import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.learninglog.api.*;

import lombok.Setter;

@Setter
public final class LearningLogCommentEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Createable, Describeable, Deleteable {

	public final static String ENTITY_PREFIX = "learninglog-comment";

	private final Logger LOG = Logger.getLogger(LearningLogCommentEntityProvider.class);
	
	private DeveloperHelperService developerService = null;
	private LearningLogManager learningLogManager;
	private SakaiProxy sakaiProxy = null;
	
	public void init() {
	}
	
	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		
		final String userId = developerService.getCurrentUserId();
		
		final String id = (String) params.get("id");
		final String postId = (String) params.get("postId");
		final String content = (String) params.get("content");
		final String siteId = (String) params.get("siteId");
		final String visibility = (String) params.get("visibility");
		
		final Comment comment = new Comment();
		comment.setId(id);
		comment.setPostId(postId);
		comment.setCreatorId(userId);
		comment.setContent(content);
		comment.setVisibility(visibility);

		final boolean isNew = "".equals(comment.getId());
		
		if(learningLogManager.saveComment(comment)) {

			if(isNew && visibility.equals(Visibilities.READY)) {
				final String reference = Constants.REFERENCE_ROOT + "/" + siteId + "/comment/" + postId;
				sakaiProxy.postEvent(Constants.BLOG_COMMENT_CREATED_SS,reference,siteId);
				
				// Send an email to the post author
				learningLogManager.sendNewCommentAlert(comment);
			}
			
			return comment.getId();
		} else {
			return "FAIL";
        }
	}

	public Object getSampleEntity() {
		return new Comment();
	}

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}
	
	public String[] getHandledOutputFormats() {
	    return new String[] { Formats.JSON };
	}
	
	public String[] getHandledInputFormats() {
        return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
    }

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {

		if(LOG.isDebugEnabled()) LOG.debug("deleteEntity");
		
		final String siteId = (String) params.get("siteId");
		
		if(learningLogManager.deleteComment(ref.getId()))
			sakaiProxy.postEvent(Constants.BLOG_COMMENT_DELETED_SS,ref.getId(),siteId);
	}

	public boolean entityExists(String id) {
		return true;
	}
}
