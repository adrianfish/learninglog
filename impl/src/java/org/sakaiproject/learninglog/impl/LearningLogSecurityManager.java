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

import org.apache.log4j.Logger;

import org.sakaiproject.authz.api.Role;
import org.sakaiproject.learninglog.api.*;

import lombok.Setter;

@Setter
public class LearningLogSecurityManager {

    private final Logger logger = Logger.getLogger(LearningLogSecurityManager.class);
    
	private SakaiProxy sakaiProxy;
	private PersistenceManager persistenceManager;

    public void init() { }

    public boolean canCurrentUserCommentOnPost(Post post) {

    	if(logger.isDebugEnabled()) logger.debug("canCurrentUserCommentOnPost()");
    	
    	String siteId = post.getSiteId();
		
		// If the post is comment-able and the current user is a tutor in the post's site
		if(post.isReady() && Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId()))) {
			return true;
        }
		
		// An author can always comment on their own posts
		if(post.isReady() && post.getCreatorId().equals(sakaiProxy.getCurrentUserId())) {
			return true;
        }
		
		return false;
	}
	
	public boolean canCurrentUserDeletePost(Post post) throws SecurityException {

    	String siteId = post.getSiteId();
    	
		if(Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId()))) {
			return true;
        }
		
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is the author and the post is not yet ready
		if(!post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
        }
		
		return false;
	}
	
	public boolean canCurrentUserEditPost(Post post) {

		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId())) {
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
		
		if(!post.isPrivate() && "IndexManager".equals(threadName)) return true;
		
    	String siteId = post.getSiteId();
		
	    String llRole = persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId());

		// Only tutors can view recycled posts
		if(post.isRecycled() && Roles.TUTOR.equals(llRole)) {
			return true;
        }
			
		if(post.isReady() && Roles.TUTOR.equals(llRole)) {
			return true;
        }
		
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(currentUser != null && currentUser.equals(post.getCreatorId())) {
			return true;
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

        Role sakaiRole = sakaiProxy.getRoleForCurrentUser(siteId);

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
