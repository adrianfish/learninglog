var LearningLogUtils;

(function()
{
	if(LearningLogUtils == null)
		LearningLogUtils = new Object();

	LearningLogUtils.removeAttachment = function(attachmentId,postId,elementId) {
        if(!confirm("Are you sure you want to delete this attachment?"))
            return;

        jQuery.ajax( {
	 		url : "/direct/learninglog-post/" + postId + "/deleteAttachment.json?siteId=" + blogSiteId + "&attachmentId=" + attachmentId,
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
	 		url : "/direct/learninglog-role/" + blogSiteId + "/currentUserRole.json",
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
	
	LearningLogUtils.showSearchResults = function(searchTerms) {
    	jQuery.ajax( {
			url : "/portal/tool/" + blogPlacementId + "/search",
			type : 'POST',
        	dataType : "json",
        	async : false,
			cache: false,
        	data : {'searchTerms':searchTerms},
        	success : function(results) {
        		var hits = results;
				SakaiUtils.renderTrimpathTemplate('blog_search_results_template',{'results':hits},'blog_content');
	 			$(document).ready(function() {
	 				try {
						if(window.frameElement) {
							setMainFrameHeight(window.frameElement.id);
						}
					} catch(err) {
						// This is likely under an LTI provision scenario
					}
				});
        	},
        	error : function(xmlHttpRequest,status,error) {
				alert("Failed to search. Status: " + status + ". Error: " + error);
			}
		});
	}
	
	LearningLogUtils.addFormattedDatesToCurrentPosts = function () {
        for(var i=0,j=blogCurrentPosts.length;i<j;i++) {
        	LearningLogUtils.addFormattedDateToPost(blogCurrentPosts[i]);
        }
    }
    
    LearningLogUtils.addFormattedDateToPost = function(post) {
            var d = new Date(post.createdDate);
            var formattedCreatedDate = d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
            post.formattedCreatedDate = formattedCreatedDate;

            d = new Date(post.modifiedDate);
            var formattedModifiedDate = d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
            post.formattedModifiedDate = formattedModifiedDate;

            for(var k=0,m=post.comments.length;k<m;k++) {
                d = new Date(post.comments[k].createdDate);
                formattedCreatedDate = d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
                post.comments[k].formattedCreatedDate = formattedCreatedDate;

                d = new Date(post.comments[k].modifiedDate);
                var formattedModifiedDate = d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + d.getMinutes();
                post.comments[k].formattedModifiedDate = formattedModifiedDate;
            }
        }
    
    LearningLogUtils.addFormattedDatesToCurrentPost = function () {
    	LearningLogUtils.addFormattedDateToPost(blogCurrentPost);
	}
	
	LearningLogUtils.attachProfilePopup = function() {
	
		if(blogInPDA) return;
		
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

	LearningLogUtils.setPostsForCurrentSite = function() {

		jQuery.ajax( {
	       	url : "/direct/learninglog-post.json?siteId=" + blogSiteId,
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(data) {
				blogCurrentPosts = data['learninglog-post_collection'];
                LearningLogUtils.addFormattedDatesToCurrentPosts();
			},
			error : function(xmlHttpRequest,status,errorThrown) {
				alert("Failed to get posts. Reason: " + errorThrown);
			}
	   	});
	}

	LearningLogUtils.parsePermissions = function() {
		var roleMapList = [];
		
		jQuery.ajax({
	    	url : "/direct/learninglog-role/" + blogSiteId + ".json",
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
		var myData = {'siteId':blogSiteId};

		for(var i=0,j=boxes.length;i<j;i++)
			myData[boxes[i].name] = boxes[i].value;

		jQuery.ajax( {
	 		url : "/direct/learninglog-role/new",
			type : 'POST',
			data : myData,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(result) {
				switchState('home');
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

        alert(isAutosave);
	
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
	
	LearningLogUtils.saveComment = function() {

		var comment = {
				'id':$('#blog_comment_id_field').val(),
				'postId':blogCurrentPost.id,
				'content':SakaiUtils.getEditorData('blog_content_editor'),
				'siteId':blogSiteId
				};

		jQuery.ajax( {
	 		url : "/direct/learninglog-comment/new",
			type : 'POST',
			data : comment,
			timeout: 30000,
			async : false,
			dataType: 'text',
		   	success : function(id) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to save comment. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	LearningLogUtils.recyclePost = function(postId) {
		if(!confirm(blog_delete_post_message))
			return false;

		jQuery.ajax( {
	 		url : "/direct/learninglog-post/" + postId + "/recycle",
			dataType : 'text',
			async : false,
			cache : false,
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to recycle post. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	LearningLogUtils.deleteSelectedPosts = function() {
		var selected = $('.blog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/learninglog-post/" + selected[i].id + "?siteId=" + blogSiteId;
			if(i < (j - 1)) commands += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/batch?_refs=" + commands,
			dataType : 'text',
			type:'DELETE',
			async : false,
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	LearningLogUtils.restoreSelectedPosts = function() {
		var selected = $('.blog_recycled_post_checkbox:checked');

		var commands = '';

		for(var i=0,j=selected.length;i<j;i++) {
			commands += "/direct/learninglog-post/" + selected[i].id + "/restore";
			if(i < (j - 1)) commands += ",";
		}

		jQuery.ajax( {
	 		url : "/direct/batch?_refs=" + commands,
			dataType : 'text',
			async : false,
		   	success : function(result) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to restore selected posts. Status: " + status + '. Error: ' + error);
			}
	  	});

		return false;
	}

	LearningLogUtils.getCurrentUserPermissions = function() {
		var permissions = null;
		jQuery.ajax( {
	 		url : "/direct/site/" + blogSiteId + "/userPerms/learninglog.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms,status) {
				permissions = perms;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user permissions. Status: " + stat + ". Error: " + error);
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
	  	}
		else {
			for(var i=0,j=blogCurrentPosts.length;i<j;i++) {
				if(blogCurrentPosts[i].id === postId)
					post = blogCurrentPosts[i];
			}
		}

		return post;
	}

	LearningLogUtils.deleteComment = function(commentId) {
		if(!confirm(blog_delete_comment_message))
			return false;
		
		jQuery.ajax( {
	 		url : "/direct/learninglog-comment/" + commentId + "?siteId=" + blogSiteId,
	   		async : false,
			type:'DELETE',
		   	success : function(text,status) {
				switchState('home');
			},
			error : function(xmlHttpRequest,status,error) {
				alert("Failed to delete comment. Status: " + status + ". Error: " + error);
			}
	  	});
	  	
	  	return false;
	}
}) ();
