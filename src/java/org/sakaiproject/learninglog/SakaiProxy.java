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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.FunctionManager;
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
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.learninglog.api.Attachment;
import org.sakaiproject.learninglog.api.BlogMember;
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

public class SakaiProxy {

	private Logger logger = Logger.getLogger(SakaiProxy.class);

	private ToolManager toolManager;
	private SessionManager sessionManager;
	private ServerConfigurationService serverConfigurationService;
	private SiteService siteService;
	private UserDirectoryService userDirectoryService;
	private EntityManager entityManager;
	private SqlService sqlService;
	private FunctionManager functionManager;
	private EventTrackingService eventTrackingService;
	private EmailService emailService;
	private DigestService digestService;
	private ContentHostingService contentHostingService;
	private SecurityService securityService;
	
	public SakaiProxy() {
		toolManager = (ToolManager) ComponentManager.get(ToolManager.class);
		sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
		serverConfigurationService = (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
		siteService = (SiteService) ComponentManager.get(SiteService.class);
		userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
		entityManager = (EntityManager) ComponentManager.get(EntityManager.class);
		entityManager = (EntityManager) ComponentManager.get(EntityManager.class);
		sqlService = (SqlService) ComponentManager.get(SqlService.class);
		functionManager = (FunctionManager) ComponentManager.get(FunctionManager.class);
		eventTrackingService = (EventTrackingService) ComponentManager.get(EventTrackingService.class);
		emailService = (EmailService) ComponentManager.get(EmailService.class);
		digestService = (DigestService) ComponentManager.get(DigestService.class);
		contentHostingService = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
		securityService = (SecurityService) ComponentManager.get(SecurityService.class);
	}

	public String getCurrentSiteId() {

		return toolManager.getCurrentPlacement().getContext(); // equivalent to
															   // PortalService.getCurrentSiteId();
	}

	public String getCurrentToolId() {

		return toolManager.getCurrentPlacement().getId();
	}

	public String getCurrentUserId() {

		Session session = sessionManager.getCurrentSession();
		String userId = session.getUserId();
		return userId;
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

	public List<BlogMember> getSiteMembers(String siteId) {
		ArrayList<BlogMember> result = new ArrayList<BlogMember>();
		try {
			Site site = siteService.getSite(siteId);
			Set<String> userIds = site.getUsers();
			for (String userId : userIds) {
				try {
					User sakaiUser = userDirectoryService.getUser(userId);
					BlogMember member = new BlogMember(sakaiUser);
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
		entityManager.registerEntityProducer(entityProducer, "blog");
	}

	public void registerFunction(String function) {
		List functions = functionManager.getRegisteredFunctions("blog.");

		if (!functions.contains(function)) {
			functionManager.registerFunction(function);
		}
	}

	public void sendEmailWithMessage(String user, String subject, String message) {
		Set<String> users = new HashSet<String>(1);
		users.add(user);
		sendEmailWithMessage(users, subject, message);

	}

	public void sendEmailWithMessage(Set<String> users, String subject, String message) {
		sendEmailToParticipants(users, subject, message);
	}

	public void addDigestMessage(String userId, String subject, String message) {
		try {
			digestService.digest(userId, subject, message);
		} catch (Exception e) {
			logger.error("Failed to add message to digest.", e);
		}
	}

	public void addDigestMessage(Set<String> users, String subject, String message) {
		for (String userId : users) {
			try {
				digestService.digest(userId, subject, message);
			} catch (Exception e) {
				logger.error("Failed to add message to digest.", e);
			}
		}
	}

	private void sendEmailToParticipants(Set<String> to, String subject, String text) {
		class EmailSender implements Runnable {

			private Thread runner;

			private String sender;

			private String subject;

			private String text;

			private Set<String> participants;

			public EmailSender(Set<String> to, String subject, String text) {
				this.sender = serverConfigurationService.getString("ui.service", "Sakai") + " <no-reply@" + serverConfigurationService.getServerName() + ">";
				this.participants = to;
				this.text = text;
				this.subject = subject;
				runner = new Thread(this, "LearningLog Emailer Thread");
				runner.start();
			}

			public synchronized void run() {
				List<String> additionalHeader = new ArrayList<String>();
				additionalHeader.add("Content-Type: text/plain");

				String emailSender = getEmailForTheUser(sender);
				if (emailSender == null || emailSender.trim().equals("")) {
					emailSender = getDisplayNameForTheUser(sender);
				}

				for (String userId : participants) {
					String emailParticipant = getEmailForTheUser(userId);
					try {
						// TODO: This should all be parameterised and
						// internationalised.
						// logger.info("Sending email to " + participantId +
						// " ...");
						emailService.send(emailSender, emailParticipant, subject, text, emailParticipant, sender, additionalHeader);
					} catch (Exception e) {
						System.out.println("Failed to send email to '" + userId + "'. Message: " + e.getMessage());
					}
				}
			}
		}

		new EmailSender(to, subject, text);
	}

	public void postEvent(String event, String entityId, String siteId) {
		eventTrackingService.post(eventTrackingService.newEvent(event, entityId, siteId, true, NotificationService.NOTI_OPTIONAL));
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

	public Role getRoleForCurrentUser(String siteId) {
		try {
			Site site = siteService.getSite(siteId);
			return site.getUserRole(getCurrentUserId());
		} catch (Exception e) {
			e.printStackTrace();
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

	public String getSakaiSkin() {
		// Shouldn't have to do any of this fudging. getSiteSkin should do it.
		String defaultSkin = serverConfigurationService.getString("skin.default", "default");
		String siteSkin = siteService.getSiteSkin(getCurrentSiteId());
		String templates = serverConfigurationService.getString("portal.templates", "neoskin");
		if ("neoskin".equals(templates)) {
			String prefix = serverConfigurationService.getString("portal.neoprefix", "neo-");
			defaultSkin = prefix + defaultSkin;
			if (siteSkin != null)
				siteSkin = prefix + siteSkin;
		}

		return siteSkin != null ? siteSkin : defaultSkin;
	}

	/**
	 * Saves the file to Sakai's content hosting
	 */
	public void addAttachment(String siteId, String creatorId, Attachment attachment) throws Exception {
		
		int id = attachment.getId();
		String name = attachment.getName();
		String mimeType = attachment.getMimeType();
		byte[] fileData = attachment.getData();

		if (name == null | name.length() == 0)
			throw new IllegalArgumentException("The name argument must be populated.");
		
		String resourceId = "/group/" + siteId + "/learninglog-files/" + id;
		
		try {
			contentHostingService.checkResource(resourceId);
			// Already exists. Return.
			return;
		} catch (IdUnusedException idue) {}

		if (name.endsWith(".doc"))
			mimeType = "application/msword";
		else if (name.endsWith(".xls"))
			mimeType = "application/excel";

		try {
			enableSecurityAdvisor();

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
			if (logger.isInfoEnabled())
				logger.info("A resource with id '" + resourceId + "' exists already. Returning id without recreating ...");
		} finally {
			disableSecurityAdvisor();
		}
	}

	public void getAttachment(String siteId, Attachment attachment) {
		if (siteId == null)
			siteId = getCurrentSiteId();

		try {
			enableSecurityAdvisor();
			String id = "/group/" + siteId + "/learninglog-files/" + attachment.getId();
			ContentResource resource = contentHostingService.getResource(id);
			ResourceProperties properties = resource.getProperties();
			attachment.setMimeType(properties.getProperty(ResourceProperties.PROP_CONTENT_TYPE));
			attachment.setName(properties.getProperty(ResourceProperties.PROP_DISPLAY_NAME));
			attachment.setUrl(resource.getUrl());
		} catch (Exception e) {
			//if (logger.isDebugEnabled())
				e.printStackTrace();

			logger.error("Caught an exception with message '" + e.getMessage() + "'");
		} finally {
			disableSecurityAdvisor();
		}
	}

	public void deleteAttachment(String siteId, String id) throws Exception {
		enableSecurityAdvisor();
		try {
			String resourceId = "/group/" + siteId + "/learninglog-files/" + id;
			contentHostingService.removeResource(resourceId);
		} finally {
			disableSecurityAdvisor();
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
}
