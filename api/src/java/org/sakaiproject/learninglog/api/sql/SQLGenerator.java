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

package org.sakaiproject.learninglog.api.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.sakaiproject.learninglog.api.Attachment;
import org.sakaiproject.learninglog.api.Comment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;
import org.sakaiproject.learninglog.api.Visibilities;

public class SQLGenerator {

	private Logger logger = Logger.getLogger(SQLGenerator.class);

	public String CLOB = "MEDIUMTEXT";

	public String TIMESTAMP = "DATETIME";

	public String VARCHAR = "VARCHAR";

	public List<String> getCreateTablesStatements() {
		ArrayList result = new ArrayList();

		result.add(doTableForSitesUsedIn());
		result.add(doTableForPost());
		result.add(doTableForComment());
		result.add(doTableForAutoSavedComment());
		result.add(doTableForAuthor());
		result.add(doTableForRole());
		result.add(doTableForAttachments());
		result.add(doTableForSettings());
		return result;
	}

    public PreparedStatement getSelectUsedInSiteStatement(String siteId, Connection conn) throws Exception {

        PreparedStatement st = conn.prepareStatement("SELECT * FROM LL_SITES_USED_IN WHERE SITE_ID = ?");
        st.setString(1, siteId);
        return st;
    }

    public PreparedStatement getInsertUsedInSiteStatement(String siteId, Connection conn) throws Exception {

        PreparedStatement st = conn.prepareStatement("INSERT INTO LL_SITES_USED_IN (SITE_ID) VALUES(?)");
        st.setString(1, siteId);
        return st;
    }

	public List<String> getSelectStatementsForQuery(QueryBean query) {
		List<String> statements = new ArrayList<String>();

		String queryString = query.getQueryString();

		if (queryString != null && queryString.length() > 0) {
			String sql = "SELECT * FROM LL_POST WHERE TITLE LIKE '%" + queryString + "%'";

			if (query.queryBySiteId())
				sql += " AND SITE_ID = '" + query.getSiteId() + "'";

			sql += " ORDER BY CREATED_DATE DESC";

			statements.add(sql);

			sql = "SELECT DISTINCT LL_POST.* FROM LL_POST,LL_COMMENT WHERE LL_POST.POST_ID = LL_COMMENT.POST_ID";

			if (query.queryBySiteId())
				sql += " AND LL_POST.SITE_ID = '" + query.getSiteId() + "'";

			sql += " AND LL_COMMENT.CONTENT like '%" + queryString + "%'" + " ORDER BY CREATED_DATE DESC";

			statements.add(sql);

			return statements;
		}

		StringBuilder statement = new StringBuilder();
		statement.append("SELECT * FROM LL_POST");

		if (query.hasConditions())
			statement.append(" WHERE ");

		// we know that there are conditions. Build the statement
		if (query.queryBySiteId())
			statement.append("SITE_ID = '").append(query.getSiteId()).append("' AND ");

		if (query.queryByCreator())
			statement.append("CREATOR_ID = '").append(query.getCreator()).append("' AND");

		if (query.queryByVisibility()) {
			statement.append("(");

			List<String> visibilities = query.getVisibilities();
			int length = visibilities.size();
			for (int i = 0; i < length; i++) {
				statement.append("VISIBILITY = '").append(visibilities.get(i)).append("'");

				if (i < (length - 1)) {
					statement.append(" OR ");
				}
			}

			statement.append(") AND ");
		}

		if (query.queryByInitDate()) {
			statement.append("CREATED_DATE >= '").append(query.getInitDate()).append("' AND ");
		}

		if (query.queryByEndDate()) {
			statement.append("CREATED_DATE <= '").append(query.getEndDate()).append("' AND ");
		}

		// in this point, we know that there is a AND at the end of the
		// statement. Remove it.
		statement = new StringBuilder(statement.toString().substring(0, statement.length() - 4));
		statement.append(" ORDER BY CREATED_DATE DESC");

		statements.add(statement.toString());
		return statements;
	}

