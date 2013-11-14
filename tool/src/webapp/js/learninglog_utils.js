var LearningLogUtils;

(function() {

	if(LearningLogUtils == null) {
		LearningLogUtils = new Object();
    }

	LearningLogUtils.removeAttachment = function(name,postId,elementId) {

        if(!confirm("Are you sure you want to delete this attachment?")) {
            return;
        }

        jQuery.ajax( {
	 		url : "/direct/learninglog-post/" + postId + "/deleteAttachment.json?siteId=" + startupArgs.blogSiteId + "&name=" + name,
            dataType : "text",
            async : false,
            cache: false,
            success : function(text,textStatus) {
                jQuery("#" + elementId).remove();
            },
            error : function(xhr,textStatus,errorThrown) {
                alert("Failed to delete attachment. Reason: " + errorThrown);
            }
        });
    }

	LearningLogUtils.getCurrentUserRole = function() {

		var role = null;
		jQuery.ajax( {
	 		url : "/direct/learninglog-role/" + startupArgs.blogSiteId + "/currentUserRole.json",
	   		dataType : "text",
	   		async : false,
	   		cache : false,
		   	success : function(r) {
				role = r;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user role. Status: " + stat + ". Error: " + error);
			}
	  	});

		return role;
	}
	
	LearningLogUtils.addFormattedDatesToCurrentPosts = function () {

        for(var i=0,j=blogCurrentPosts.length;i<j;i++) {
        	LearningLogUtils.addFormattedDatesToPost(blogCurrentPosts[i]);
        }
    }
    
    LearningLogUtils.addFormattedDatesToPost = function(post) {

        post.formattedCreatedDate = LearningLogUtils.formatDate(post.createdDate);
        post.formattedModifiedDate = LearningLogUtils.formatDate(post.modifiedDate);

        for(var i=0,j=post.comments.length;i<j;i++) {
            post.comments[i].formattedCreatedDate = LearningLogUtils.formatDate(post.comments[i].createdDate);
            post.comments[i].formattedModifiedDate = LearningLogUtils.formatDate(post.comments[i].modifiedDate);
        }
    }
    
    LearningLogUtils.addFormattedDatesToCurrentPost = function () {
    	LearningLogUtils.addFormattedDatesToPost(blogCurrentPost);
	}

    LearningLogUtils.formatDate = function(longDate) {

        var d = new Date(longDate);
        var hours = d.getHours();
        if(hours < 10) hours = '0' + hours;
        var minutes = d.getMinutes();
        if(minutes < 10) minutes = '0' + minutes;
        return d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
    }

    LearningLogUtils.addEscapedCreatorIdsToComments = function(post) {

        var comments = post.comments;
        for(var i=0,j=comments.length;i<j;i++) {
            comments[i].escapedCreatorId = escape(comments[i].creatorId);
        }
    }
	
	LearningLogUtils.attachProfilePopup = function() {
	
		$('a.showPostsLink').cluetip({
			width: '620px',
			cluetipClass: 'blog',
			sticky: true,
 			dropShadow: false,
			arrows: true,
			mouseOutClose: true,
			closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
			closePosition: 'top',
			showTitle: false,
			hoverIntent: true,
			ajaxSettings: {type: 'GET'}
		});
	}

	LearningLogUtils.parsePermissions = function() {

		var roleMapList = [];
		
		jQuery.ajax({
	    	url : "/direct/learninglog-role/" + startupArgs.blogSiteId + ".json",
	      	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(roles) {
				for(var r in roles.data) {
					roleMapList.push({'sakaiRole':r,'llRole':roles.data[r]});
				}
		   	},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});

		return roleMapList;
	}

	LearningLogUtils.savePermissions = function() {

		var boxes = $('.blog_role_radiobutton:checked');
		var myData = {'siteId':startupArgs.blogSiteId};

		for(var i=0,j=boxes.length;i<j;i++)
			myData[boxes[i].name] = boxes[i].value;

		jQuery.ajax( {
	 		url : "/direct/learninglog-role/new",
			type : 'POST',
			data : myData,
			timeout: 30000,
			dataType: 'text',
		   	success : function(result) {
                location.reload();
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to create meeting. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	LearningLogUtils.savePostAsDraft = function(isAutosave) {
		return LearningLogUtils.storePost('PRIVATE',false,isAutosave);
	}

	LearningLogUtils.publishPost = function() {
	
		if(!confirm(blog_publish_post_message)) {
			return false;
		}

		LearningLogUtils.storePost('READY',true);
	}
		
	LearningLogUtils.storePost = function(visibility,isPublish,isAutosave) {

		var title = $('#blog_title_field').val();
        if(title.length < 4) {
        	alert("You must add a title of at least 4 characters.");
            return false;
        }
        
		var content = SakaiUtils.getEditorData('blog_content_editor');
		if(content === '') {
			alert("You need to add some content!");
			return false;
		}
		
		$('#blog_visibility_field').val(visibility);
		if(isPublish) {
			$('#blog_mode_field').val('publish');
		}
		
		if(isAutosave && isAutosave == true) {
			$('#blog_autosave_field').val('yes');
		} else {
			$('#blog_autosave_field').val('no');
        }
		
		$('#ll_post_form').submit();
	}

	LearningLogUtils.autosaveComment = function() {

		if(!SakaiUtils.isEditorDirty('blog_content_editor')) {
			return 0;
		}
	
		return LearningLogUtils.storeComment('AUTOSAVE');
	}

	LearningLogUtils.saveCommentAsDraft = function() {
		return LearningLogUtils.storeComment('PRIVATE');
	}

	LearningLogUtils.publishComment = function() {
	
		LearningLogUtils.storeComment('READY');
	}
	
	LearningLogUtils.storeComment = function(visibility) {

		var comment = {
				'id':$('#blog_comment_id_field').val(),
				'postId':blogCurrentPost.id,
				'content':SakaiUtils.getEditorData('blog_content_editor'),
				'siteId':startupArgs.blogSiteId,
				'visibility':visibility
				};

		jQuery.ajax( {
	 		url : "/direct/learninglog-comment/new",
			type : 'POST',
			data : comment,
			timeout: 30000,
			dataType: 'text',
		   	success : function(id) {

                $('#blog_comment_id_field').val(id);
                
                if(visibility === 'AUTOSAVE') {

                    SakaiUtils.resetEditor('blog_content_editor');

                    // Flash the autosaved message
                    $('#learninglog_autosaved_message').show();
                    setTimeout(function() {
                            $('#learninglog_autosaved_message').fadeOut(200);
                        },2000);
                } else {
				    switchState('userPosts',{'userId':blogCurrentPost.creatorId});
                }
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to save comment. Status: " + status + '. Error: ' + error);
			}
	  	});
	}

	LearningLogUtils.recyclePost = function(postId) {

		if(!confirm(blog_delete_post_message)) {
			return false;
        }

		jQuery.ajax( {
	 		url : "/direct/learninglog-post/" + postId + "/recycle",
			dataType : 'text',
			cache : false,
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to recycle post. Status: " + status + '. Error: ' + error);
			}
	  	});
	}

	LearningLogUtils.deleteSelectedPosts = function() {
		
		var selected = $('.blog_recycled_post_checkbox:checked');

        if(selected.length <= 0) {
            // No posts selected for deletion
            return;
        }

		if(!confirm(blog_really_delete_post_message)) {
			return false;
		}
        
		var postIds = '';

		for(var i=0,j=selected.length;i<j;i++) {
			postIds += selected[i].id;
			if(i < (j - 1)) postIds += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/learninglog-post/remove?posts=" + postIds + "&site=" + startupArgs.blogSiteId,
			dataType : 'text',
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});
	}

	LearningLogUtils.restoreSelectedPosts = function() {

		var selected = $('.blog_recycled_post_checkbox:checked');

        if(selected.length <= 0) {
            // No posts selected for restoration
            return;
        }

		var postIds = '';

		for(var i=0,j=selected.length;i<j;i++) {
			postIds += selected[i].id;
			if(i < (j - 1)) postIds += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/learninglog-post/restore?posts=" + postIds,
			dataType : 'text',
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to restore selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});
	}

	LearningLogUtils.getCurrentUserPermissions = function() {

		var permissions = null;
		jQuery.ajax( {
	 		url : "/direct/site/" + startupArgs.blogSiteId + "/userPerms/learninglog.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms,status) {
				permissions = perms.data;
			},
			error : function(xHR,stat,error) {
				alert("Failed to get the current user learninglog permissions. Status: " + stat + ". Error: " + error);
			}
	  	});

		jQuery.ajax( {
	 		url : "/direct/site/" + startupArgs.blogSiteId + "/userPerms/site.upd.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms,status) {
                permissions = permissions.concat(perms.data);
			},
			error : function(xHR,stat,error) {
				alert("Failed to get the current user site permissions. Status: " + stat + ". Error: " + error);
			}
	  	});
	  	
	  	return permissions;
	}

	LearningLogUtils.findPost = function(postId) {

		var post = null;
		
		if(!blogCurrentPosts) {

			jQuery.ajax( {
	 			url : "/direct/learninglog-post/" + postId + ".json",
	   			dataType : "json",
	   			async : false,
	   			cache : false,
		   		success : function(p,status) {
					post = p;
				},
				error : function(xmlHttpRequest,stat,error) {
					alert("Failed to get the post. Status: " + stat + ". Error: " + error);
				}
	  		});
	  	} else {

			for(var i=0,j=blogCurrentPosts.length;i<j;i++) {
				if(blogCurrentPosts[i].id === postId) {
					post = blogCurrentPosts[i];
                }
			}
		}

		return post;
	}

	LearningLogUtils.deleteComment = function(commentId) {

		if(!confirm(blog_delete_comment_message)) {
			return false;
        }
		
		jQuery.ajax( {
	 		url : "/direct/learninglog-comment/" + commentId + "?siteId=" + startupArgs.blogSiteId,
			type:'DELETE',
		   	success : function(text,status) {
                switchState('userPosts',{'userId':blogCurrentPost.creatorId});
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete comment. Status: " + status + ". Error: " + error);
			}
	  	});
	}

}) ();
