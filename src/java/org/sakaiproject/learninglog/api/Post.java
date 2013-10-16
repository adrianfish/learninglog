package org.sakaiproject.learninglog.api;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Post {
		
	@Getter @Setter
	private String id = "";

	@Getter @Setter
	private String title = "";
	
	@Getter @Setter
	private String content = "";

	@Getter @Setter
	private long createdDate = -1L;
	
	@Getter @Setter
	private long modifiedDate = -1L;

	@Getter @Setter
	private String visibility = Visibilities.PRIVATE;

	@Getter @Setter
	private String creatorId = null;
	
	@Getter @Setter
	private String creatorDisplayName = null;

	@Getter
	private List<Comment> comments = new ArrayList<Comment>();
	
	@Getter @Setter
	private List<Attachment> attachments = new ArrayList<Attachment>();

	@Getter @Setter
	private String siteId;
	
	public boolean isAutosave = false;

	@Getter @Setter
    private String url = "";

	public Post() {
		long now = new Date().getTime();
		createdDate = now;
		modifiedDate = now;
	}

	public final void addComment(Comment comment) {
		comments.add(comment);
	}

	public final boolean isPrivate() {
		return Visibilities.PRIVATE.equals(visibility);
	}

	public boolean hasComments() {
		return comments.size() > 0;
	}

	public void removeComment(Comment comment) {
		comments.remove(comment);
	}

	public boolean isRecycled() {
		return Visibilities.RECYCLED.equals(visibility);
	}

	public boolean isReady() {
		return Visibilities.READY.equals(visibility);
	}
}
