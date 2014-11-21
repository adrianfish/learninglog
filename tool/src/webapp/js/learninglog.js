learninglog.switchState = function (state, args) {

	$('#cluetip').hide();

	$('#blog_toolbar > li > span').removeClass('current');
	
	if ('home' === state) {
	    $('#blog_home_link > span').addClass('current');
		this.switchState(this.homeState, args);
	} else if ('viewMembers' === state) {
	    $('#blog_home_link > span').addClass('current');

		$.ajax({
	    	url : "/direct/learninglog-author.json?siteId=" + this.startupArgs.blogSiteId,
	      	dataType : "json",
			cache: false,
		   	success : function (data) {

				var authors = data['learninglog-author_collection'];
				for (var i=0,j=authors.length;i<j;i++) {
                    authors[i].formattedDateOfLastPost = learninglog.utils.formatDate(authors[i].dateOfLastPost);
                    authors[i].formattedDateOfLastComment = learninglog.utils.formatDate(authors[i].dateOfLastComment);
				}
				learninglog.utils.renderTrimpathTemplate('blog_authors_content_template', {'authors': authors}, 'blog_content');

 				$(document).ready(function () {

 					learninglog.utils.attachProfilePopup();

                    $.tablesorter.addParser({
                        id: 'learninglogDate',
                        is: function (s) {
                            return false;
                        },
                        format: function (s) {

                            if (s.indexOf(none) === 0) {
                                return 0;
                            }

                            var matches = s.match(/^([\d]{1,2}) (\w+) ([\d]{4}) \@ ([\d]{2}):([\d]{2}).*$/);
                            var d = new Date(matches[3], blog_month_mappings[matches[2]], matches[1], matches[4], matches[5]);
                            return d.getTime();
                        },
                        type: 'numeric'
                    });

  					$("#blog_author_table").tablesorter({
	 						cssHeader: 'blogSortableTableHeader',
	 						cssAsc: 'blogSortableTableHeaderSortUp',
	 						cssDesc: 'blogSortableTableHeaderSortDown',
							textExtraction: 'complex',	
							sortList: [[0,0]],
	 						widgets: ['zebra'],
	 						headers:
	 						{
	 							2: {sorter: 'learninglogDate'},
	 							4: {sorter: 'learninglogDate'}
	 						}
                        }).tablesorterPager({container: $("#blogBloggerPager"), positionFixed: false});
	 						
                    learninglog.resizeMainFrame();
	   			});
			},
			error : function (xmlHttpRequest, status, errorThrown) {
				alert("Failed to get authors. Reason: " + errorThrown);
			}
	   	});
	} else if ('userPosts' === state) {
	    $('#blog_home_link > span').addClass('current');

		// Default to using the current session user id ...
		var userId = this.currentUser.id;
		
		// ... but override it with any supplied one
		if(args && args.userId) {
			userId = args.userId;
        }

		var url = "/direct/learninglog-post.json?siteId=" + this.startupArgs.blogSiteId + "&creatorId=" + userId;

		$.ajax( {
	       	'url' : url,
	       	dataType : "text",
			cache: false,
		   	success : function (text) {
		   	
		   		var data = JSON.parse(text);

				var profileMarkup = learninglog.sakai.getProfileMarkup(userId);

				learninglog.currentPosts = data['learninglog-post_collection'];
                learninglog.utils.addFormattedDatesToCurrentPosts();
	 			
				learninglog.utils.renderTrimpathTemplate('blog_user_posts_template', {'creatorId': userId,'posts': learninglog.currentPosts}, 'blog_content');
				$('#blog_author_profile').html(profileMarkup);
	 			for (var i=0,j=learninglog.currentPosts.length;i<j;i++) {
                    learninglog.utils.addEscapedCreatorIdsToComments(learninglog.currentPosts[i]);
					learninglog.utils.renderTrimpathTemplate('blog_post_template', learninglog.currentPosts[i], 'post_' + learninglog.currentPosts[i].id);
                }

                $(document).ready(function () {

                    $('.ll_toggle_comments_link').click(function (e) {

                        var postId = this.id.substring(0, this.id.indexOf('_comments_toggle'));
                        $('#' + postId + '_comments').toggle();
                        learninglog.resizeMainFrame();
                    });

                    learninglog.resizeMainFrame();
                });
            },
			error : function (xmlHttpRequest, status, errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	} else if ('post' === state) {
		if (args && args.postId) {
			this.currentPost = learninglog.utils.findPost(args.postId);
        }

		if (!this.currentPost) {
			return false;
        }
			
		this.utils.addFormattedDatesToCurrentPost();
        this.utils.addEscapedCreatorIdsToComments(this.currentPost);

		this.utils.renderTrimpathTemplate('blog_post_page_content_template', this.currentPost, 'blog_content');
		this.utils.renderTrimpathTemplate('blog_post_template', this.currentPost, 'post_' + this.currentPost.id);

	 	$(document).ready(function () {

			$('#blog_user_posts_link').click(function (e) {
				learninglog.switchState('userPosts', {'userId': learninglog.currentPost.creatorId});
			});

			$('.content').show();

			if (learninglog.currentPost.comments.length > 0) {
                $('.comments').show();
            }

            learninglog.resizeMainFrame();
	 	});
	} else if ('createPost' === state) {
	    $('#blog_create_post_link > span').addClass('current');

		var post = {id: '', title: '', content: '', commentable: true, attachments: []};

		if (args && args.postId) {
			post = learninglog.utils.findPost(args.postId);
        }

		this.utils.renderTrimpathTemplate('blog_create_post_template', post, 'blog_content');

	 	$(document).ready(function () {

            if (post.attachments) {
                $('#attachments-area').show();
            }
	 	
			$('#blog_save_post_button').click(function (e) { learninglog.utils.savePostAsDraft(false); });

			$('#blog_publish_post_button').click(function (e) { learninglog.utils.publishPost(); });

			$('#blog_cancel_button').click(function(e) {
				learninglog.switchState('home');
			});
			
 			learninglog.sakai.setupWysiwygEditor('blog_content_editor', 600, 400);
 			
	    	$('#ll_attachment').MultiFile( {
		       		max: 5,
					namePattern: '$name_$i',
                    afterFileAppend: function (element, value, master_element) {
                        learninglog.attachmentsDirty = true;
                    }
				} );
				
			var savePostOptions = { 
				dataType: 'text',
				timeout: 30000,
				iframe: true,
   				success: function (text, statusText, xhr) {
   				
   					var post = JSON.parse(unescape(text));
   					
   					if (!post.isAutosave) {
						learninglog.switchState(learninglog.homeState);
                    } else {
   						// Get rid of the populated upload fields or they may result
   						// in multiple copies of the same attachment, especially in
   						// the autosave scenario
                    	var mfs = $('.MultiFile-applied');
                    	for (var i=0,j=mfs.length - 1;i<j;i++) {
                        	$(mfs[i]).remove();
                    	}
                        $('.MultiFile-label').remove();
						$('#blog_post_id_field').val(post.id);
                        if (post.attachments.length > 0) {
                            // Now add the current attachments
                            var attachments = $('#attachments-area').show();
                            for (var i=0,j=post.attachments.length;i<j;i++) {
                                var attachment = post.attachments[i];
                                attachments.append("<div id=\"file_" + i +"\"><span>" + attachment.name + "</span><a href=\"#\" onclick=\"learninglog.utils.removeAttachment('" + attachment.id + "','" + post.id + "','file_" + i +"');\" title=\"Delete attachment\"><img src=\"/library/image/silk/cross.png\" width=\"16\" height=\"16\"/></a><br /></div>");
                            }
                        }

						// Flash the autosaved message
						$('#learninglog_autosaved_message').show();
						setTimeout(function () {
								$('#learninglog_autosaved_message').fadeOut(200);
							},2000);

                        learninglog.sakai.resetEditor('blog_content_editor');
                        learninglog.attachmentsDirty = false;
					}
   				},
   				beforeSubmit: function (arr, $form, options) {
   					return true;
   				},
   				error : function (xmlHttpRequest, textStatus, errorThrown) {
   					alert('Error:' + errorThrown);
				}
   			};
				
   			$('#ll_post_form').ajaxForm(savePostOptions);

			// Start the auto saver
			learninglog.autosaveId = setInterval(function () {

					if (!(learninglog.sakai.isEditorDirty('blog_content_editor') || learninglog.attachmentsDirty) || $('#blog_title_field').val().length < 4) {
						return;
					}
					
					learninglog.utils.savePostAsDraft(true);
				},10000);
	 	});
	} else if ('createComment' === state) {
		if (!args || !args.postId) {
			return;
        }

		this.currentPost = learninglog.utils.findPost(args.postId);

		learninglog.utils.addFormattedDatesToCurrentPost();

		var comment = {id: '', postId: args.postId, content: ''};

		if (args.commentId) {
			var comments = learninglog.currentPost.comments;

			for (var i=0,j=comments.length;i<j;i++) {
				if (comments[i].id === args.commentId) {
					comment = comments[i];
					break;
				}
			}

			if (comment.autosavedVersion) {
				if (confirm('There is an autosaved version of this comment. Do you want to use that instead?')) {
					comment = comment.autosavedVersion;
				}
			}
		}

		this.utils.renderTrimpathTemplate('blog_create_comment_template', comment, 'blog_content');

		$(document).ready(function () {

			learninglog.utils.renderTrimpathTemplate('blog_post_template', learninglog.currentPost, 'blog_post_' + args.postId);

			$('#blog_save_comment_button').click(function (e) { learninglog.utils.saveCommentAsDraft(); });

			$('#blog_publish_comment_button').click(function (e) { learninglog.utils.publishComment(); });

			$('#blog_cancel_button').click(function (e) {
                learninglog.switchState('userPosts', {'userId': learninglog.currentPost.creatorId});
            });
			
 			learninglog.sakai.setupWysiwygEditor('blog_content_editor', 600, 400);

			// Start the auto saver
			learninglog.autosaveId = setInterval(function() {
					if(!learninglog.sakai.isEditorDirty('blog_content_editor')) {
						return;
					}
					
					learninglog.utils.autosaveComment();
				},10000);
		});
	} else if ('permissions' === state) {
	    $('#blog_permissions_link > span').addClass('current');

		var roleMapList = learninglog.utils.parsePermissions();

		this.utils.renderTrimpathTemplate('blog_permissions_content_template', {'roleMapList': roleMapList}, 'blog_content');

	 	$(document).ready(function () {

			$('#blog_permissions_save_button').click(learninglog.utils.savePermissions);
			$('#blog_permissions_cancel_button').click(function (e) {
				return learninglog.switchState('home');
			});

            learninglog.resizeMainFrame();
		});
	} else if ('viewRecycled' === state) {
	    $('#blog_recycle_bin_link > span').addClass('current');

		$.ajax( {
	       	url : "/direct/learninglog-post.json?siteId=" + this.startupArgs.blogSiteId + "&visibilities=RECYCLED",
	       	dataType : "json",
			cache: false,
		   	success : function (data) {

				var posts = data['learninglog-post_collection'];
	 			
				learninglog.utils.renderTrimpathTemplate('blog_recycled_posts_template', {'posts': posts}, 'blog_content');
	 			for (var i=0,j=posts.length;i<j;i++) {
                    learninglog.utils.addFormattedDatesToPost(posts[i]);
                    learninglog.utils.addEscapedCreatorIdsToComments(posts[i]);
					learninglog.utils.renderTrimpathTemplate('blog_post_template', posts[i], 'post_' + posts[i].id);
                }

				$('#blog_really_delete_button').click(learninglog.utils.deleteSelectedPosts);
				$('#blog_restore_button').click(learninglog.utils.restoreSelectedPosts);

                $(document).ready(function () {
                    learninglog.resizeMainFrame();
                });
			},
			error : function (xmlHttpRequest, status, errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
};

learninglog.toggleFullContent = function (v) {

    $(document).ready(function () {
        learninglog.resizeMainFrame();
    });
	
	if (v.checked) {
		$('.content').hide();
		$('.comments').hide();
		$('.comment_toggle').hide();
	} else {
		$('.content').show();
		$('.comments').show();
		$('.comment_toggle').show();
	}
};

learninglog.toggleComments = function (v) {

    $(document).ready(function () {
        learninglog.resizeMainFrame();
    });
	
	if (v.checked) {
		$('.comments').show();
	} else {
		$('.comments').hide();
	}
};

learninglog.resizeMainFrame = function () {

    try {
        if (window.frameElement) {
            setMainFrameHeight(window.frameElement.id);
        }
    } catch (err) {
        // This is likely under an LTI provision scenario
    }
};

(function () {

	// We need the toolbar in a template so we can swap in the translations
	learninglog.utils.renderTrimpathTemplate('blog_toolbar_template', {}, 'blog_toolbar');

	$('#blog_home_link > span > a').click(function (e) {
		return learninglog.switchState('home');
	});

	$('#blog_create_post_link > span > a').click(function (e) {
		return learninglog.switchState('createPost');
	});

	$('#blog_permissions_link > span > a').click(function (e) {
		return learninglog.switchState('permissions');
	});

	$('#blog_recycle_bin_link > span > a').click(function (e) {
		return learninglog.switchState('viewRecycled');
	});

	$('#blog_group_mode_link > span > a').click(function (e) {

        if (learninglog.groupMode) {
            this.textContent = blog_turn_group_mode_on_label;
            this.title = blog_turn_group_mode_on_tooltip;
            learninglog.groupMode = false;
        } else {
            this.textContent = blog_turn_group_mode_off_label;
            this.title = blog_turn_group_mode_off_tooltip;
            learninglog.groupMode = true;
        }
	});

	if (!learninglog.startupArgs.isTutor) {
		$("#blog_create_post_link").show();
    } else {
		$("#blog_create_post_link").hide();
    }

	learninglog.currentUser = learninglog.sakai.getCurrentUser();
	
	if (!learninglog.currentUser) {
		alert("No current user. Have you logged in?");
		return;
	}
	
	if (learninglog.startupArgs.isTutor) {
		learninglog.homeState = 'viewMembers';
	} else {
		learninglog.homeState = 'userPosts';
	}

    var initialState = learninglog.homeState;

    if(learninglog.startupArgs.postId !== 'none') {
        initialState = 'post';
    }

	var currentUserPermissions = new LearningLogPermissions(learninglog.utils.getCurrentUserPermissions());
	
	if (currentUserPermissions.modifyPermissions) {
		$("#blog_permissions_link").show();
		$("#blog_recycle_bin_link").show();
		$("#blog_group_mode_link").show();
	} else {
		$("#blog_permissions_link").hide();
		$("#blog_recycle_bin_link").hide();
		$("#blog_group_mode_link").hide();
	}

	try {
		if (window.frameElement) {
			window.frameElement.style.minHeight = '600px';
		}
	} catch (err) {
		// This is likely under an LTI provision scenario
	}

	// Clear the autosave interval
	if (learninglog.autosaveId) {
		clearInterval(learninglog.autosaveId);
	}

	
	// Now switch into the requested state
	learninglog.switchState(initialState, learninglog.startupArgs);
})();
