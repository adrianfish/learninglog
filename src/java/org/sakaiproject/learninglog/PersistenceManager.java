package org.sakaiproject.learninglog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.learninglog.api.Attachment;
import org.sakaiproject.learninglog.api.BlogMember;
import org.sakaiproject.learninglog.api.Comment;
import org.sakaiproject.learninglog.api.Post;
import org.sakaiproject.learninglog.api.QueryBean;
import org.sakaiproject.learninglog.sql.SQLGenerator;

import lombok.Setter;

public class PersistenceManager {

	private final Logger logger = Logger.getLogger(PersistenceManager.class);

	private SQLGenerator sqlGenerator;

    @Setter
	private SakaiProxy sakaiProxy;

	public void init() {
		
		sqlGenerator = new SQLGenerator();

		if (sakaiProxy.isAutoDDL()) {
			if (!setupTables()) {
				logger.error("Failed to setup the tables");
			}
		}
	}

	public boolean setupTables() {

		Connection connection = null;
		Statement statement = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			statement = connection.createStatement();

			try {
				
				for (String sql : sqlGenerator.getCreateTablesStatements()) {
					statement.executeUpdate(sql);
				}

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage() + ". Rolling back ...");
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst setting up tables. Message: " + e.getMessage());
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean existPost(String postId) throws Exception {

		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			rs = statement.executeQuery(sql);
			boolean exists = rs.next();
			return exists;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public List<Post> getAllPost(String placementId) throws Exception {
		
		return getAllPost(placementId, false);
	}

	public List<Post> getAllPost(String placementId, boolean populate) throws Exception {

		List<Post> result = new ArrayList<Post>();

		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sqlGenerator.getSelectAllPost(placementId));
			result = transformResultSetInPostCollection(rs, connection);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}

		return result;
	}

