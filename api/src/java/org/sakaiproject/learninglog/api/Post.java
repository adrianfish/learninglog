package org.sakaiproject.learninglog.api;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.util.BaseResourceProperties;

import lombok.Getter;
import lombok.Setter;

public class Post implements Entity {
		
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

	@Getter @Setter
	private List<Comment> comments = new ArrayList<Comment>();

	@Getter @Setter
	private List<Attachment> attachments = new ArrayList<Attachment>();

	@Getter @Setter
	private String siteId;
	
	public boolean isAutosave = false;

	@Setter
    private String url = "";

	public Post() {
		long now = new Date().getTime();
		createdDate = now;
		modifiedDate = now;
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

    /** START ENTITY IMPL */

    public String getReference() {
        return Constants.REFERENCE_ROOT + Entity.SEPARATOR + siteId  + Entity.SEPARATOR + "post" + Entity.SEPARATOR + id;
    }

    public String getReference(String rootProperty) {
        return getReference();
    }

    public String getUrl() {
        return url;
    }

    public String getUrl(String rootProperty) {
        return url;
    }

    public ResourceProperties getProperties() {

        ResourceProperties rp = new BaseResourceProperties();
        rp.addProperty("id", getId());
        return rp;
    }

    public Element toXml(Document doc,Stack stack) {
        return null;
    }
}
