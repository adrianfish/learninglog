package org.sakaiproject.learninglog.api;

import lombok.Getter;
import lombok.Setter;

import org.sakaiproject.user.api.User;

@Getter @Setter
public class BlogMember {
	
	private int numberOfPosts = 0;
	
	private int numberOfComments = 0;

	private long dateOfLastPost = -1L;
	
	private long dateOfLastComment = -1L;
	
	private String lastCommentCreator = "";

	// We don't want this serialising by EB :)
	private transient User sakaiUser = null;

    private String role = Roles.STUDENT;
	
	public BlogMember() { }
	
	public BlogMember(User user) {
		this.sakaiUser = user;
	}

	public String getUserId() {
		return sakaiUser.getId();
	}

	public String getUserEid() {
		return sakaiUser.getEid();

	}

	public String getUserDisplayName() {
		return sakaiUser.getLastName() + ", " + sakaiUser.getFirstName();
	}

    public boolean isStudent() {
        return role.equals(Roles.STUDENT);
    }

    public boolean isTutor() {
        return role.equals(Roles.TUTOR);
    }
}
