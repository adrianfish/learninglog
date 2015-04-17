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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.email.api.DigestService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.learninglog.api.*;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.BaseResourceProperties;

import lombok.Setter;

@Setter
public class SakaiProxyImpl implements SakaiProxy {

	private final Logger logger = Logger.getLogger(SakaiProxyImpl.class);

	private ToolManager toolManager;
	private SessionManager sessionManager;
	private ServerConfigurationService serverConfigurationService;
	private SiteService siteService;
	private UserDirectoryService userDirectoryService;
	private EntityManager entityManager;
	private SqlService sqlService;
	private FunctionManager functionManager;
	private NotificationService notificationService;
	private EventTrackingService eventTrackingService;
	private ContentHostingService contentHostingService;
	private SecurityService securityService;
    private UsageSessionService usageSessionService;
	
	public void init() {}

    public String[] getStrings(String name) {
        return serverConfigurationService.getStrings(name);
    }

	public String getCurrentSiteId() {
		return toolManager.getCurrentPlacement().getContext();
	}

	public String getCurrentUserId() {

		Session session = sessionManager.getCurrentSession();
		String userId = session.getUserId();
		return userId;
	}

	public User getUser(String userId) throws UserNotDefinedException {
        return userDirectoryService.getUser(userId);
	}

	public Connection borrowConnection() throws SQLException {
		return sqlService.borrowConnection();
	}

	public void returnConnection(Connection connection) {
		sqlService.returnConnection(connection);
	}

	public String getVendor() {
		return sqlService.getVendor();
	}

	public String getDisplayNameForTheUser(String userId) {
		
		try {
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getDisplayName();
		} catch (Exception e) {
			return userId; // this can happen if the user does not longer exist
						   // in the system
		}
	}

    private String getEmailForTheUser(String userId) {

        try {
            User sakaiUser = userDirectoryService.getUser(userId);
            return sakaiUser.getEmail();
        } catch (Exception e) {
            return ""; // this can happen if the user does not longer exist in
                       // the system
        }
    }

	public boolean isAutoDDL() {

		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}

	public List<BlogMember> getSiteMembers(String siteId, PersistenceManager persistenceManager) {

		ArrayList<BlogMember> result = new ArrayList<BlogMember>();

		try {
			Site site = siteService.getSite(siteId);
			for (Member siteMember : site.getMembers()) {
				try {
                    String userId = siteMember.getUserId();
					User sakaiUser = userDirectoryService.getUser(userId);
					BlogMember member = new BlogMember(sakaiUser);
                    member.setRole(persistenceManager.getLLRole(siteId, siteMember.getRole().getId()));
					result.add(member);
				} catch (UserNotDefinedException unde) {
					logger.error("Failed to get site member details", unde);
				}
			}
		} catch (Exception e) {
			logger.error("Exception thrown whilst getting site members", e);
		}

		return result;
	}

	public String getServerUrl() {
		return serverConfigurationService.getServerUrl();
	}

	public String getPortalUrl() {
		return serverConfigurationService.getPortalUrl();
	}

	public void registerEntityProducer(EntityProducer entityProducer) {
		entityManager.registerEntityProducer(entityProducer, Constants.ENTITY_PREFIX);
	}

	public void registerFunction(String function) {

		List functions = functionManager.getRegisteredFunctions("learninglog.");

		if (!functions.contains(function)) {
			functionManager.registerFunction(function);
		}
	}

	public void postEvent(String event, String reference) {

        UsageSession usageSession = usageSessionService.getSession();
        eventTrackingService.post(eventTrackingService.newEvent(event, reference, true, NotificationService.NOTI_OPTIONAL), usageSession);
	}

	public void postEvent(String event, String reference, String siteId) {
		eventTrackingService.post(eventTrackingService.newEvent(event, reference, siteId, true, NotificationService.NOTI_OPTIONAL));
	}

