var blogSiteId = null;
var blogPlacementId = null;
var blogCurrentUserPermissions = null;
var blogCurrentUserRole = null;
var blogCurrentPost = null;
var blogCurrentPosts = null;
var blogCurrentUser = null;
var blogHomeState = null;
var blogTextAreaChanged = false;

var wysiwygEditor = 'ckeditor'; //default

var llIsTutor = false;

var blogBaseDataUrl = "";

var blogInPDA = false;

var autosave_id = null;

(function() {

	// We need the toolbar in a template so we can swap in the translations
	SakaiUtils.renderTrimpathTemplate('blog_toolbar_template',{},'blog_toolbar');

	$('#blog_home_link > span > a').click(function(e) {
		return switchState('home');
	});

	$('#blog_create_post_link > span > a').click(function(e) {
		return switchState('createPost');
	});

	$('#blog_permissions_link > span > a').click(function(e) {
		return switchState('permissions');
	});

	$('#blog_recycle_bin_link > span > a').click(function(e) {
		return switchState('viewRecycled');
	});

	$('#blog_search_field').change(LearningLogUtils.showSearchResults);
	
	var arg = SakaiUtils.getParameters();
	
	if(!arg || !arg.placementId || !arg.siteId) {
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}
	
	blogSiteId = arg.siteId;
	blogPlacementId = arg.placementId;

	blogCurrentUser = SakaiUtils.getCurrentUser();
	
	if(!blogCurrentUser) {
		alert("No current user. Have you logged in?");
		return;
	}
	
	var href = document.location.href;

    if(href.indexOf("portal/pda") != -1) {
        blogInPDA = true;
        wysiwygEditor = 'none';
    } else {
        blogInPDA = false;
    }
    
    if(arg['language']) {
        $.localise('learninglog-translations',{language:arg['language'],loadBase: true});
    }
    else {
        $.localise('learninglog-translations');
    }
	
	blogCurrentUserPermissions = new LearningLogPermissions(LearningLogUtils.getCurrentUserPermissions().data);

	if(LearningLogUtils.getCurrentUserRole() === "Tutor") llIsTutor = true;

	if(llIsTutor) blogHomeState = 'viewMembers';
	else blogHomeState = 'userPosts';
	
	//if(blogCurrentUserPermissions == null) return;
	
	if(blogCurrentUserPermissions.modifyPermissions) {
		$("#blog_permissions_link").show();
		$("#blog_recycle_bin_link").show();
	}
	else {
		$("#blog_permissions_link").hide();
		$("#blog_recycle_bin_link").hide();
	}

	try {
		if(window.frameElement) {
			window.frameElement.style.minHeight = '600px';
		}
	} catch(err) {
		// This is likely under an LTI provision scenario
	}

	// Clear the autosave interval
	if(autosave_id)
		clearInterval(autosave_id);
	
	// Now switch into the requested state
	switchState(arg.state,arg);
})();

