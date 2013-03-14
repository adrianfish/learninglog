package org.sakaiproject.learninglog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.entity.api.*;
import org.sakaiproject.learninglog.api.BlogFunctions;
import org.sakaiproject.learninglog.api.BlogMember;
import org.sakaiproject.learninglog.api.Comment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;
import org.sakaiproject.learninglog.api.Roles;
import org.sakaiproject.learninglog.api.Visibilities;
import org.sakaiproject.learninglog.api.XmlDefs;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LearningLogManager implements EntityProducer {

	private Logger logger = Logger.getLogger(LearningLogManager.class);
	
	public static final String ENTITY_PREFIX = "learninglog";
	public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
	public static final String BLOG_POST_CREATED = "learninglog.post.created";
	public static final String BLOG_POST_DELETED = "learninglog.post.deleted";
	public static final String BLOG_POST_RECYCLED = "learninglog.post.recycled";
	public static final String BLOG_COMMENT_CREATED = "learninglog.comment.created";
	public static final String BLOG_COMMENT_DELETED = "learninglog.comment.deleted";
	public static final String BLOG_POST_RESTORED = "learninglog.post.restored";

	private PersistenceManager persistenceManager;
	private LearningLogSecurityManager securityManager;
	private SakaiProxy sakaiProxy;

	public LearningLogManager(SakaiProxy sakaiProxy) {
		
		this.sakaiProxy = sakaiProxy;
		
		sakaiProxy.registerFunction(BlogFunctions.BLOG_MODIFY_PERMISSIONS);

		sakaiProxy.registerEntityProducer(this);

		persistenceManager = new PersistenceManager(sakaiProxy);

		securityManager = new LearningLogSecurityManager(sakaiProxy, persistenceManager);
	}

	public Post getPost(String postId) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("getPost(" + postId + ")");

		Post post = persistenceManager.getPost(postId);
		if (securityManager.canCurrentUserReadPost(post))
			return post;
		else
			throw new Exception("The current user does not have permissions to read this post.");
	}

	public List<Post> getPosts(String placementId) throws Exception {
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getAllPost(placementId);
		filtered = securityManager.filter(unfiltered);
		return filtered;
	}

	public List<Post> getPosts(QueryBean query) throws Exception {
		// Get all the posts for the supplied site and filter them through the
		// security manager
		List<Post> filtered;
		List<Post> unfiltered = persistenceManager.getPosts(query);
		filtered = securityManager.filter(unfiltered);
		return filtered;
	}

	public boolean savePost(Post post) {
		try {
			if (securityManager.canCurrentUserSavePost(post))
				return persistenceManager.savePost(post);
		} catch (Exception e) {
			logger.error("Caught exception whilst creating post", e);
		}

		return false;
	}

	public boolean deletePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);
			if (securityManager.canCurrentUserDeletePost(post))
				return persistenceManager.deletePost(post);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean saveComment(Comment comment) {
		try {
			return persistenceManager.saveComment(comment);
		} catch (Exception e) {
			logger.error("Caught exception whilst saving comment", e);
		}

		return false;
	}

	public boolean deleteComment(String commentId) {
		try {
			if (persistenceManager.deleteComment(commentId)) {
				// sakaiProxy.postEvent(BLOG_COMMENT_DELETED,commentId(),post.getSiteId());
				return true;
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting comment.", e);
		}

		return false;
	}

	public boolean recyclePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);

			if (securityManager.canCurrentUserDeletePost(post)) {
				if (persistenceManager.recyclePost(post)) {
					post.setVisibility(Visibilities.RECYCLED);
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Caught an exception whilst recycling post '" + postId + "'");
		}

		return false;
	}

	private String serviceName() {
		return LearningLogManager.class.getName();
	}

	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments) {
		if (logger.isDebugEnabled())
			logger.debug("archive(siteId:" + siteId + ",archivePath:" + archivePath + ")");

		StringBuilder results = new StringBuilder();

		results.append(getLabel() + ": Started.\n");

		int postCount = 0;

		try {
			// start with an element with our very own (service) name
			Element element = doc.createElement(serviceName());
			element.setAttribute("version", "2.5.x");
			((Element) stack.peek()).appendChild(element);
			stack.push(element);

			Element blog = doc.createElement("blog");
			List<Post> posts = getPosts(siteId);
			if (posts != null && posts.size() > 0) {
				for (Post post : posts) {
					Element postElement = post.toXml(doc, stack);
					blog.appendChild(postElement);
					postCount++;
				}
			}

			((Element) stack.peek()).appendChild(blog);
			stack.push(blog);

			stack.pop();

			results.append(getLabel() + ": Finished. " + postCount + " post(s) archived.\n");
		} catch (Exception any) {
			results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
			logger.warn(getLabel() + " exception caught. Message: " + any.getMessage());
		}

		stack.pop();

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport) {
		logger.debug("merge(siteId:" + siteId + ",root tagName:" + root.getTagName() + ",archivePath:" + archivePath + ",fromSiteId:" + fromSiteId);

		StringBuilder results = new StringBuilder();

		int postCount = 0;

		NodeList postNodes = root.getElementsByTagName(XmlDefs.POST);
		final int numberPosts = postNodes.getLength();

		for (int i = 0; i < numberPosts; i++) {
			Node child = postNodes.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				// Problem
				continue;
			}

			Element postElement = (Element) child;

			Post post = new Post();
			post.fromXml(postElement);
			post.setSiteId(siteId);

			savePost(post);
			postCount++;
		}

		results.append("Stored " + postCount + " posts.");

		return results.toString();
	}

	/**
	 * From EntityProducer
	 */
	public Entity getEntity(Reference ref) {
		if (logger.isDebugEnabled())
			logger.debug("getEntity(Ref ID:" + ref.getId() + ")");

		Entity rv = null;

		try {
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex, reference.length() - lastIndex);
			rv = getPost(postId);
		} catch (Exception e) {
			logger.warn("getEntity(): " + e);
		}

		return rv;
	}

	/**
	 * From EntityProducer
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId) {
		if (logger.isDebugEnabled())
			logger.debug("getEntityAuthzGroups(Ref ID:" + ref.getId() + "," + userId + ")");

		// TODO Auto-generated method stub
		return null;
	}

	public String getEntityDescription(Reference arg0) {
		return null;
	}

	public ResourceProperties getEntityResourceProperties(Reference ref) {
		try {
			String reference = ref.getReference();

			int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
			String postId = reference.substring(lastIndex, reference.length() - lastIndex);
			Entity entity = getPost(postId);
			return entity.getProperties();
		} catch (Exception e) {
			logger.warn("getEntity(): " + e);
			return null;
		}
	}

	/**
	 * From EntityProducer
	 */
	public String getEntityUrl(Reference ref) {
		return getEntity(ref).getUrl();
	}

	/**
	 * From EntityProducer
	 */
	public HttpAccess getHttpAccess() {
		return new HttpAccess() {

			public void handleAccess(HttpServletRequest arg0, HttpServletResponse arg1, Reference arg2, Collection arg3) throws EntityPermissionException, EntityNotDefinedException, EntityAccessOverloadException, EntityCopyrightException {
				try {
					String referenceString = arg2.getReference();
					String postId = referenceString.substring(referenceString.lastIndexOf(Entity.SEPARATOR) + 1);
					Post post = getPost(postId);
					String url = "http://btc224000006.lancs.ac.uk/blog-tool/blog.html?state=post&postId=" + postId + "&siteId=" + post.getSiteId();
					logger.debug("URL:" + url);
					arg1.sendRedirect(url);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

	/**
	 * From EntityProducer
	 */
	public String getLabel() {
		return "blog";
	}

	/**
	 * From EntityProducer
	 */
	public boolean parseEntityReference(String reference, Reference ref) {
		if (!reference.startsWith(LearningLogManager.REFERENCE_ROOT))
			return false;

		return true;
	}

	public boolean willArchiveMerge() {
		return true;
	}

	public String getEntityPrefix() {
		return LearningLogManager.ENTITY_PREFIX;
	}

	public boolean entityExists(String id) {
		String postId = id.substring(id.lastIndexOf(Entity.SEPARATOR));

		try {
			if (persistenceManager.postExists(postId))
				return true;
		} catch (Exception e) {
			logger.error("entityExists threw an exception", e);
		}

		return false;
	}

	public List<BlogMember> getAuthors(String siteId) {
		List<BlogMember> authors = sakaiProxy.getSiteMembers(siteId);
		for (BlogMember author : authors)
			persistenceManager.populateAuthorData(author, siteId);
		return authors;
	}

	public boolean restorePost(String postId) {
		try {
			Post post = persistenceManager.getPost(postId);
			return persistenceManager.restorePost(post);
		} catch (Exception e) {
			logger.error("Caught an exception whilst restoring post '" + postId + "'");
		}

		return false;
	}

	public void sendNewPostAlert(Post post) {
		Set<String> eachList = new TreeSet<String>();

		Set<Role> sakaiRoles = sakaiProxy.getSiteRoles(post.getSiteId());
		for (Role sakaiRole : sakaiRoles) {
			String llRole = persistenceManager.getLLRole(post.getSiteId(), sakaiRole.getId());
			if (Roles.TUTOR.equals(llRole))
				eachList.addAll(sakaiProxy.getUsersInRole(post.getSiteId(), sakaiRole.getId()));
		}

		eachList.remove("admin");

		String siteTitle = sakaiProxy.getSiteTitle(post.getSiteId());

		String message = sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()) + " created a new post titled '" + post.getTitle() + "' in '" + siteTitle + "'\n\nClick on the link below to view it:\n" + post.getUrl();

		sakaiProxy.sendEmailWithMessage(eachList, "[ " + siteTitle + " - LearningLog ] New Post", message);
	}

	public void sendNewCommentAlert(Comment comment) {
		try {
			Post post = getPost(comment.getPostId());

			// We don't really want an email when we comment on our own posts
			if (comment.getCreatorId().equals(post.getCreatorId()))
				return;

			BlogMember author = sakaiProxy.getMember(post.getCreatorId());

			String userId = author.getUserId();

			String siteTitle = sakaiProxy.getSiteTitle(post.getSiteId());

			String message = sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()) + " commented on your post titled '" + post.getTitle() + "' in '" + siteTitle + "'\n\nFollow the link below to view it:\n" + post.getUrl();

			sakaiProxy.sendEmailWithMessage(userId, "[ " + siteTitle + " - LearningLog ] New Comment", message);
		} catch (Exception e) {
			logger.error("Failed to send new comment alert.", e);
		}
	}

	public boolean saveRoles(String siteId, Map<String, String> map) {
		return persistenceManager.saveRoles(siteId, map);
	}

	public Map<String, String> getRoles(String siteId) {
		Map<String, String> map = persistenceManager.getRoles(siteId);

		// If no roles have been mapped yet for this site, return a default
		if (map.size() == 0) {
			Set<Role> roles = sakaiProxy.getSiteRoles(siteId);
			for (Role role : roles)
				map.put(role.getId(), Roles.STUDENT);
		}

		return map;
	}

	public String getCurrentUserRole(String siteId) {
		Role sakaiRole = sakaiProxy.getRoleForCurrentUser(siteId);
		return persistenceManager.getLLRole(siteId, sakaiRole.getId());
	}

	public boolean deleteAttachment(String siteId, String attachmentId, String postId) {
		return persistenceManager.deleteAttachment(siteId, attachmentId, postId);
	}
}
