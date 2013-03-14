package org.sakaiproject.learninglog.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.sakaiproject.learninglog.api.Comment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;

public interface ISQLGenerator
{
	public static final String DEFAULT_PREFIX = "LL_";

	public static final String TABLE_POST = DEFAULT_PREFIX + "POST";
	public static final String TABLE_COMMENT = DEFAULT_PREFIX + "COMMENT";
	public static final String TABLE_AUTHOR = DEFAULT_PREFIX + "AUTHOR";
	public static final String TABLE_ROLE = DEFAULT_PREFIX + "ROLE";
	public static final String TABLE_ATTACHMENTS = DEFAULT_PREFIX + "ATTACHMENTS";

	public static final String  POST_ID = "POST_ID";
	public static final String  NAME = "NAME";
	
	// From BLOGGER_POST
	public static final String TITLE = "TITLE";

	public static final String CREATED_DATE = "CREATED_DATE";
	public static final String MODIFIED_DATE = "MODIFIED_DATE";

	// From BLOGGER_POST
	public static final String VISIBILITY = "VISIBILITY";
	
	// From BLOG_AUTHOR
	public static final String TOTAL_POSTS = "TOTAL_POSTS";
	public static final String TOTAL_COMMENTS = "TOTAL_COMMENTS";
	public static final String LAST_POST_DATE = "LAST_POST_DATE";
	public static final String LAST_COMMENT_DATE = "LAST_COMMENT_DATE";
	public static final String LAST_COMMENT_AUTHOR = "LAST_COMMENT_AUTHOR";

	public static final String USER_ID = "USER_ID";
	public static final String CREATOR_ID = "CREATOR_ID";
	
	public static final String EMAIL_FREQUENCY = "EMAIL_FREQUENCY";

	// From BLOGGER_POST
	public static final String SITE_ID = "SITE_ID";

	// From BLOGGER_COMMENT
	public static final String COMMENT_ID = "COMMENT_ID";

	// From BLOGGER_COMMENT
	public static final String CONTENT = "CONTENT";
	
	public static final String SAKAI_ROLE = "SAKAI_ROLE";
	public static final String LL_ROLE = "LL_ROLE";

	public abstract List<String> getCreateTablesStatements();

	public abstract List<String> getSelectStatementsForQuery(QueryBean query);

	public abstract String getSelectAllPost(String siteId);

	public abstract String getSelectPost(String OID);
	
	public String getSelectComments(String postId);

	public abstract List<PreparedStatement> getInsertStatementsForPost(Post post, Connection connection) throws Exception;
	
	public abstract List<PreparedStatement> getInsertStatementsForAttachments(Post post, Connection connection) throws Exception;
	
	public abstract List<PreparedStatement> getSaveStatementsForComment(Comment comment,Connection connection) throws Exception;
	
	public abstract List<PreparedStatement> getDeleteStatementsForPost(Post post,Connection connection) throws Exception;

	public abstract List<PreparedStatement> getDeleteStatementsForComment(String commentId, Connection connection) throws Exception;
	
	public abstract PreparedStatement getDeleteStatementForAttachment(String attachmentId, String postId, Connection connection) throws Exception;

	public abstract List<PreparedStatement> getRecycleStatementsForPost(Post post,Connection connection) throws Exception;
	
	public abstract List<PreparedStatement> getRestoreStatementsForPost(Post post,Connection connection) throws Exception;

	public abstract String getSelectAuthorStatement(String userId,String siteId);
	
	public abstract PreparedStatement getSelectRolesStatement(String siteId,Connection conn) throws Exception;

	public abstract List<PreparedStatement> getSaveRolesStatements(String siteId, Map<String,String> roles,Connection conn);
	
	public abstract PreparedStatement getSelectLLRoleStatement(String siteId,String sakaiRole,Connection conn) throws Exception;

	public abstract String getMessageAttachmentsSelectStatement(String postId);

}