	private final String doTableForPost() {
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_POST");
		statement.append("(");
		statement.append("POST_ID CHAR(36) NOT NULL,");
		statement.append("SITE_ID " + VARCHAR + "(255), ");
		statement.append("CREATOR_ID " + VARCHAR + "(255) NOT NULL, ");
		statement.append("TITLE " + VARCHAR + "(255) NOT NULL, ");
		statement.append("CONTENT " + CLOB + " NOT NULL, ");
		statement.append("VISIBILITY " + VARCHAR + "(16) NOT NULL, ");
		statement.append("CREATED_DATE " + TIMESTAMP + " NOT NULL" + ", ");
		statement.append("MODIFIED_DATE " + TIMESTAMP + ", ");
		statement.append("CONSTRAINT ll_post_pk PRIMARY KEY (POST_ID)");
		statement.append(")");
		return statement.toString();
	}

	private final String doTableForSitesUsedIn() {

		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_SITES_USED_IN");
		statement.append("(");
		statement.append("SITE_ID " + VARCHAR + "(255) NOT NULL, ");
		statement.append("CONSTRAINT ll_sites_used_in_pk PRIMARY KEY (SITE_ID)");
		statement.append(")");
		return statement.toString();
	}

	private final String doTableForComment() {
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_COMMENT");
		statement.append("(");
		statement.append("COMMENT_ID CHAR(36) NOT NULL,");
		statement.append("POST_ID CHAR(36) NOT NULL,");
		statement.append("SITE_ID " + VARCHAR + "(255) NOT NULL, ");
		statement.append("CREATOR_ID CHAR(36) NOT NULL,");
		statement.append("CONTENT " + CLOB + " NOT NULL, ");
		statement.append("VISIBILITY " + VARCHAR + "(16) NOT NULL, ");
		statement.append("CREATED_DATE " + TIMESTAMP + " NOT NULL,");
		statement.append("MODIFIED_DATE " + TIMESTAMP + " NOT NULL,");
		statement.append("CONSTRAINT ll_comment_pk PRIMARY KEY (COMMENT_ID)");
		statement.append(")");
		return statement.toString();
	}

    private final String doTableForAutoSavedComment() {
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_AUTOSAVED_COMMENT");
		statement.append("(");
		statement.append("COMMENT_ID CHAR(36) NOT NULL,");
		statement.append("POST_ID CHAR(36) NOT NULL,");
		statement.append("SITE_ID " + VARCHAR + "(255) NOT NULL, ");
		statement.append("CREATOR_ID CHAR(36) NOT NULL,");
		statement.append("CONTENT " + CLOB + " NOT NULL, ");
		statement.append("VISIBILITY " + VARCHAR + "(16) NOT NULL, ");
		statement.append("CREATED_DATE " + TIMESTAMP + " NOT NULL,");
		statement.append("MODIFIED_DATE " + TIMESTAMP + " NOT NULL,");
		statement.append("CONSTRAINT ll_comment_pk PRIMARY KEY (COMMENT_ID)");
		statement.append(")");
		return statement.toString();
	}

	protected String doTableForAuthor() {
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_AUTHOR");
		statement.append("(");
		statement.append("USER_ID CHAR(36) NOT NULL,");
		statement.append("SITE_ID " + VARCHAR + "(255),");
		statement.append("TOTAL_POSTS INT NOT NULL,");
		statement.append("LAST_POST_DATE " + TIMESTAMP + ",");
		statement.append("TOTAL_COMMENTS INT NOT NULL,");
		statement.append("LAST_COMMENT_DATE " + TIMESTAMP + ",");
		statement.append("LAST_COMMENT_AUTHOR " + VARCHAR + "(255),");
		statement.append("CONSTRAINT ll_author_pk PRIMARY KEY (USER_ID,SITE_ID)");
		statement.append(")");
		return statement.toString();
	}

