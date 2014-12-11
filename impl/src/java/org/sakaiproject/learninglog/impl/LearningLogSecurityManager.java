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

package org.sakaiproject.learninglog.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import org.sakaiproject.authz.api.Role;
import org.sakaiproject.learninglog.api.*;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;

import lombok.Setter;

@Setter
public class LearningLogSecurityManager {

    private final Logger logger = Logger.getLogger(LearningLogSecurityManager.class);
    
	private SakaiProxy sakaiProxy;
	private PersistenceManager persistenceManager;

    public void init() { }

    public boolean canCurrentUserCommentOnPost(Post post) {

    	logger.debug("canCurrentUserCommentOnPost()");
    	
    	String siteId = post.getSiteId();

        String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the post is comment-able and the current user is a tutor in the post's site
		if (post.isReady() && Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForUser(currentUser, siteId).getId()))) {
			return true;
        }
		
		// An author can always comment on their own posts
		if (post.isReady() && post.getCreatorId().equals(currentUser)) {
			return true;
        }
		
		return false;
	}
	
	public boolean canCurrentUserDeletePost(Post post) throws SecurityException {

    	String siteId = post.getSiteId();

        String currentUser = sakaiProxy.getCurrentUserId();
    	
		if (Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForUser(currentUser, siteId).getId()))) {
			return true;
        }
		
		// If the current user is the author and the post is not yet ready
		if (!post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
        }
		
		return false;
	}
	
	public boolean canCurrentUserEditPost(Post post) {

		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if (post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
        }
		
		return false;
	}

	/**
	 * Tests whether the current user can read each Post and if not, filters
	 * that post out of the resulting list
	 */
	public List<Post> filter(List<Post> posts) {

		List<Post> filtered = new ArrayList<Post>();

		for(Post post : posts) {

			if(canCurrentUserReadPost(post)) {
                post.setComments(filterComments(post));
				filtered.add(post);
			}
		}
		
		return filtered;
	}
	
	public boolean canCurrentUserReadPost(Post post) {

		String threadName = Thread.currentThread().getName();
		
		if (!post.isPrivate() && "IndexManager".equals(threadName)) return true;
		
    	String siteId = post.getSiteId();

		String currentUser = sakaiProxy.getCurrentUserId();

		// If the current user is authenticated and the post author, yes.
		if (currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
        }

		if (post.isRecycled() || post.isReady()) {
            if (persistenceManager.isGroupMode(siteId)) {
                // Is the current user a tutor in any of the groups which the author
                // belongs to?

                Set<String> tutors = persistenceManager.getTutorsForStudent(post.getCreatorId(), siteId);

                if (tutors.contains(currentUser)) {
                    return true;
                }
            } else {
		
                String llRole = persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForUser(currentUser, siteId).getId());

                // Only tutors can view recycled posts
                if (Roles.TUTOR.equals(llRole)) {
                    return true;
                }
            }
        }
		
		return false;
	}

	/**
	 * Tests whether the current user can read each Comment and if not, filters
	 * that comment out of the resulting list
	 */
	public List<Comment> filterComments(Post post) {

		List<Comment> filtered = new ArrayList<Comment>();

		for(Comment comment : post.getComments()) {

			if(canCurrentUserReadComment(comment, post)) {
				filtered.add(comment);
			}
		}
		
		return filtered;
	}

    public List<BlogMember> filterAuthors(List<BlogMember> unfiltered, String siteId) {

        Site site = sakaiProxy.getSite(siteId);

        List<String> userIds = new ArrayList<String>();

        if (site == null) {
            return new ArrayList<BlogMember>();
        } else {
            String currentUserId = sakaiProxy.getCurrentUserId();

            if (site.getMember(currentUserId) == null) {
                return new ArrayList<BlogMember>();
            } else {
                if (persistenceManager.isGroupMode(siteId)) {
                    for (Group group : site.getGroupsWithMember(currentUserId)) {
                        userIds.addAll(group.getUsers());
                    }
                } else {
                    userIds.addAll(site.getUsers());
                }
            }

            List<BlogMember> filtered = new ArrayList<BlogMember>();

            for (BlogMember unfilteredMember : unfiltered) {
                if (unfilteredMember.isStudent()) {
                    if (userIds.contains(unfilteredMember.getUserId())) {
                        filtered.add(unfilteredMember);
                    }
                }
            }

            return filtered;
        }
    }

	public boolean canCurrentUserReadComment(Comment comment, Post post) {

        if(comment == null) {

            logger.warn("Null comment supplied. Returning false ...");
            return false;
        }

        String currentUserId = sakaiProxy.getCurrentUserId();

        if(currentUserId == null) {
            logger.warn("Not logged in. Returning false ...");
            return false;
        }

    	String siteId = comment.getSiteId();

        Role sakaiRole = sakaiProxy.getRoleForUser(currentUserId, siteId);

        if(sakaiRole == null) {
            logger.warn("No role. Returning false ...");
            return false;
        }

        if(post == null) {
            try {
                post = persistenceManager.getPost(comment.getPostId());
            } catch (Exception e) {
                logger.error("Failed to get post.", e);
                return false;
            }
        }

	    String llRole = persistenceManager.getLLRole(siteId, sakaiRole.getId());

        if(post.isRecycled() && Roles.TUTOR.equals(llRole)) {
            // The enclosing post has been recycled and I *am* a tutor. I can see it.
            return true;
        }

        if(currentUserId.equals(comment.getCreatorId())) {
            // I wrote it. I can see it.
            return true;
        } else if(comment.isReady() && (Roles.TUTOR.equals(llRole) || post.getCreatorId().equals(currentUserId))) {
            // It's ready and I'm a tutor OR this is my post. I can see it.
            return true;
        }

		return false;
	}

	public boolean canCurrentUserSavePost(Post post) {

		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
        }
		
		return false;
	}
}
