package org.sakaiproject.learninglog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.learninglog.api.BlogFunctions;
import org.sakaiproject.learninglog.api.BlogMember;
import org.sakaiproject.learninglog.api.Comment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;
import org.sakaiproject.learninglog.api.Roles;
import org.sakaiproject.learninglog.api.Visibilities;

import lombok.Setter;

@Setter
public class LearningLogManager {

	private final Logger logger = Logger.getLogger(LearningLogManager.class);

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

	public void init() {
		sakaiProxy.registerFunction(BlogFunctions.BLOG_MODIFY_PERMISSIONS);
	}

	public Post getPost(String postId) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("getPost(" + postId + ")");
        }

		Post post = persistenceManager.getPost(postId);
		if (securityManager.canCurrentUserReadPost(post)) {
			return post;
        } else {
			throw new Exception("The current user does not have permissions to read this post.");
        }
	}

	public List<Post> getPosts(String placementId) throws Exception {

		List<Post> unfiltered = persistenceManager.getAllPost(placementId);
		return securityManager.filter(unfiltered);
	}

	public List<Post> getPosts(QueryBean query) throws Exception {

		List<Post> unfiltered = persistenceManager.getPosts(query);
		return securityManager.filter(unfiltered);
	}

	public boolean savePost(Post post, boolean isPublishing) {

		try {
			if (securityManager.canCurrentUserSavePost(post)) {
				return persistenceManager.savePost(post, isPublishing);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst creating post", e);
		}

		return false;
	}

	public boolean deletePost(String postId) {

		try {
			Post post = persistenceManager.getPost(postId);
			if (securityManager.canCurrentUserDeletePost(post)) {
				return persistenceManager.deletePost(post);
			}
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
			return persistenceManager.deleteComment(commentId);
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

	public List<BlogMember> getAuthors(String siteId) {

		List<BlogMember> authors = sakaiProxy.getSiteMembers(siteId);
		for (BlogMember author : authors) {
			persistenceManager.populateAuthorData(author, siteId);
		}
		return authors;
	}

	public boolean restorePost(String postId) {

		try {
			return persistenceManager.restorePost(postId);
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
			if (Roles.TUTOR.equals(llRole)) {
				eachList.addAll(sakaiProxy.getUsersInRole(post.getSiteId(), sakaiRole.getId()));
			}
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
			if (comment.getCreatorId().equals(post.getCreatorId())) {
				return;
			}

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
			for (Role role : roles) {
				map.put(role.getId(), Roles.STUDENT);
			}
		}

		return map;
	}

	public String getCurrentUserRole(String siteId) {

		Role sakaiRole = sakaiProxy.getRoleForCurrentUser(siteId);
		String llRole = persistenceManager.getLLRole(siteId, sakaiRole.getId());
        if(llRole == null) {
            logger.info("There is no LL Role for the current user. Returning 'Student' ...");
            llRole = "Student";
        }
		return llRole;
	}

	public boolean deleteAttachment(String siteId, String name, String postId) {
		return persistenceManager.deleteAttachment(siteId, name, postId);
	}
}