	public Set<String> getSiteUsers(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			return site.getUsers();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public BlogMember getMember(String memberId) {

		User user;
		try {
			user = userDirectoryService.getUser(memberId);
			BlogMember member = new BlogMember(user);

			return member;
		} catch (UserNotDefinedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getSiteTitle(String siteId) {

		try {
			return siteService.getSite(siteId).getTitle();
		} catch (Exception e) {
			logger.error("Caught exception whilst getting site title", e);
		}

		return "";
	}

	public String getLearningLogPageId(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.learninglog");
			return tc.getPageId();
		} catch (Exception e) {
			return "";
		}
	}

	public String getLearningLogToolId(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.learninglog");
			return tc.getId();
		} catch (Exception e) {
			return "";
		}
	}

	public Set<Role> getSiteRoles(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			return site.getRoles();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public Role getRoleForUser(String userId, String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			return site.getUserRole(userId);
		} catch (IdUnusedException iue) {
			logger.error("There is no site with id '" + siteId + "'. Null will be returned.");
		}

		return null;
	}

	public Set<String> getUsersInRole(String siteId, String role) {

		try {
			Site site = siteService.getSite(siteId);
			return site.getUsersHasRole(role);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Saves the attachment to the current users my workspace resources
	 */
	public void addAttachments(Post post, boolean isPublishing) throws Exception {

        String creatorId = post.getCreatorId();

        for(Attachment attachment : post.getAttachments()) {
		
            String name = attachment.name;
            String mimeType = attachment.mimeType;
            byte[] fileData = attachment.data;

            if (name == null | name.length() == 0) {
                throw new IllegalArgumentException("The name argument must be populated.");
            }

            String resourceId = ContentHostingService.COLLECTION_USER + creatorId + "/learninglog-draft-files/" + attachment.name;

            if (name.endsWith(".doc")) {
                mimeType = "application/msword";
            } else if (name.endsWith(".xls")) {
                mimeType = "application/excel";
            }

            try {

                if(isPublishing) {

                    try {
                        // This may be a new attachment and thus may never have
                        // been added to the draft files area
                        ContentResource resource = contentHostingService.getResource(resourceId);
                        fileData = resource.getContent();
                        mimeType = resource.getContentType();
                        contentHostingService.removeResource(resourceId);
                    } catch (Exception e) { }

                    // Now set the resource id to the published one.
                    resourceId = contentHostingService.getDropboxCollection(post.getSiteId()) + attachment.name;
                }

                ContentResourceEdit resource = contentHostingService.addResource(resourceId);
                resource.setContentType(mimeType);
                resource.setContent(fileData);
                ResourceProperties props = new BaseResourceProperties();
                props.addProperty(ResourceProperties.PROP_CONTENT_TYPE, mimeType);
                props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);
                props.addProperty(ResourceProperties.PROP_CREATOR, creatorId);
                props.addProperty(ResourceProperties.PROP_ORIGINAL_FILENAME, name);
                resource.getPropertiesEdit().set(props);
                contentHostingService.commitResource(resource, NotificationService.NOTI_NONE);
            } catch (IdUsedException e) {

                if (logger.isInfoEnabled()) {
                    logger.info("A resource with id '" + resourceId + "' exists already. Returning id without recreating ...");
                }
		    }
        }
	}

	public void getAttachment(Post post, Attachment attachment) {

        String creatorId = post.getCreatorId();

        String currentUserId = getCurrentUserId();

        String resourceId = null;

        if(post.isPrivate() && creatorId.equals(currentUserId)) {
		    resourceId = ContentHostingService.COLLECTION_USER + creatorId + "/learninglog-draft-files/" + attachment.name;
        } else if(!post.isPrivate()) {

			resourceId = contentHostingService.getDropboxCollection(post.getSiteId());
            
            if(creatorId.equals(currentUserId)) {
			    resourceId += attachment.name;
            } else {
			    resourceId += creatorId + "/" + attachment.name;
            }
        }

		try {
			enableSecurityAdvisor();
			ContentResource resource = contentHostingService.getResource(resourceId);
			ResourceProperties properties = resource.getProperties();
			attachment.mimeType = properties.getProperty(ResourceProperties.PROP_CONTENT_TYPE);
			attachment.name = properties.getProperty(ResourceProperties.PROP_DISPLAY_NAME);
			attachment.url = resource.getUrl();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Caught an exception with message '" + e.getMessage() + "'");
		} finally {
			disableSecurityAdvisor();
		}
	}

    /**
     * This only gets called by the post editing ui.
     */
	public void deleteAttachment(String name) throws Exception {

        String resourceId = ContentHostingService.COLLECTION_USER + getCurrentUserId() + "/learninglog-draft-files/" + name;
	    contentHostingService.removeResource(resourceId);
	}

    /**
     * This only gets called by the recycle bin ui, and only on draft posts.
     */
	public void deleteAttachments(Post post) throws Exception {

        String creatorId = post.getCreatorId();
        String currentUserId = getCurrentUserId();
        String baseDropBoxResourceId = contentHostingService.getDropboxCollection(post.getSiteId()) + creatorId + "/";

        for(Attachment attachment : post.getAttachments()) {

            String resourceId = null;

            if(post.isPrivate() && creatorId.equals(currentUserId)) {
		        resourceId = ContentHostingService.COLLECTION_USER + creatorId + "/learninglog-draft-files/" + attachment.name;
            } else if(!post.isPrivate()) {

                resourceId = baseDropBoxResourceId + attachment.name;
            }

            contentHostingService.removeResource(resourceId);
        }
	}

	private void enableSecurityAdvisor() {

		securityService.pushAdvisor(new SecurityAdvisor() {

			public SecurityAdvice isAllowed(String userId, String function, String reference) {
				return SecurityAdvice.ALLOWED;
			}
		});
	}

	private void disableSecurityAdvisor() {
		securityService.popAdvisor();
	}

    public NotificationEdit addTransientNotification() {
        return notificationService.addTransientNotification();
    }

    public boolean canModifyPermissions(String siteId) {
        return securityService.unlock(BlogFunctions.BLOG_MODIFY_PERMISSIONS, "/site/" + siteId)
            || securityService.unlock(SiteService.SECURE_UPDATE_SITE, "/site/" + siteId);
    }

    public Site getSite(String siteId) {

		try {
			return siteService.getSite(siteId);
		} catch (Exception e) {
			e.printStackTrace();
            return null;
		}
    }

    public Set<String> getFellowGroupMembers(String userId, String siteId) {

        Site site = getSite(siteId);

        Set<String> fellowMembers = new HashSet<String>();

        for (Group group : site.getGroupsWithMember(userId)) {
            fellowMembers.addAll(group.getUsers());
        }

        fellowMembers.remove(userId);

        return fellowMembers;
    }
}
