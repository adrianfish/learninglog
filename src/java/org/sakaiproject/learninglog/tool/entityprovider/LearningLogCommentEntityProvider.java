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
import org.sakaiproject.learninglog.LearningLogManager;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.learninglog.api.Comment;

public class LearningLogCommentEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Createable, Describeable, Deleteable
{
	public final static String ENTITY_PREFIX = "learninglog-comment";
	
	private DeveloperHelperService developerService = null;
	public void setDeveloperService(DeveloperHelperService developerService) {
		this.developerService = developerService;
	}
	  
	protected final Logger LOG = Logger.getLogger(LearningLogCommentEntityProvider.class);
	
	private LearningLogManager learningLogManager;
	private SakaiProxy sakaiProxy = null;
	
	public void init() {
		
		sakaiProxy = new SakaiProxy();
		learningLogManager = new LearningLogManager(sakaiProxy);
	}
	
	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		
		String userId = developerService.getCurrentUserId();
		
		String id = (String) params.get("id");
		String postId = (String) params.get("postId");
		String content = (String) params.get("content");
		String siteId = (String) params.get("siteId");
		
		Comment comment = new Comment();
		comment.setId(id);
		comment.setPostId(postId);
		comment.setCreatorId(userId);
		comment.setContent(content);
		
		boolean isNew = "".equals(comment.getId());
		
		if(learningLogManager.saveComment(comment))
		{
			if(isNew)
			{
				String reference = LearningLogManager.REFERENCE_ROOT + "/" + siteId + "/comment/" + postId;
				sakaiProxy.postEvent(LearningLogManager.BLOG_COMMENT_CREATED,reference,siteId);
				
				// Send an email to the post author
				learningLogManager.sendNewCommentAlert(comment);
			}
			
			return comment.getId();
		}
		else
			return "FAIL";
	}

	public Object getSampleEntity()
	{
		return new Comment();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}
	
	public String[] getHandledOutputFormats() {
	    return new String[] { Formats.JSON };
	}
	
	public String[] getHandledInputFormats() {
        return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
    }

	public void deleteEntity(EntityReference ref, Map<String, Object> params)
	{
		if(LOG.isDebugEnabled()) LOG.debug("deleteEntity");
		
		String siteId = (String) params.get("siteId");
		
		if(learningLogManager.deleteComment(ref.getId()))
			sakaiProxy.postEvent(LearningLogManager.BLOG_COMMENT_DELETED,ref.getId(),siteId);
	}

	public boolean entityExists(String id) {
		return false;
	}
}
