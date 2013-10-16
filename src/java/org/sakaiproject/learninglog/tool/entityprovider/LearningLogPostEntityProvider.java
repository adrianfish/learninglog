package org.sakaiproject.learninglog.tool.entityprovider;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.sakaiproject.learninglog.LearningLogManager;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.learninglog.api.Attachment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;
import org.sakaiproject.learninglog.api.Visibilities;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

import lombok.Setter;

@Setter
public class LearningLogPostEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, Inputable, Createable, Outputable, Describeable, Deleteable, CollectionResolvable, ActionsExecutable, Statisticable
{
	public final static String ENTITY_PREFIX = "learninglog-post";
	
	private static final String[] EVENT_KEYS
		= new String[] {
			LearningLogManager.BLOG_POST_CREATED,
			LearningLogManager.BLOG_POST_DELETED,
			LearningLogManager.BLOG_POST_RECYCLED,
			LearningLogManager.BLOG_POST_RESTORED,
			LearningLogManager.BLOG_COMMENT_CREATED,
			LearningLogManager.BLOG_COMMENT_DELETED};
	
	private DeveloperHelperService developerService = null;
	private LearningLogManager learningLogManager;
	private SakaiProxy sakaiProxy  = null;
	
	public void init() {
	}

	protected final Logger LOG = Logger.getLogger(LearningLogPostEntityProvider.class);

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

		String id = ref.getId();

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

		String userId = developerService.getCurrentUserId();

		String id = (String) params.get("id");
		String visibility = (String) params.get("visibility");
		String title = (String) params.get("title");
		String content = (String) params.get("content");
		String siteId = (String) params.get("siteId");
		String mode = (String) params.get("mode");
		String isAutosave = (String) params.get("isAutosave");

		Post post = new Post();
		post.setId(id);
		post.setVisibility(visibility);
		post.setCreatorId(userId);
		post.setSiteId(siteId);
		post.setTitle(title);
		post.setContent(content);
		post.setAttachments(getAttachments(params));
		post.isAutosave = (isAutosave.equals("yes")) ? true : false;

		String toolId = sakaiProxy.getLearningLogToolId(siteId);
		post.setUrl(sakaiProxy.getServerUrl() + "/portal/directtool/" + toolId + "?state=post&postId=" + id);
		
		boolean isNew = "".equals(post.getId());

		if (learningLogManager.savePost(post)) {
			if((isNew || (mode != null && "publish".equals(mode))) && post.isReady()) {
				String reference = LearningLogManager.REFERENCE_ROOT + "/" + siteId + "/post/" + post.getId();
				sakaiProxy.postEvent(LearningLogManager.BLOG_POST_CREATED,reference,post.getSiteId());
				
				// Send an email to all site participants apart from the author
				learningLogManager.sendNewPostAlert(post);
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
		}
		else {
			throw new EntityException("Failed to save post","");
		}
	}
	
