package org.sakaiproject.learninglog.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.NotificationEdit;

import org.sakaiproject.learninglog.api.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.Setter;

@Setter
public class LearningLogManagerImpl implements LearningLogManager {

	private final Logger logger = Logger.getLogger(LearningLogManagerImpl.class);

	private PersistenceManager persistenceManager;
	private LearningLogSecurityManager securityManager;
	private SakaiProxy sakaiProxy;

	public void init() {

		sakaiProxy.registerFunction(BlogFunctions.BLOG_MODIFY_PERMISSIONS);
		sakaiProxy.registerEntityProducer(this);

        NotificationEdit ne1 = sakaiProxy.addTransientNotification();
        ne1.setResourceFilter(Constants.REFERENCE_ROOT);
        ne1.setFunction(Constants.BLOG_POST_CREATED);
        NewPostNotification npn = new NewPostNotification();
        npn.setSakaiProxy(sakaiProxy);
        npn.setPersistenceManager(persistenceManager);
        ne1.setAction(npn);

        NotificationEdit ne2 = sakaiProxy.addTransientNotification();
        ne2.setResourceFilter(Constants.REFERENCE_ROOT);
        ne2.setFunction(Constants.BLOG_COMMENT_CREATED);
        NewCommentNotification ncn = new NewCommentNotification();
        ncn.setSakaiProxy(sakaiProxy);
        ncn.setPersistenceManager(persistenceManager);
        ne2.setAction(ncn);
	}

	public Post getPost(String postId) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("getPost(" + postId + ")");
        }

		Post post = persistenceManager.getPost(postId);
		if (securityManager.canCurrentUserReadPost(post)) {
            post.setComments(securityManager.filterComments(post));
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

	public Comment getComment(String commentId) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("getComment(" + commentId + ")");
        }

		Comment comment = persistenceManager.getComment(commentId);
		if (securityManager.canCurrentUserReadComment(comment, null)) {
			return comment;
        } else {
			throw new Exception("The current user does not have permissions to read this comment.");
        }
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

		List<BlogMember> authors = securityManager.filterAuthors(sakaiProxy.getSiteMembers(siteId));
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

        try {
            sakaiProxy.postEvent(Constants.BLOG_POST_CREATED, post.getReference(), post.getSiteId());
        } catch(Exception e) {
            logger.error("Failed to post post created event",e);
        }
	}

	public void sendNewCommentAlert(Comment comment) {

		try {
			Post post = getPost(comment.getPostId());

			// We don't really want an email when we comment on our own posts
			if (comment.getCreatorId().equals(post.getCreatorId())) {
				return;
			}

            sakaiProxy.postEvent(Constants.BLOG_COMMENT_CREATED, comment.getReference());
		} catch (Exception e) {
			logger.error("Failed to post new comment event.", e);
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

		Role sakaiRole = sakaiProxy.getRoleForUser(sakaiProxy.getCurrentUserId(), siteId);
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

    public boolean setGroupMode(String siteId, String groupMode) {
		return persistenceManager.setGroupMode(siteId, groupMode);
    }

    public boolean isGroupMode(String siteId) {
		return persistenceManager.isGroupMode(siteId);
    }

    /** START EntityProducer IMPL */

    public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments) {
        return null;
    }

    public Entity getEntity(Reference reference) {

        String referenceString = reference.getReference();

        String[] parts = referenceString.split(Entity.SEPARATOR);

        if (!parts[1].equals(Constants.ENTITY_PREFIX)) {
            return null;
        }

        String type = parts[3];

        String entityId = parts[4];

        try {
            if ("post".equals(type)) {
                return getPost(entityId);
            } else if ("comments".equals(type)) {
                return getComment(entityId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Collection getEntityAuthzGroups(Reference ref, String userId) {

        List ids = new ArrayList();
        ids.add("/site/" + ref.getContext());
        return ids;
    }

    public String getEntityDescription(Reference arg0) {
        return null;
    }

    public ResourceProperties getEntityResourceProperties(Reference arg0) {
        return null;
    }

    public String getEntityUrl(Reference reference) {
        return null;
    }

    public HttpAccess getHttpAccess() {
        return null;
    }

    public String getLabel() {
        return Constants.ENTITY_PREFIX;
    }

    public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport) {
        return null;
    }

    public boolean parseEntityReference(String referenceString, Reference reference) {

        if(logger.isDebugEnabled()) {
            logger.debug("parseEntityReference(\"" + referenceString + "\")");
        }

        String[] parts = referenceString.split(Entity.SEPARATOR);

        if (parts.length < 2 || !parts[1].equals(Constants.ENTITY_PREFIX)) {
            return false;
        }

        if(parts.length == 2) {
            reference.set("sakai:learninglog", "", "", null, "");
            return true;
        }

        String siteId = parts[2];
        String type = parts[3];
        String entityId = parts[4];

        if ("post".equals(type)) {

            reference.set(Constants.ENTITY_PREFIX,"post" , entityId, null, siteId);
            return true;
        } else if ("comments".equals(type)) {

            reference.set(Constants.ENTITY_PREFIX,"comments" , entityId, null, siteId);
            return true;
        }

        return false;
    }

    public boolean willArchiveMerge() {
        return false;
    }

    /** END EntityProducer IMPL */
}