	protected String doTableForRole() {
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE LL_ROLE");
		statement.append("(");
		statement.append("SITE_ID " + VARCHAR + "(255) NOT NULL, ");
		statement.append("SAKAI_ROLE " + VARCHAR + "(99) NOT NULL,");
		statement.append("LL_ROLE " + VARCHAR + "(50) NOT NULL)");
		return statement.toString();
	}

	protected String doTableForAttachments() {
		return "CREATE TABLE LL_ATTACHMENTS (ID INT NOT NULL AUTO_INCREMENT,POST_ID CHAR(36) NOT NULL, NAME " + VARCHAR + "(255) NOT NULL, PRIMARY KEY(ID))";
	}

	protected String doTableForSettings() {
		return "CREATE TABLE LL_SETTINGS (SITE_ID " + VARCHAR + "(255) NOT NULL,GROUP_MODE CHAR(1) NOT NULL, EMAILS_MODE CHAR(1) NOT NULL,PRIMARY KEY(SITE_ID))";
	}

	public String getSelectComments(String postId) {
		return "SELECT * FROM LL_COMMENT WHERE POST_ID = '" + postId + "' ORDER BY CREATED_DATE ASC";
	}

	public String getSelectUnsavedAutosavedComments(String postId) {
		return "SELECT * FROM LL_AUTOSAVED_COMMENT WHERE POST_ID = '" + postId + "' AND COMMENT_ID NOT IN (SELECT COMMENT_ID FROM LL_COMMENT)";
	}

	public String getSelectAllPost(String siteId) {
		return "SELECT * FROM LL_POST WHERE SITE_ID = '" + siteId + "' ORDER BY CREATED_DATE DESC";
	}

	public String getSelectPost(String postId) {
		return "SELECT * FROM LL_POST WHERE POST_ID = '" + postId + "'";
	}

	public String getSelectComment(String commentId) {
		return "SELECT * FROM LL_COMMENT WHERE COMMENT_ID = '" + commentId + "'";
	}

	public List<PreparedStatement> getInsertStatementsForAutoSavedComment(Comment comment, Connection connection) throws Exception {

		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		if ("".equals(comment.getId())) {
			comment.setId(UUID.randomUUID().toString());
        } else {
            // We always replace autosaved posts with the latest, so delete the old one.
            statements.add(getDeleteAutosavedCommentStatement(comment.getId(), connection));
        }

		String sql = "INSERT INTO LL_AUTOSAVED_COMMENT (COMMENT_ID,POST_ID,SITE_ID,CREATOR_ID,VISIBILITY,CREATED_DATE,MODIFIED_DATE,CONTENT) VALUES (?,?,?,?,'" + Visibilities.AUTOSAVE + "',?,?,?)";

		PreparedStatement commentST = connection.prepareStatement(sql);
		commentST.setString(1, comment.getId());
		commentST.setString(2, comment.getPostId());
		commentST.setString(3, comment.getSiteId());
		commentST.setString(4, comment.getCreatorId());
		commentST.setTimestamp(5, new Timestamp(comment.getCreatedDate()));
		commentST.setTimestamp(6, new Timestamp(comment.getModifiedDate()));
		commentST.setString(7, comment.getContent());

		statements.add(commentST);

		return statements;
	}

	public PreparedStatement getDeleteAutosavedCommentStatement(String commentId, Connection connection) throws Exception {

		PreparedStatement st = connection.prepareStatement("DELETE FROM LL_AUTOSAVED_COMMENT WHERE COMMENT_ID = ?");
		st.setString(1, commentId);
		return st;
	}

