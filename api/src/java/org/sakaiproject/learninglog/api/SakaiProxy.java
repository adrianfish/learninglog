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
package org.sakaiproject.learninglog.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.sakaiproject.authz.api.Role;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.event.api.NotificationEdit;
import org.sakaiproject.learninglog.api.Attachment;
import org.sakaiproject.learninglog.api.BlogMember;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.site.api.Site;

public interface SakaiProxy {

	public String getCurrentSiteId();

	public String getCurrentUserId();

	public User getUser(String userId) throws UserNotDefinedException;

	public Connection borrowConnection() throws SQLException;

	public void returnConnection(Connection connection);

	public String getVendor();

	public String getDisplayNameForTheUser(String userId);

	public boolean isAutoDDL();

	public List<BlogMember> getSiteMembers(String siteId, PersistenceManager persistenceManager);

	public String getServerUrl();

	public String getPortalUrl();

	public void registerEntityProducer(EntityProducer entityProducer);

	public void registerFunction(String function);

	public void postEvent(String event, String reference);

	public void postEvent(String event, String entityId, String siteId);

	public Set<String> getSiteUsers(String siteId);

	public BlogMember getMember(String memberId);

	public String getSiteTitle(String siteId);

	public String getLearningLogPageId(String siteId);

	public String getLearningLogToolId(String siteId);

	public Set<Role> getSiteRoles(String siteId);

	public Role getRoleForUser(String userId, String siteId);

	public Set<String> getUsersInRole(String siteId, String role);

	/**
	 * Saves the attachment to the current users my workspace resources
	 */
	public void addAttachments(Post post, boolean isPublishing) throws Exception;

	public void getAttachment(Post post, Attachment attachment);

    /**
     * This only gets called by the post editing ui.
     */
	public void deleteAttachment(String name) throws Exception;

    /**
     * This only gets called by the recycle bin ui, and only on draft posts.
     */
	public void deleteAttachments(Post post) throws Exception;

    public NotificationEdit addTransientNotification();

    public boolean canModifyPermissions(String siteId);

    public Set<String> getFellowGroupMembers(String userId, String siteId);

    public Site getSite(String siteId);
}
