package org.sakaiproject.learninglog.api;

import org.sakaiproject.entity.api.Entity;

public class Constants {

    public static final String ENTITY_PREFIX = "learninglog";
    public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;

    // For user notifications
	public static final String BLOG_POST_CREATED = "learninglog.post.created";
	public static final String BLOG_COMMENT_CREATED = "learninglog.comment.created";

    // For sitestats
	public static final String BLOG_POST_CREATED_SS = "learninglog.post.created.ss";
	public static final String BLOG_POST_DELETED_SS = "learninglog.post.deleted.ss";
	public static final String BLOG_POST_RECYCLED_SS = "learninglog.post.recycled.ss";
	public static final String BLOG_COMMENT_CREATED_SS = "learninglog.comment.created.ss";
	public static final String BLOG_COMMENT_DELETED_SS = "learninglog.comment.deleted.ss";
	public static final String BLOG_POST_RESTORED_SS = "learninglog.post.restored.ss";
}
