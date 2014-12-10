package org.sakaiproject.learninglog.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SiteEmailNotification;

import org.sakaiproject.learninglog.api.SakaiProxy;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.Roles;

import lombok.Setter;

public class NewPostNotification extends SiteEmailNotification {
	
	private static ResourceLoader rb = new ResourceLoader("org.sakaiproject.learninglog.bundle.newpostnotification");
	
    @Setter
	private SakaiProxy sakaiProxy;

    @Setter
	private PersistenceManager persistenceManager;
	
	public NewPostNotification() {}
	
    public NewPostNotification(String siteId) {
        super(siteId);
    }
    
    protected String getFromAddress(Event event) {

        String userEmail = "no-reply@" + ServerConfigurationService.getServerName();
        String userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
        String no_reply = "From: \"" + userDisplay + "\" <" + userEmail + ">";
        String from = getFrom(event);
        String userId = event.getUserId();

        //checks if "from" email id has to be included? and whether the notification is a delayed notification?. SAK-13512
        if ((ServerConfigurationService.getString("emailFromReplyable@org.sakaiproject.event.api.NotificationService").equals("true")) && from.equals(no_reply) && userId !=null){

                try {

                    User u = UserDirectoryService.getUser(userId);
                    userDisplay = u.getDisplayName();
                    userEmail = u.getEmail();
                    if ((userEmail != null) && (userEmail.trim().length()) == 0) userEmail = null;

                } catch (UserNotDefinedException e) {
                }

                // some fallback positions
                if (userEmail == null) userEmail = "no-reply@" + ServerConfigurationService.getServerName();
                if (userDisplay == null) userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
                from="From: \"" + userDisplay + "\" <" + userEmail + ">";
        }

        return from;
    }
    
	protected String plainTextContent(Event event) {

		Reference ref = EntityManager.newReference(event.getResource());
        Post post = (Post) ref.getEntity();
        
		String creatorName = "";
		String siteTitle = "";
		try {
			creatorName = UserDirectoryService.getUser(post.getCreatorId()).getDisplayName();
			siteTitle = SiteService.getSite(post.getSiteId()).getTitle();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return rb.getFormattedMessage("noti.newpost", new Object[]{creatorName, post.getTitle(), siteTitle, post.getUrl()});
	}
	
	protected String getSubject(Event event) {

        String siteId = event.getContext();
        
        String siteTitle = "";
		try {
			siteTitle = SiteService.getSite(siteId).getTitle();
		} catch (IdUnusedException e) {
			e.printStackTrace();
		}
        
        return rb.getFormattedMessage("noti.subject", new Object[]{siteTitle});
	}
	
	protected List<User> getRecipients(Event event) {

        String siteId = event.getContext();

        String userId = event.getUserId();

		Set<String> tutorUserIds = new TreeSet<String>();

        if (persistenceManager.isGroupMode(siteId)) {
            Set<String> fellowMembers = sakaiProxy.getFellowGroupMembers(userId, siteId);
            for (String fellowMember : fellowMembers) {
                String sakaiRoleId = sakaiProxy.getRoleForUser(fellowMember, siteId).getId();
                String llRole = persistenceManager.getLLRole(siteId, sakaiRoleId);
                if (Roles.TUTOR.equals(llRole)) {
                    tutorUserIds.add(fellowMember);
                }
            }
        } else {
            Set<Role> sakaiRoles = sakaiProxy.getSiteRoles(siteId);
            for (Role sakaiRole : sakaiRoles) {
                String sakaiRoleId = sakaiRole.getId();
                String llRole = persistenceManager.getLLRole(siteId, sakaiRoleId);
                if (Roles.TUTOR.equals(llRole)) {
                    tutorUserIds.addAll(sakaiProxy.getUsersInRole(siteId, sakaiRoleId));
                }
            }
        }

        for (String tutorUserId : tutorUserIds) {
            System.out.println("TUTOR USER ID: " + tutorUserId);
        }

		tutorUserIds.remove("admin");
        
        List<User> tutors = new ArrayList<User>();

        for(String tutorUserId : tutorUserIds) {
            try {
                tutors.add(sakaiProxy.getUser(tutorUserId));
            } catch (UserNotDefinedException unde) {
            }
        }

	    return tutors;
	}
	
	protected String getTag(String title, boolean shouldUseHtml) {
		return rb.getFormattedMessage("noti.tag", new Object[]{ServerConfigurationService.getString("ui.service", "Sakai"), ServerConfigurationService.getPortalUrl(), title});
    }
	
	protected List getHeaders(Event event) {

        List rv = super.getHeaders(event);
        rv.add("Subject: " + getSubject(event));
        rv.add(getFromAddress(event));
        rv.add(getTo(event));
        return rv;
    }
}
