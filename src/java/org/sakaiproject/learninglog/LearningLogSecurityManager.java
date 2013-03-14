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

package org.sakaiproject.learninglog;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.Roles;

public class LearningLogSecurityManager
{
    private Logger logger = Logger.getLogger(LearningLogSecurityManager.class);
    
	private SakaiProxy sakaiProxy;
	
	private PersistenceManager persistenceManager;

    public LearningLogSecurityManager(SakaiProxy sakaiProxy,PersistenceManager persistenceManager)
    {
    	this.sakaiProxy = sakaiProxy;
    	this.persistenceManager = persistenceManager;
    }

    public boolean canCurrentUserCommentOnPost(Post post)
	{
    	if(logger.isDebugEnabled()) logger.debug("canCurrentUserCommentOnPost()");
    	
		//if(sakaiProxy.isOnGateway() && post.isPublic() && post.isCommentable())
			//return true;
    	
    	String siteId = post.getSiteId();
		
		// If the post is comment-able and the current user is a tutor in the post's site
		if(post.isReady() && Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId())))
			return true;
		
		// An author can always comment on their own posts
		if(post.isReady() && post.getCreatorId().equals(sakaiProxy.getCurrentUserId()))
			return true;
		
		return false;
	}
	
	public boolean canCurrentUserDeletePost(Post post) throws SecurityException
	{
    	String siteId = post.getSiteId();
    	
		if(Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId())))
			return true;
		
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is the author and the post is not yet ready
		if(!post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId()))
			return true;
		
		return false;
	}
	
	public boolean canCurrentUserEditPost(Post post)
	{
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(post.isReady() && currentUser != null && currentUser.equals(post.getCreatorId()))
			return true;
		
		return false;
	}

	/**
	 * Tests whether the current user can read each Post and if not, filters
	 * that post out of the resulting list
	 */
	public List<Post> filter(List<Post> posts)
	{
		List<Post> filtered = new ArrayList<Post>();
		for(Post post : posts)
		{
			if(canCurrentUserReadPost(post))
			{
				filtered.add(post);
			}
		}
		
		return filtered;
	}
	
	public boolean canCurrentUserReadPost(Post post)
	{
		String threadName = Thread.currentThread().getName();
		
		if(!post.isPrivate() && "IndexManager".equals(threadName)) return true;
		
    	String siteId = post.getSiteId();
		
		// Only tutors can view recycled posts
		if(post.isRecycled()
			&& Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId())))
			return true;
			
		if(post.isReady()
			&& (Roles.TUTOR.equals(persistenceManager.getLLRole(siteId, sakaiProxy.getRoleForCurrentUser(siteId).getId()))))
			return true;
		
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(currentUser != null && currentUser.equals(post.getCreatorId()))
			return true;
		
		return false;
	}

	public boolean canCurrentUserSavePost(Post post)
	{
		String currentUser = sakaiProxy.getCurrentUserId();
		
		// If the current user is authenticated and the post author, yes.
		if(currentUser != null && currentUser.equals(post.getCreatorId()))
			return true;
		
		return false;
	}
}