function switchState(state,arg) {
	$('#cluetip').hide();

	if(!llIsTutor)
		$("#blog_create_post_link").show();
	else
		$("#blog_create_post_link").hide();
	
	if('home' === state) {
		switchState(blogHomeState,arg);
	}
	else if('viewMembers' === state) {

		jQuery.ajax({
	    	url : "/direct/learninglog-author.json?siteId=" + blogSiteId,
	      	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				SakaiUtils.renderTrimpathTemplate('blog_authors_content_template',{'authors':data['learninglog-author_collection']},'blog_content');

 				$(document).ready(function() {
 					LearningLogUtils.attachProfilePopup();
  									
  					$("#blog_author_table").tablesorter({
	 						cssHeader:'blogSortableTableHeader',
	 						cssAsc:'blogSortableTableHeaderSortUp',
	 						cssDesc:'blogSortableTableHeaderSortDown',
							textExtraction: 'complex',	
							sortList: [[0,0]],
	 						widgets: ['zebra'],
	 						headers:
	 						{
	 							2: {sorter: "isoDate"},
	 							3: {sorter: "isoDate"}
	 						} }).tablesorterPager({container: $("#blogBloggerPager"),positionFixed: false});
	 						
	 				try {
 						if(window.frameElement) {
	 						setMainFrameHeight(window.frameElement.id);
	 					}
					} catch(err) {
						// This is likely under an LTI provision scenario
					}
	   			});
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get authors. Reason: " + errorThrown);
			}
	   	});
	}
	else if('userPosts' === state) {
		// Default to using the current session user id ...
		var userId = blogCurrentUser.id;
		
		// ... but override it with any supplied one
		if(arg && arg.userId)
			userId = arg.userId;

		var url = "/direct/learninglog-post.json?siteId=" + blogSiteId + "&creatorId=" + userId;

		jQuery.ajax( {
	       	'url' : url,
	       	dataType : "text",
	       	async : false,
			cache: false,
		   	success : function(text) {
		   	
		   		var data = JSON.parse(text);

				var profileMarkup = SakaiUtils.getProfileMarkup(userId);

				blogCurrentPosts = data['learninglog-post_collection'];
                LearningLogUtils.addFormattedDatesToCurrentPosts();
	 			
				SakaiUtils.renderTrimpathTemplate('blog_user_posts_template',{'creatorId':userId,'posts':blogCurrentPosts},'blog_content');
				$('#blog_author_profile').html(profileMarkup);
	 			for(var i=0,j=blogCurrentPosts.length;i<j;i++)
					SakaiUtils.renderTrimpathTemplate('blog_post_template',blogCurrentPosts[i],'post_' + blogCurrentPosts[i].id);

				try {
	 				if(window.frameElement) {
	 					$(document).ready(function() {
	 						setMainFrameHeight(window.frameElement.id);
	 					});
					}
				} catch(err) {
					// This is likely under an LTI provision scenario
				}
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
	else if('post' === state) {
		if(arg && arg.postId)
			blogCurrentPost = LearningLogUtils.findPost(arg.postId);

		if(!blogCurrentPost)
			return false;
			
		LearningLogUtils.addFormattedDatesToCurrentPost();
	 			
		SakaiUtils.renderTrimpathTemplate('blog_post_page_content_template',blogCurrentPost,'blog_content');
		SakaiUtils.renderTrimpathTemplate('blog_post_template',blogCurrentPost,'post_' + blogCurrentPost.id);

	 	$(document).ready(function() {
			$('#blog_user_posts_link').click(function(e) {
				switchState('userPosts',{'userId' : blogCurrentPost.creatorId});
			});

			$('.content').show();

			if(blogCurrentPost.comments.length > 0) $('.comments').show();

			try {
	 			if(window.frameElement) {
	 				setMainFrameHeight(window.frameElement.id);
	 			}
			} catch(err) {
				// This is likely under an LTI provision scenario
			}
	 	});
	}
	else if('createPost' === state) {
		var post = {id:'',title:'',content:'',commentable:true,attachments:[]};

		if(arg && arg.postId)
			post = LearningLogUtils.findPost(arg.postId);

		SakaiUtils.renderTrimpathTemplate('blog_create_post_template',post,'blog_content');

	 	$(document).ready(function() {

            if(post.attachments) {
                $('#attachments-area').show();
            }
	 	
			$('#blog_save_post_button').click(function (e) { LearningLogUtils.savePostAsDraft(false); });

			$('#blog_publish_post_button').click(LearningLogUtils.publishPost);

			$('#blog_cancel_button').click(function(e) {
				switchState('home');
			});
			
            if(blogInPDA) {
	 		    $('#blog_content_editor').keypress(function (e) {
				    blogTextAreaChanged = true;	 		
	 		    });
            }

 			SakaiUtils.setupWysiwygEditor('blog_content_editor',600,400,'Default',blogSiteId);
 			
	    	$('#ll_attachment').MultiFile( {
		       		max: 5,
					namePattern: '$name_$i'
				} );
				
			var savePostOptions = { 
				dataType: 'text',
				timeout: 30000,
				iframe: true,
   				success: function(text,statusText,xhr) {
   				
   					var post = JSON.parse(unescape(text));
   					
   					if(!post.isAutosave) {
						switchState(blogHomeState);
                    } else {
   						// Get rid of the populated upload fields or they may result
   						// in multiple copies of the same attachment, especially in
   						// the autosave scenario
                    	var mfs = $('.MultiFile-applied');
                    	for(var i=0,j=mfs.length - 1;i<j;i++) {
                        	$(mfs[i]).remove();
                    	}
                        $('.MultiFile-label').remove();
						$('#blog_post_id_field').val(post.id);
                        if(post.attachments.length > 0) {
                            // Now add the current attachments
                            var attachments = $('#attachments-area').show();
                            //attachments.append("<span id=\"current_attachments_label\">Current attachments:</span><br /><br />");
                            for(var i=0,j=post.attachments.length;i<j;i++) {
                                var attachment = post.attachments[i];
                                attachments.append("<div id=\"file_" + i +"\"><span>" + attachment.name + "</span><a href=\"#\" onclick=\"LearningLogUtils.removeAttachment('" + attachment.id + "','" + post.id + "','file_" + i +"');\" title=\"Delete attachment\"><img src=\"/library/image/silk/cross.png\" width=\"16\" height=\"16\"/></a><br /></div>");
                            }
                        }
						// Flash the autosaved message
						$('#learninglog_autosaved_message').show();
						setTimeout(function() {
								$('#learninglog_autosaved_message').fadeOut(200);
							},2000);
					}
   				},
   				beforeSubmit: function(arr, $form, options) {
   					return true;
   				},
   				error : function(xmlHttpRequest,textStatus,errorThrown) {
   					alert('Error:' + errorThrown);
				}
   			};
				
   			$('#ll_post_form').ajaxForm(savePostOptions);

			// Start the auto saver
			autosave_id = setInterval(function() {
					if(!SakaiUtils.isEditorDirty('blog_content_editor') || $('#blog_title_field').val().length < 4) {
						return;
					}
					
					LearningLogUtils.savePostAsDraft(true);
				},10000);
	 	});
	}
	else if('createComment' === state) {
		if(!arg || !arg.postId)
			return;

		blogCurrentPost = LearningLogUtils.findPost(arg.postId);

		var comment = {id: '',postId: arg.postId,content: ''};

		var currentIndex = -1;

		if(arg.commentId) {
			var comments = blogCurrentPost.comments;

			for(var i=0,j=comments.length;i<j;i++) {
				if(comments[i].id == arg.commentId) {
					comment = comments[i];
					currentIndex = i;
					break;
				}
			}
		}

		SakaiUtils.renderTrimpathTemplate('blog_create_comment_template',comment,'blog_content');

		$(document).ready(function() {
			SakaiUtils.renderTrimpathTemplate('blog_post_template',blogCurrentPost,'blog_post_' + arg.postId);
			$('#blog_save_comment_button').click(LearningLogUtils.saveComment);
			
 			SakaiUtils.setupWysiwygEditor('blog_content_editor',600,400,'Default',blogSiteId);
		});
	}
	else if('permissions' === state) {
		var roleMapList = LearningLogUtils.parsePermissions();

		SakaiUtils.renderTrimpathTemplate('blog_permissions_content_template',{'roleMapList':roleMapList},'blog_content');

	 	$(document).ready(function() {
			$('#blog_permissions_save_button').click(LearningLogUtils.savePermissions);
			$('#blog_permissions_cancel_button').click(function(e) {
				return switchState('home');
			});

			try {
				if(window.frameElement) {
					setMainFrameHeight(window.frameElement.id);
				}
			} catch(err) {
				// This is likely under an LTI provision scenario
			}
		});
	}
	else if('viewRecycled' === state) {
		jQuery.ajax( {
	       	url : "/direct/learninglog-post.json?siteId=" + blogSiteId + "&visibilities=RECYCLED",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {

				var posts = data['learninglog-post_collection'];
	 			
				SakaiUtils.renderTrimpathTemplate('blog_recycled_posts_template',{'posts':posts},'blog_content');
	 			for(var i=0,j=posts.length;i<j;i++)
					SakaiUtils.renderTrimpathTemplate('blog_post_template',posts[i],'post_' + posts[i].id);

				$('#blog_really_delete_button').click(LearningLogUtils.deleteSelectedPosts);
				$('#blog_restore_button').click(LearningLogUtils.restoreSelectedPosts);

				try {
	 				if(window.frameElement) {
	 					$(document).ready(function() {
	 						setMainFrameHeight(window.frameElement.id);
	 					});
					}
				} catch(err) {
					// This is likely under an LTI provision scenario
				}
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}
}

function toggleFullContent(v)
{
	try {
 		if(window.frameElement) {
			$(document).ready(function() {
 				setMainFrameHeight(window.frameElement.id);
			});
		}
	} catch(err) {
		// This is likely under an LTI provision scenario
	}
	
	if(v.checked) {
		$('.content').hide();
		$('.comments').hide();
	}
	else {
		$('.content').show();
		$('.comments').show();
	}
}