	private List<Attachment> getAttachments(Map<String, Object> params) {
		List<FileItem> fileItems = new ArrayList<FileItem>();

		String uploadsDone = (String) params.get(RequestFilter.ATTR_UPLOADS_DONE);

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

		List<Attachment> attachments = new ArrayList<Attachment>();
		if (fileItems.size() > 0) {
			for (Iterator i = fileItems.iterator(); i.hasNext();) {
				FileItem fileItem = (FileItem) i.next();

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

	public Object getSampleEntity()
	{
		return new Post();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats()
	{
		return new String[] { Formats.JSON };
	}

	public String[] getHandledInputFormats()
	{
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public List<Post> getEntities(EntityReference ref, Search search)
	{
		List<Post> posts = new ArrayList<Post>();

		Restriction creatorRes = search.getRestrictionByProperty("creatorId");

		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		Restriction visibilities = search.getRestrictionByProperty("visibilities");

		QueryBean query = new QueryBean();
		query.setVisibilities(new String[] {Visibilities.READY,Visibilities.PRIVATE});

		if (locRes != null)
		{
			String location = locRes.getStringValue();
			String context = new EntityReference(location).getId();

			query.setSiteId(context);
		}

		if (creatorRes != null)
			query.setCreator(creatorRes.getStringValue());

		if (visibilities != null)
		{
			String visibilitiesValue = visibilities.getStringValue();
			String[] values = visibilitiesValue.split(",");
			query.setVisibilities(values);
		}

		try
		{
			posts = learningLogManager.getPosts(query);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting posts.", e);
		}

		return posts;
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("deleteEntity");
		
		String siteId = (String) params.get("siteId");
		
		if(learningLogManager.deletePost(ref.getId()))
		{
			String reference = LearningLogManager.REFERENCE_ROOT + "/" + siteId + "/post/" + ref.getId();
			sakaiProxy.postEvent(LearningLogManager.BLOG_POST_DELETED,reference,siteId);
		}
	}

	@EntityCustomAction(action = "recycle", viewKey = EntityView.VIEW_SHOW)
	public String handleRecycle(EntityReference ref)
	{
		String postId = ref.getId();
		
		if (postId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		
		Post post = null;
		
		try
		{
			post = learningLogManager.getPost(postId);
		}
		catch(Exception e)
		{
		}
		
		if(post == null)
			throw new IllegalArgumentException("Invalid post id");
		
		if(learningLogManager.recyclePost(postId))
		{
			String reference = LearningLogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/post/" + ref.getId();
			sakaiProxy.postEvent(LearningLogManager.BLOG_POST_RECYCLED,reference,post.getSiteId());
			
			return "SUCCESS";
		}
		else
		{
			return "FAIL";
		}
	}
	
	@EntityCustomAction(action = "restore", viewKey = EntityView.VIEW_SHOW)
	public String handleRestore(EntityReference ref)
	{
		String postId = ref.getId();
		
		if (postId == null)
		{
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}
		
		Post post = null;
		
		try
		{
			post = learningLogManager.getPost(postId);
		}
		catch(Exception e)
		{
		}
		
		if(post == null)
			throw new IllegalArgumentException("Invalid post id");
		
		if(learningLogManager.restorePost(postId))
		{
			String reference = LearningLogManager.REFERENCE_ROOT + "/" + post.getSiteId() + "/post/" + ref.getId();
			sakaiProxy.postEvent(LearningLogManager.BLOG_POST_RESTORED,reference,post.getSiteId());
			
			return "SUCCESS";
		}
		else
		{
			return "FAIL";
		}
	}
	
	@EntityCustomAction(action = "deleteAttachment", viewKey = EntityView.VIEW_SHOW)
	public String handleDeleteAttachment(EntityReference ref, Map<String,Object> params) {
		
		String postId = ref.getId();
		
		if (postId == null) {
			throw new IllegalArgumentException("Invalid path provided: expect to receive the post id");
		}
		
		String siteId = (String) params.get("siteId");
		
		if (siteId == null) {
			throw new IllegalArgumentException("Invalid parameters provided: expect to receive the site id as a parameter named 'siteId'");
		}
		
		String attachmentId = (String) params.get("attachmentId");
		
		if (attachmentId == null) {
			throw new IllegalArgumentException("Invalid parameters provided: expect to receive the attachment id as a parameter named 'attachmentId'");
		}
		
		if(learningLogManager.deleteAttachment(siteId, attachmentId,postId)) {
			return "SUCCESS";
		} else {
			return "FAIL";
		}
	}

	/**
	 * From Statisticable
	 */
	public String getAssociatedToolId()
	{
		return "sakai.learninglog";
	}

	/**
	 * From Statisticable
	 */
	public String[] getEventKeys()
	{
		String[] temp = new String[EVENT_KEYS.length];
		System.arraycopy(EVENT_KEYS, 0, temp, 0, EVENT_KEYS.length);
		return temp;
	}

	/**
	 * From Statisticable
	 */
	public Map<String, String> getEventNames(Locale locale)
	{
		Map<String, String> localeEventNames = new HashMap<String, String>();
		ResourceLoader msgs = new ResourceLoader("LearningLogEvents");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++)
		{
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}

	public class JSONPost {
		
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