	public List<PreparedStatement> getInsertStatementsForComment(Comment comment, Connection connection) throws Exception {
		
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement existingCommentST = null;
		Statement postST = null;

        boolean isNew = false;

        String oldVisibility = null;

		try {

			if ("".equals(comment.getId())) {
                isNew = true;
				comment.setId(UUID.randomUUID().toString());
            } else {

                // We always replace autosaved posts with the latest, so delete the old one.
                statements.add(getDeleteAutosavedCommentStatement(comment.getId(), connection));

				existingCommentST = connection.createStatement();
                ResultSet existingRS = existingCommentST.executeQuery("SELECT * FROM LL_COMMENT WHERE COMMENT_ID = '" + comment.getId() + "'");
                if(!existingRS.next()) {
                    // This situation will arise if an autosave has successfully happened, but
                    // and explicit save hasn't been carried out yet.
                    isNew = true;
                } else {
                    oldVisibility = existingRS.getString("VISIBILITY");
                }
                existingRS.close();
            }

			if (isNew) {

				String sql = "INSERT INTO LL_COMMENT (COMMENT_ID,POST_ID,SITE_ID,CREATOR_ID,VISIBILITY,CREATED_DATE,MODIFIED_DATE,CONTENT) VALUES(?,?,?,?,?,?,?,?)";

				PreparedStatement statement = connection.prepareStatement(sql);

				statement.setString(1, comment.getId());
				statement.setString(2, comment.getPostId());
				statement.setString(3, comment.getSiteId());
				statement.setString(4, comment.getCreatorId());
				statement.setString(5, comment.getVisibility());
				statement.setTimestamp(6, new Timestamp(comment.getCreatedDate()));
				statement.setTimestamp(7, new Timestamp(comment.getModifiedDate()));
				statement.setString(8, comment.getContent());

				statements.add(statement);

                if(comment.isReady()) {
                    postST = connection.createStatement();
                    ResultSet postRS = postST.executeQuery("SELECT * FROM LL_POST WHERE POST_ID = '" + comment.getPostId() + "'");
                    if (postRS.next()) {
                        String blogCreatorId = postRS.getString("CREATOR_ID");
                        String siteId = postRS.getString("SITE_ID");

                        PreparedStatement authorST = connection.prepareStatement("UPDATE LL_AUTHOR SET TOTAL_COMMENTS = TOTAL_COMMENTS + 1,LAST_COMMENT_DATE = ?,LAST_COMMENT_AUTHOR = ? WHERE USER_ID = ? AND SITE_ID = ?");
                        authorST.setTimestamp(1, new Timestamp(comment.getCreatedDate()));
                        authorST.setString(2, comment.getCreatorDisplayName());
                        authorST.setString(3, blogCreatorId);
                        authorST.setString(4, siteId);
                        statements.add(authorST);
                    } else {
			            logger.warn("No post for id '" + comment.getPostId() + "'. The author table will not be updated.");
                    }
				    postRS.close();
                }
			} else {

                String sql = "UPDATE LL_COMMENT SET CONTENT = ?, MODIFIED_DATE = ?, VISIBILITY = ? WHERE COMMENT_ID = ?";
                PreparedStatement statement = connection.prepareStatement(sql);

                statement.setString(1, comment.getContent());
                statement.setTimestamp(2, new Timestamp(comment.getModifiedDate()));
                statement.setString(3, comment.getVisibility());
                statement.setString(4, comment.getId());
                statements.add(statement);

                if(oldVisibility.equals(Visibilities.PRIVATE) && comment.getVisibility().equals(Visibilities.READY)) {
                    postST = connection.createStatement();
                    ResultSet postRS = postST.executeQuery("SELECT * FROM LL_POST WHERE POST_ID = '" + comment.getPostId() + "'");
                    if (postRS.next()) {
                        String blogCreatorId = postRS.getString("CREATOR_ID");
                        String siteId = postRS.getString("SITE_ID");

                        PreparedStatement authorST = connection.prepareStatement("UPDATE LL_AUTHOR SET TOTAL_COMMENTS = TOTAL_COMMENTS + 1,LAST_COMMENT_DATE = ?,LAST_COMMENT_AUTHOR = ? WHERE USER_ID = ? AND SITE_ID = ?");
                        authorST.setTimestamp(1, new Timestamp(comment.getCreatedDate()));
                        authorST.setString(2, comment.getCreatorDisplayName());
                        authorST.setString(3, blogCreatorId);
                        authorST.setString(4, siteId);
                        statements.add(authorST);
                    } else {
			            logger.warn("No post for id '" + comment.getPostId() + "'. The author table will not be updated.");
                    }
				    postRS.close();
                }
			}
		} finally {

			if (postST != null) {
				try {
					postST.close();
				} catch (Exception e) {}
			}

			if (existingCommentST != null) {
				try {
					existingCommentST.close();
				} catch (Exception e) {}
			}
		}

		return statements;
	}

