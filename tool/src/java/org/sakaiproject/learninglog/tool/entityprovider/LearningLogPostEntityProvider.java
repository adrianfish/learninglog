package org.sakaiproject.learninglog.tool.entityprovider;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Deleteable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import org.sakaiproject.learninglog.api.*;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

import lombok.Setter;

@Setter
public final class LearningLogPostEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Createable, Outputable, Describeable, Deleteable, CollectionResolvable, ActionsExecutable, Statisticable {

	public final static String ENTITY_PREFIX = "learninglog-post";
	
	private static final String[] EVENT_KEYS
		= new String[] {
			Constants.BLOG_POST_CREATED_SS,
			Constants.BLOG_POST_DELETED_SS,
			Constants.BLOG_POST_RECYCLED_SS,
			Constants.BLOG_POST_RESTORED_SS,
			Constants.BLOG_COMMENT_CREATED_SS,
			Constants.BLOG_COMMENT_DELETED_SS};
	
	private DeveloperHelperService developerService;
	private LearningLogManager learningLogManager;
	private SakaiProxy sakaiProxy;
	
	public void init() {}

	private final Logger LOG = Logger.getLogger(LearningLogPostEntityProvider.class);

	public boolean entityExists(String id) {

		if (id == null) {
			return false;
		}

		if ("".equals(id)) {
			return false;
		}

		try {
			return (learningLogManager.getPost(id) != null);
		}
		catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref) {

		final String id = ref.getId();

		if (id == null || "".equals(id)) {
			return new Post();
		}

		Post post = null;

		try {
			post = learningLogManager.getPost(id);
		}
		catch (Exception e) {
			LOG.error("Caught exception whilst getting post.", e);
		}

		if (post == null) {
			throw new IllegalArgumentException("Post not found");
		}

		// TODO: Security !!!!

		return post;
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {

		final String userId = developerService.getCurrentUserId();

		final String id = (String) params.get("id");
		final String visibility = (String) params.get("visibility");
		final String title = (String) params.get("title");
		final String content = (String) params.get("content");
		final String siteId = (String) params.get("siteId");
		final String mode = (String) params.get("mode");
		final String isAutosave = (String) params.get("isAutosave");

		final Post post = new Post();
		post.setId(id);
		post.setVisibility(visibility);
		post.setCreatorId(userId);
		post.setSiteId(siteId);
		post.setTitle(title);
		post.setContent(content);
		post.setAttachments(getAttachments(params));
		post.isAutosave = (isAutosave.equals("yes")) ? true : false;


		final String toolId = sakaiProxy.getLearningLogToolId(siteId);
		post.setUrl(sakaiProxy.getServerUrl() + "/portal/directtool/" + toolId + "?state=post&postId=" + id);
		
		final boolean isNew = "".equals(post.getId());
		final boolean isPublishing = mode != null && "publish".equals(mode);

        if(isPublishing) {
            // Make sure we merge in all the previously uploaded attachments, otherwise
            // they won't get moved into the current user's dropbox
            try {
                Post storedPost = learningLogManager.getPost(id);
                List<Attachment> allAttachments = post.getAttachments();
                allAttachments.addAll(storedPost.getAttachments());
                post.setAttachments(allAttachments);
            } catch (Exception e) {

            }
        }

		if (learningLogManager.savePost(post, isPublishing)) {

			if((isNew || isPublishing) && post.isReady()) {

                // This is for sitestats purposes.
				sakaiProxy.postEvent(Constants.BLOG_POST_CREATED_SS, post.getReference(), post.getSiteId());
				
                if (learningLogManager.isEmailsMode(post.getSiteId())) {
				    learningLogManager.sendNewPostAlert(post);
                }
			}
			
			try {

				String json = "{\"id\":\"" + post.getId() + "\",\"isAutosave\":" + post.isAutosave + ",\"attachments\":[";
				for(Attachment attachment : post.getAttachments()) {
					json += "{\"id\":\"" + attachment.id + "\",\"name\":\"" + attachment.name + "\"},";
				}
				if(json.endsWith(",")) {
					json = json.substring(0,json.length() - 1);
				}
				json += "]}";
				return URLEncoder.encode(json,"UTF-8");
			} catch(Exception e) {
				throw new EntityException("Failed to encode JSON response","");
			}
		} else {
			throw new EntityException("Failed to save post","");
		}
	}
	
	private List<Attachment> getAttachments(Map<String, Object> params) {

		final List<FileItem> fileItems = new ArrayList<FileItem>();

		final String uploadsDone = (String) params.get(RequestFilter.ATTR_UPLOADS_DONE);

		if (uploadsDone != null && uploadsDone.equals(RequestFilter.ATTR_UPLOADS_DONE)) {
			LOG.debug("UPLOAD STATUS: " + params.get("upload.status"));

			try {
				FileItem attachment1 = (FileItem) params.get("attachment_0");
				if (attachment1 != null && attachment1.getSize() > 0)
					fileItems.add(attachment1);
				FileItem attachment2 = (FileItem) params.get("attachment_1");
				if (attachment2 != null && attachment2.getSize() > 0)
					fileItems.add(attachment2);
				FileItem attachment3 = (FileItem) params.get("attachment_2");
				if (attachment3 != null && attachment3.getSize() > 0)
					fileItems.add(attachment3);
				FileItem attachment4 = (FileItem) params.get("attachment_3");
				if (attachment4 != null && attachment4.getSize() > 0)
					fileItems.add(attachment4);
				FileItem attachment5 = (FileItem) params.get("attachment_4");
				if (attachment5 != null && attachment5.getSize() > 0)
					fileItems.add(attachment5);
			} catch (Exception e) {

			}
		}

		final List<Attachment> attachments = new ArrayList<Attachment>();
		if (fileItems.size() > 0) {
			for (Iterator i = fileItems.iterator(); i.hasNext();) {
				final FileItem fileItem = (FileItem) i.next();

				String name = fileItem.getName();

				if (name.contains("/"))
					name = name.substring(name.lastIndexOf("/") + 1);
				else if (name.contains("\\"))
					name = name.substring(name.lastIndexOf("\\") + 1);

				attachments.add(new Attachment(name, fileItem.getContentType(), fileItem.get()));
			}
		}

		return attachments;
	}

	public Object getSampleEntity() {
		return new Post();
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

	public List<Post> getEntities(EntityReference ref, Search search) {

		List<Post> posts = new ArrayList<Post>();

		final Restriction creatorRes = search.getRestrictionByProperty("creatorId");

		final Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		final Restriction visibilities = search.getRestrictionByProperty("visibilities");

		final QueryBean query = new QueryBean();
		query.setVisibilities(new String[] {Visibilities.READY,Visibilities.PRIVATE});

		if (locRes != null) {
			final String location = locRes.getStringValue();
			final String context = new EntityReference(location).getId();
			query.setSiteId(context);
		}

		if (creatorRes != null) {
			query.setCreator(creatorRes.getStringValue());
        }

		if (visibilities != null) {
			final String visibilitiesValue = visibilities.getStringValue();
			final String[] values = visibilitiesValue.split(",");
			query.setVisibilities(values);
		}

		try {
			posts = learningLogManager.getPosts(query);
		} catch (Exception e) {
			LOG.error("Caught exception whilst getting posts.", e);
		}

		return posts;
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("deleteEntity");
        }
		
		final String siteId = (String) params.get("siteId");
		
		if(learningLogManager.deletePost(ref.getId())) {

			String reference = Constants.REFERENCE_ROOT + "/" + siteId + "/post/" + ref.getId();
			sakaiProxy.postEvent(Constants.BLOG_POST_DELETED_SS,reference,siteId);
		}
	}

	@EntityCustomAction(action = "recycle", viewKey = EntityView.VIEW_SHOW)
	public String handleRecycle(EntityReference ref) {

		final String postId = ref.getId();
		
		if (postId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
        }
		
		Post post = null;
		
		try {
			post = learningLogManager.getPost(postId);
		} catch(Exception e) {
		}
		
		if(post == null)
			throw new IllegalArgumentException("Invalid post id");
		
		if(learningLogManager.recyclePost(postId)) {

			final String reference = Constants.REFERENCE_ROOT + "/" + post.getSiteId() + "/post/" + ref.getId();
			sakaiProxy.postEvent(Constants.BLOG_POST_RECYCLED_SS,reference,post.getSiteId());
			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}
	
	@EntityCustomAction(action = "deleteAttachment", viewKey = EntityView.VIEW_SHOW)
	public String handleDeleteAttachment(EntityReference ref, Map<String,Object> params) {
		
		final String postId = ref.getId();
		
		if (postId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}
		
		final String siteId = (String) params.get("siteId");
		
		if (siteId == null) {
			throw new IllegalArgumentException("Invalid parameters provided: expect to receive the site id as a parameter named 'siteId'");
		}
		
		final String name = (String) params.get("name");
		
		if (name == null) {
			throw new IllegalArgumentException("Invalid parameters provided: expect to receive the attachment name as a parameter named 'name'");
		}
		
		if(learningLogManager.deleteAttachment(siteId, name, postId)) {
			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}

	@EntityCustomAction(action = "restore", viewKey = EntityView.VIEW_LIST)
	public String handleRestore(EntityView view, Map<String,Object> params) {
		
		final String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null) {
			throw new EntityException("You must be logged in to restore posts","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		if(!params.containsKey("posts")) {
			throw new EntityException("Bad request: a posts param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		final String postIdsString = (String) params.get("posts");
		
		final String[] postIds = postIdsString.split(",");
		
		for(String postId : postIds) {

			Post post = null;

			try {
				post = learningLogManager.getPost(postId);
			} catch (Exception e) {
				LOG.error("Failed to retrieve post with id '" + postId + "' during restore operation. Skipping restore ...",e);
				continue;
			}

			if (post == null) {
				LOG.info("Post id '" + postId + "' is invalid. Skipping restore ...");
				continue;
			}

			if (learningLogManager.restorePost(postId)) {
				final String reference = Constants.REFERENCE_ROOT + "/" + post.getSiteId() + "/posts/" + postId;
				sakaiProxy.postEvent(Constants.BLOG_POST_RESTORED_SS, reference, post.getSiteId());
			}
		}
		
		return "SUCCESS";
	}

	@EntityCustomAction(action = "remove", viewKey = EntityView.VIEW_LIST)
	public String handleRemove(EntityView view, Map<String,Object> params) {
		
		final String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to delete posts","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		if (!params.containsKey("posts")) {
			throw new EntityException("Bad request: a posts param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		final String siteId = (String) params.get("site");
		
		if (siteId == null) {
			throw new EntityException("Bad request: a site param must be supplied","",HttpServletResponse.SC_BAD_REQUEST);
		}
		
		final String postIdsString = (String) params.get("posts");
		
		final String[] postIds = postIdsString.split(",");
		
		for (String postId : postIds) {

			if (learningLogManager.deletePost(postId)) {
				final String reference = Constants.REFERENCE_ROOT + "/" + siteId + "/posts/" + postId;
				sakaiProxy.postEvent(Constants.BLOG_POST_DELETED_SS, reference, siteId);
			}
		}
		
		return "SUCCESS";
	}

    @EntityCustomAction(action = "set-group-mode", viewKey = EntityView.VIEW_LIST)
	public String handleSetGroupMode(EntityView view, Map<String,Object> params) {

		final String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to set group mode", "", HttpServletResponse.SC_UNAUTHORIZED);
		}

		final String siteId = (String) params.get("siteId");
		
		if (siteId == null) {
			throw new EntityException("Bad request: a site param must be supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        final String groupMode = (String) params.get("groupMode");

		if (groupMode == null) {
			throw new EntityException("Bad request: a group mode must be supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (sakaiProxy.canModifyPermissions(siteId)) {
            learningLogManager.setGroupMode(siteId, groupMode);
        }

        return "SUCCESS";
	}

    @EntityCustomAction(action = "set-emails-mode", viewKey = EntityView.VIEW_LIST)
	public String handleSetEmailsMode(EntityView view, Map<String,Object> params) {

		final String userId = developerHelperService.getCurrentUserId();
		
		if (userId == null) {
			throw new EntityException("You must be logged in to set emails mode", "", HttpServletResponse.SC_UNAUTHORIZED);
		}

		final String siteId = (String) params.get("siteId");
		
		if (siteId == null) {
			throw new EntityException("Bad request: a site param must be supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        final String emailsMode = (String) params.get("emailsMode");

		if (emailsMode == null) {
			throw new EntityException("Bad request: an emails mode must be supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (sakaiProxy.canModifyPermissions(siteId)) {
            learningLogManager.setEmailsMode(siteId, emailsMode);
        }

        return "SUCCESS";
	}


	/**
	 * From Statisticable
	 */
	public String getAssociatedToolId() {
		return "sakai.learninglog";
	}

	/**
	 * From Statisticable
	 */
	public String[] getEventKeys() {

		String[] temp = new String[EVENT_KEYS.length];
		System.arraycopy(EVENT_KEYS, 0, temp, 0, EVENT_KEYS.length);
		return temp;
	}

	/**
	 * From Statisticable
	 */
	public Map<String, String> getEventNames(Locale locale) {

		final Map<String, String> localeEventNames = new HashMap<String, String>();
		final ResourceLoader msgs = new ResourceLoader("LearningLogEvents");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++) {
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}

	public final class JSONPost {
		
		public String id = "";
		public boolean isAutosave = false;
		public List<Attachment> attachments = new ArrayList<Attachment>();
		
		public JSONPost(Post post) {

			this.id = post.getId();
			this.isAutosave = post.isAutosave;
			this.attachments = post.getAttachments();
		}
	}
}