	public boolean saveComment(Comment comment) {
		
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				comment.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));
				statements = sqlGenerator.getSaveStatementsForComment(comment, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst saving comment. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst saving comment.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean deleteComment(String commentId) {
		
		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {

				statements = sqlGenerator.getDeleteStatementsForComment(commentId, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting comment. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting comment.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean savePost(Post post) {

		Connection connection = null;
		List<PreparedStatement> statements = null;
		List<PreparedStatement> attachmentStatements = null;
		Statement lastIdSt = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {

				statements = sqlGenerator.getInsertStatementsForPost(post, connection);

				for (PreparedStatement st : statements) {
					st.executeUpdate();
				}
				
				lastIdSt = connection.createStatement();
				
				attachmentStatements = sqlGenerator.getInsertStatementsForAttachments(post, connection);
				
				Iterator<Attachment> attachmentIterator = post.getAttachments().iterator();
				
				for (PreparedStatement st : attachmentStatements) {
					Attachment attachment = attachmentIterator.next();
					if(st.executeUpdate() == 1) {
						ResultSet rs = lastIdSt.executeQuery("SELECT LAST_INSERT_ID()");
						if(!rs.next()) {
							// Badness
						}
						attachment.id = rs.getInt(1);
						sakaiProxy.addDraftAttachment(post.getSiteId(), post.getCreatorId(), attachment);
					}
				}

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst saving post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst saving post.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean deletePost(Post post) {

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getDeleteStatementsForPost(post, connection);

				for (PreparedStatement st : statements)
					st.executeUpdate();

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting post.", e);
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (Exception e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean recyclePost(Post post) {

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getRecycleStatementsForPost(post, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst recycling post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst recycling post.", e);
			return false;
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean restorePost(String postId) {

		Connection connection = null;
		List<PreparedStatement> statements = null;

		try {
			Post post = getPost(postId);
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statements = sqlGenerator.getRestoreStatementsForPost(post, connection);
				for (PreparedStatement st : statements)
					st.executeUpdate();
				connection.commit();
				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst recycling post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst recycling post.", e);
			return false;
		} finally {
			if (statements != null) {
				for (PreparedStatement st : statements) {
					try {
						st.close();
					} catch (SQLException e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public List<Post> getPosts(QueryBean query) throws Exception {
		
		List<Post> posts = new ArrayList<Post>();

		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			List<String> sqlStatements = sqlGenerator.getSelectStatementsForQuery(query);
			for (String sql : sqlStatements) {
				rs = st.executeQuery(sql);
				posts.addAll(transformResultSetInPostCollection(rs, connection));
			}
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}

		return posts;
	}

	public Post getPost(String postId) throws Exception {

		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			rs = st.executeQuery(sql);
			List<Post> posts = transformResultSetInPostCollection(rs, connection);
			rs.close();

			if (posts.size() == 0)
				throw new Exception("getPost: Unable to find post with id:" + postId);
			if (posts.size() > 1)
				throw new Exception("getPost: there is more than one post with id:" + postId);

			return posts.get(0);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	private List<Post> transformResultSetInPostCollection(ResultSet rs, Connection connection) throws Exception {
		
		List<Post> result = new ArrayList<Post>();

		if (rs == null)
			return result;

		Statement commentST = null;

		try {
            commentST = connection.createStatement();
            while (rs.next()) {
                Post post = new Post();
                String postId = rs.getString("POST_ID");
                post.setId(postId);
                String siteId = rs.getString("SITE_ID");
                post.setSiteId(siteId);
                String title = rs.getString("TITLE"); post.setTitle(title);

				String content = rs.getString("CONTENT");
				post.setContent(content);

				Date postCreatedDate = rs.getTimestamp("CREATED_DATE");
				post.setCreatedDate(postCreatedDate.getTime());

				Date postModifiedDate = rs.getTimestamp("MODIFIED_DATE");
				post.setModifiedDate(postModifiedDate.getTime());

				String postCreatorId = rs.getString("CREATOR_ID");
				post.setCreatorId(postCreatorId);

				String visibility = rs.getString("VISIBILITY");
				post.setVisibility(visibility);

                String toolId = sakaiProxy.getLearningLogToolId(siteId);
                post.setUrl(sakaiProxy.getServerUrl() + "/portal/directtool/" + toolId + "?state=post&postId=" + postId);

				String sql = sqlGenerator.getSelectComments(postId);
				ResultSet commentRS = commentST.executeQuery(sql);

				while (commentRS.next()) {
					String commentId = commentRS.getString("COMMENT_ID");
					String commentCreatorId = commentRS.getString("CREATOR_ID");
					Date commentCreatedDate = commentRS.getTimestamp("CREATED_DATE");
					Date commentModifiedDate = commentRS.getTimestamp("MODIFIED_DATE");
					String commentContent = commentRS.getString("CONTENT");

					Comment comment = new Comment();
					comment.setId(commentId);
					comment.setPostId(post.getId());
					comment.setCreatorId(commentCreatorId);
					comment.setCreatedDate(commentCreatedDate.getTime());
					comment.setContent(commentContent);
					comment.setModifiedDate(commentModifiedDate.getTime());
					comment.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));

					post.addComment(comment);
				}

				commentRS.close();

				post.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()));

				sql = sqlGenerator.getMessageAttachmentsSelectStatement(post.getId());

				ResultSet rs2 = null;
				Statement st = null;

				List<Attachment> attachments = new ArrayList<Attachment>();

				try {
					st = connection.createStatement();
					rs2 = st.executeQuery(sql);

					while (rs2.next()) {
						int id = rs2.getInt("ID");
						String name = rs2.getString("NAME");

						Attachment attachment = new Attachment();
						attachment.id = id;
						attachment.name = name;

						sakaiProxy.getAttachment(post.getSiteId(), attachment);

						attachments.add(attachment);
					}
				} finally {
					try {
						if (rs2 != null)
							rs2.close();
					} catch (Exception e) {
					}

					if (st != null) {
						try {
							st.close();
						} catch (Exception e) {}
					}
				}

				post.setAttachments(attachments);

				result.add(post);
			}
		} finally {
			if (commentST != null) {
				try {
					commentST.close();
				} catch (Exception e) {}
			}
		}

		return result;
	}

	public boolean postExists(String postId) throws Exception {
		
		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectPost(postId);
			rs = st.executeQuery(sql);
			boolean exists = rs.next();
			return exists;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean populateAuthorData(BlogMember author, String siteId) {
		
		Connection connection = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getSelectAuthorStatement(author.getUserId(), siteId);
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int totalPosts = rs.getInt("TOTAL_POSTS");
				int totalComments = rs.getInt("TOTAL_COMMENTS");

				Timestamp lastPostTimestamp = rs.getTimestamp("LAST_POST_DATE");
				if (null != lastPostTimestamp) {

					long lastPostDate = lastPostTimestamp.getTime();

					Timestamp lastCommentTimestamp = rs.getTimestamp("LAST_COMMENT_DATE");
					if (lastCommentTimestamp != null)
						author.setDateOfLastComment(lastCommentTimestamp.getTime());

					String lastCommentAuthor = rs.getString("LAST_COMMENT_AUTHOR");
					author.setNumberOfPosts(totalPosts);
					author.setNumberOfComments(totalComments);
					author.setDateOfLastPost(lastPostDate);
					author.setLastCommentCreator(sakaiProxy.getDisplayNameForTheUser(lastCommentAuthor));
				}
			}

			rs.close();

			return true;
		} catch (Exception e) {
			logger.error("Caught exception whilst populating author data. Returning false ...", e);
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {}
			}

			sakaiProxy.returnConnection(connection);
		}
	}

	public Map<String, String> getRoles(String siteId) {
		Map<String, String> tutorRoles = new HashMap<String, String>();

		Connection connection = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getSelectRolesStatement(siteId, connection);
			rs = st.executeQuery();

			while (rs.next()) {
				String sakaiRole = rs.getString("SAKAI_ROLE");
				String llRole = rs.getString("LL_ROLE");
				tutorRoles.put(sakaiRole, llRole);
			}

			return tutorRoles;
		} catch (Exception e) {
			logger.error("Caught exception whilst getting roles. Returning null ...", e);
			return null;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			try {
				if (st != null)
					st.close();
			} catch (Exception e) {}

			sakaiProxy.returnConnection(connection);
		}
	}

	public String getLLRole(String siteId, String sakaiRole) {
		
		Connection connection = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			connection = sakaiProxy.borrowConnection();
			st = sqlGenerator.getSelectLLRoleStatement(siteId, sakaiRole, connection);
			rs = st.executeQuery();
			if (rs.next())
				return rs.getString("LL_ROLE");
			else {
				logger.error("No LL role for site '" + siteId + "' and sakai Role '" + sakaiRole + "'. Returning null ...");
				return null;
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst getting LL role. Returning null ...", e);
			return null;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
			}

			try {
				if (st != null)
					st.close();
			} catch (Exception e) {}

			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean saveRoles(String siteId, Map<String, String> map) {
		
		Connection connection = null;

		List<PreparedStatement> sqls = null;

		try {
			connection = sakaiProxy.borrowConnection();

			boolean oldAutoCommit = connection.getAutoCommit();

			try {
				connection = sakaiProxy.borrowConnection();

				connection.setAutoCommit(false);

				sqls = sqlGenerator.getSaveRolesStatements(siteId, map, connection);

				for (PreparedStatement st : sqls)
					st.executeUpdate();

				connection.commit();
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting post. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst marking tutor roles", e);
		} finally {
			if (sqls != null) {
				for (PreparedStatement st : sqls) {
					try {
						st.close();
					} catch (Exception e) {}
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}

	public boolean deleteAttachment(String siteId, String attachmentId, String postId) {

		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try {
				statement = sqlGenerator.getDeleteStatementForAttachment(attachmentId, postId, connection);
				statement.executeUpdate();

				sakaiProxy.deleteAttachment(siteId, attachmentId);

				connection.commit();

				return true;
			} catch (Exception e) {
				logger.error("Caught exception whilst deleting attachment. Rolling back ...", e);
				connection.rollback();
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
		} catch (Exception e) {
			logger.error("Caught exception whilst deleting attachment.", e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}

			sakaiProxy.returnConnection(connection);
		}

		return false;
	}
}