	public List<PreparedStatement> getDeleteStatementsForPost(Post post, Connection connection) throws Exception {
		List<PreparedStatement> result = new ArrayList<PreparedStatement>();

		PreparedStatement commentST = connection.prepareStatement("DELETE FROM LL_COMMENT WHERE POST_ID = ?");
		commentST.setString(1, post.getId());
		result.add(commentST);

		PreparedStatement attachmentsST = connection.prepareStatement("DELETE FROM LL_ATTACHMENTS WHERE POST_ID = ?");
		attachmentsST.setString(1, post.getId());
		result.add(attachmentsST);

		PreparedStatement postST = connection.prepareStatement("DELETE FROM LL_POST WHERE POST_ID = ?");
		postST.setString(1, post.getId());
		result.add(postST);

		return result;
	}

	public List<PreparedStatement> getRecycleStatementsForPost(Post post, Connection connection) throws Exception {
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		PreparedStatement st = connection.prepareStatement("UPDATE LL_POST SET VISIBILITY = '" + Visibilities.RECYCLED + "' WHERE POST_ID = ?");
		st.setString(1, post.getId());
		statements.add(st);
		statements.addAll(getAuthorTableStatements(post, false, connection));
		return statements;
	}

	public List<PreparedStatement> getRestoreStatementsForPost(Post post, Connection connection) throws Exception {
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		PreparedStatement st = connection.prepareStatement("UPDATE LL_POST SET VISIBILITY = '" + Visibilities.PRIVATE + "' WHERE POST_ID = ?");
		st.setString(1, post.getId());
		statements.add(st);
		return statements;

	}

	public List<PreparedStatement> getInsertStatementsForPost(Post post, Connection connection) throws Exception {
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement testST = null;

		try {
			if ("".equals(post.getId())) {
				// Empty post id == new post

				post.setId(UUID.randomUUID().toString());
				String sql = "INSERT INTO LL_POST(POST_ID,SITE_ID,TITLE,CONTENT,CREATED_DATE,MODIFIED_DATE,CREATOR_ID,VISIBILITY) VALUES (?,?,?,?,?,?,?,?)";

				PreparedStatement postST = connection.prepareStatement(sql);
				postST.setString(1, post.getId());

				postST.setString(2, post.getSiteId());

				postST.setString(3, post.getTitle());

				postST.setString(4, post.getContent());

				postST.setTimestamp(5, new Timestamp(post.getCreatedDate()));

				postST.setTimestamp(6, new Timestamp(post.getModifiedDate()));

				postST.setString(7, post.getCreatorId());

				postST.setString(8, post.getVisibility());

				statements.add(postST);

				if (post.isReady()) {
					statements.addAll(getAuthorTableStatements(post, true, connection));
				}
			} else {
				testST = connection.createStatement();
				ResultSet rs = testST.executeQuery("SELECT * FROM LL_POST WHERE POST_ID = '" + post.getId() + "'");

				if (!rs.next()) {
					rs.close();
					throw new Exception("Failed to get data for post '" + post.getId() + "'");
				}

				String currentVisibility = rs.getString("VISIBILITY");

				rs.close();

				String sql = "UPDATE LL_POST SET TITLE = ?,CONTENT = ?,VISIBILITY = ?,MODIFIED_DATE = ? WHERE POST_ID = ?";

				PreparedStatement postST = connection.prepareStatement(sql);
				postST.setString(1, post.getTitle());
				postST.setString(2, post.getContent());
				postST.setString(3, post.getVisibility());
				post.setModifiedDate(new Date().getTime());
				postST.setTimestamp(4, new Timestamp(post.getModifiedDate()));
				postST.setString(5, post.getId());

				statements.add(postST);

				if (post.isReady()) {
					if (Visibilities.PRIVATE.equals(currentVisibility) || Visibilities.RECYCLED.equals(currentVisibility)) {
						// This post has been made visible
						statements.addAll(getAuthorTableStatements(post, true, connection));
					}
				} else {
					if (Visibilities.READY.equals(currentVisibility)) {
						// This post has been hidden
						statements.addAll(getAuthorTableStatements(post, false, connection));
					}
				}
			}
		} finally {
			if (testST != null) {
				try {
					testST.close();
				} catch (Exception e) {
				}
			}
		}

		return statements;
	}

