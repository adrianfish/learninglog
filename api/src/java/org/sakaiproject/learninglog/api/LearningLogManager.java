package org.sakaiproject.learninglog.api;

import java.util.List;
import java.util.Map;

import org.sakaiproject.entity.api.EntityProducer;

import org.sakaiproject.learninglog.api.*;

public interface LearningLogManager extends EntityProducer {

	public Post getPost(String postId) throws Exception;

	public List<Post> getPosts(String placementId) throws Exception;

	public List<Post> getPosts(QueryBean query) throws Exception;
    
	public boolean savePost(Post post, boolean isPublishing);

	public boolean deletePost(String postId);

	public Comment getComment(String commentId) throws Exception;

	public boolean saveComment(Comment comment);

	public boolean deleteComment(String commentId);

	public boolean recyclePost(String postId);

	public List<BlogMember> getAuthors(String siteId);

	public boolean restorePost(String postId);

	public void sendNewPostAlert(Post post);

	public void sendNewCommentAlert(Comment comment);

	public boolean saveRoles(String siteId, Map<String, String> map);

	public Map<String, String> getRoles(String siteId);

	public String getCurrentUserRole(String siteId);

	public boolean deleteAttachment(String siteId, String name, String postId);

    public boolean setGroupMode(String siteId, String groupMode);

    public boolean isGroupMode(String siteId);
}