	public PreparedStatement getPublishStatementForPost(Post post, Connection connection) throws Exception {

		PreparedStatement statement = connection.prepareStatement("UPDATE LL_POST SET VISIBILITY = 'READY' WHERE POST_ID = ?");
        statement.setString(1,post.getId());
        return statement;
    }
	
    /**
     * This only returns insert statements for new attachments. Existing ones are not re-inserted.
     */
	public List<PreparedStatement> getInsertStatementsForAttachments(Post post, Connection connection) throws Exception {

		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
        PreparedStatement testST = connection.prepareStatement("SELECT * FROM LL_ATTACHMENTS WHERE POST_ID = ? AND NAME = ?");

        try {

            testST.setString(1, post.getId());
            for (Attachment attachment : post.getAttachments()) {
                testST.setString(2, attachment.name);
                if(!testST.executeQuery().next()) {

                    String insertSql = "INSERT INTO LL_ATTACHMENTS (POST_ID,NAME) VALUES(?,?)";
                    PreparedStatement attachmentPS = connection.prepareStatement(insertSql);
                    attachmentPS.setString(1, post.getId());
                    attachmentPS.setString(2, attachment.name);
                    statements.add(attachmentPS);
                }
		    }
        } finally {
            if(testST != null) {
                try {
                    testST.close();
                } catch(SQLException sqle) {}
            }
        }
		return statements;
	}

	private List<PreparedStatement> getAuthorTableStatements(Post post, boolean increment, Connection connection) throws Exception {

		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

		Statement testST = null;

		try {
			testST = connection.createStatement();
			ResultSet testRS = testST.executeQuery("SELECT COUNT(*) FROM LL_COMMENT WHERE POST_ID = '" + post.getId() + "'");
			testRS.next();
			int numComments = testRS.getInt(1);
			testRS.close();
			testST.close();

			String postAmount = " - 1";
			String commentAmount = " - ";
			if (increment) {
				postAmount = " + 1";
				commentAmount = " + ";
			}

			commentAmount += numComments;

			testST = connection.createStatement();
			testRS = testST.executeQuery("SELECT * FROM LL_AUTHOR WHERE USER_ID = '" + post.getCreatorId() + "' AND SITE_ID = '" + post.getSiteId() + "'");

			PreparedStatement totalST = null;
			if (testRS.next()) {
				totalST = connection.prepareStatement("UPDATE LL_AUTHOR SET TOTAL_POSTS = TOTAL_POSTS " + postAmount + ",TOTAL_COMMENTS = TOTAL_COMMENTS " + commentAmount + " WHERE USER_ID = ?");
				totalST.setString(1, post.getCreatorId());
				statements.add(totalST);

				PreparedStatement dateST = connection.prepareStatement("UPDATE LL_AUTHOR SET LAST_POST_DATE = (SELECT MAX(CREATED_DATE) FROM LL_POST WHERE (VISIBILITY = 'READY' OR VISIBILITY = 'PUBLIC')" + " AND USER_ID = ? AND SITE_ID = ?)" + " WHERE USER_ID = ?" + " AND SITE_ID  = ?");
				dateST.setString(1, post.getCreatorId());
				dateST.setString(2, post.getSiteId());
				dateST.setString(3, post.getCreatorId());
				dateST.setString(4, post.getSiteId());
				statements.add(dateST);
			} else {
				totalST = connection.prepareStatement("INSERT INTO LL_AUTHOR(USER_ID,SITE_ID,TOTAL_POSTS,LAST_POST_DATE,TOTAL_COMMENTS) VALUES(?,?,1,?,0)");
				totalST.setString(1, post.getCreatorId());
				totalST.setString(2, post.getSiteId());
				totalST.setTimestamp(3, new Timestamp(post.getCreatedDate()));
				statements.add(totalST);
			}

			testRS.close();
		} finally {
			if (testST != null) {
				try {
					testST.close();
				} catch (Exception e) {
				}
			}
		}

		return statements;
	}

	public List<PreparedStatement> getDeleteStatementsForComment(String commentId, Connection connection) throws Exception {

		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		Statement testST = null;

		try {
			testST = connection.createStatement();
			ResultSet rs = testST.executeQuery("SELECT tp.CREATOR_ID,tp.SITE_ID FROM LL_POST tp,LL_COMMENT tc WHERE tp.POST_ID = tc.POST_ID AND COMMENT_ID = '" + commentId + "'");
			if (rs.next()) {
				String blogCreatorId = rs.getString("CREATOR_ID");
				String siteId = rs.getString("SITE_ID");

				PreparedStatement authorST = connection.prepareStatement("UPDATE LL_AUTHOR SET TOTAL_COMMENTS = TOTAL_COMMENTS - 1 WHERE USER_ID = ? AND SITE_ID = ?");
				authorST.setString(1, blogCreatorId);
				authorST.setString(2, siteId);

				statements.add(authorST);
			}

			rs.close();

			PreparedStatement commentST = connection.prepareStatement("DELETE FROM LL_COMMENT WHERE COMMENT_ID = ?");
			commentST.setString(1, commentId);
			statements.add(commentST);

            statements.add(getDeleteAutosavedCommentStatement(commentId, connection));

			return statements;
		} finally {
			if (testST != null) {
				try {
					testST.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public String getSelectAuthorStatement(String userId, String siteId) {
		return "SELECT * FROM LL_AUTHOR WHERE USER_ID = '" + userId + "' AND SITE_ID = '" + siteId + "'";
	}

	public PreparedStatement getSelectRolesStatement(String siteId, Connection conn) throws Exception {

		PreparedStatement st = conn.prepareStatement("SELECT * FROM LL_ROLE WHERE SITE_ID = ?");
		st.setString(1, siteId);
		return st;
	}

	public List<PreparedStatement> getSaveRolesStatements(String siteId, Map<String, String> roles, Connection conn) {

		List<PreparedStatement> sqls = new ArrayList<PreparedStatement>();

		try {
			PreparedStatement st = conn.prepareStatement("DELETE FROM LL_ROLE WHERE SITE_ID = ?");
			st.setString(1, siteId);
			sqls.add(st);

			for (String sakaiRole : roles.keySet()) {
				String llRole = roles.get(sakaiRole);
				PreparedStatement st1 = conn.prepareStatement("INSERT INTO LL_ROLE (SITE_ID,SAKAI_ROLE,LL_ROLE) VALUES(?,?,?)");
				st1.setString(1, siteId);
				st1.setString(2, sakaiRole);
				st1.setString(3, llRole);
				sqls.add(st1);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst preparing statements.", e);
		}

		return sqls;
	}

	public PreparedStatement getSelectLLRoleStatement(String siteId, String sakaiRole, Connection conn) throws Exception {
		PreparedStatement st = conn.prepareStatement("SELECT LL_ROLE FROM LL_ROLE WHERE SITE_ID = ? AND SAKAI_ROLE = ?");
		st.setString(1, siteId);
		st.setString(2, sakaiRole);
		return st;
	}

	public String getMessageAttachmentsSelectStatement(String postId) {
		return "SELECT * FROM LL_ATTACHMENTS WHERE POST_ID = '" + postId + "'";
	}

	public PreparedStatement getDeleteStatementForAttachment(String name, String postId, Connection connection) throws Exception {
		PreparedStatement pst = connection.prepareStatement("DELETE FROM LL_ATTACHMENTS WHERE NAME = ?");
		pst.setString(1, name);
		return pst;
	}

	public PreparedStatement getSelectAutosavedComment(String commentId, Connection connection) throws Exception {

		PreparedStatement st = connection.prepareStatement("SELECT * FROM LL_AUTOSAVED_COMMENT WHERE COMMENT_ID = ?");
		st.setString(1, commentId);
		return st;
	}

    public PreparedStatement getInsertOrUpdateGroupMode(String siteId, String groupMode, Connection connection) throws Exception {

        PreparedStatement testST = null;

        try {
            testST = connection.prepareStatement("SELECT * FROM LL_SETTINGS WHERE SITE_ID = ?");
            testST.setString(1, siteId);
            ResultSet rs = testST.executeQuery();

            if (rs.next()) {
                PreparedStatement st = connection.prepareStatement("UPDATE LL_SETTINGS SET GROUP_MODE = ? WHERE SITE_ID = ?");
                st.setString(1, groupMode);
                st.setString(2, siteId);
                return st;
            } else {
                PreparedStatement st = connection.prepareStatement("INSERT INTO LL_SETTINGS VALUES(?,?,?)");
                st.setString(1, siteId);
                st.setString(2, groupMode);
                st.setString(3, "N");
                return st;
            }
        } finally {
            if (testST != null) {
                try {
                    testST.close();
                } catch (SQLException e) {}
            }
        }
    }

    public PreparedStatement getSelectGroupMode(String siteId, Connection connection) throws Exception {

        PreparedStatement st = connection.prepareStatement("SELECT GROUP_MODE FROM LL_SETTINGS WHERE SITE_ID = ?");
        st.setString(1, siteId);
        return st;
    }

    public PreparedStatement getInsertOrUpdateEmailsMode(String siteId, String emailsMode, Connection connection) throws Exception {

        PreparedStatement testST = null;

        try {
            testST = connection.prepareStatement("SELECT * FROM LL_SETTINGS WHERE SITE_ID = ?");
            testST.setString(1, siteId);
            ResultSet rs = testST.executeQuery();

            if (rs.next()) {
                PreparedStatement st = connection.prepareStatement("UPDATE LL_SETTINGS SET EMAILS_MODE = ? WHERE SITE_ID = ?");
                st.setString(1, emailsMode);
                st.setString(2, siteId);
                return st;
            } else {
                PreparedStatement st = connection.prepareStatement("INSERT INTO LL_SETTINGS (SITE_ID, GROUP_MODE, EMAILS_MODE) VALUES(?,?,?)");
                st.setString(1, siteId);
                st.setString(2, "N");
                st.setString(3, emailsMode);
                return st;
            }
        } finally {
            if (testST != null) {
                try {
                    testST.close();
                } catch (SQLException e) {}
            }
        }
    }

    public PreparedStatement getSelectEmailsMode(String siteId, Connection connection) throws Exception {

        PreparedStatement st = connection.prepareStatement("SELECT EMAILS_MODE FROM LL_SETTINGS WHERE SITE_ID = ?");
        st.setString(1, siteId);
        return st;
    }
}
