learninglog.utils = {};

learninglog.utils.removeAttachment = function(name,postId,elementId) {

    if (!confirm("Are you sure you want to delete this attachment?")) {
        return;
    }

    $.ajax( {
        url: "/direct/learninglog-post/" + postId + "/deleteAttachment.json?siteId=" + learninglog.startupArgs.blogSiteId + "&name=" + name,
        dataType: "text",
        async: false,
        cache: false,
        success: function (text, textStatus) {
            $("#" + elementId).remove();
        },
        error: function (xhr, textStatus, errorThrown) {
            alert("Failed to delete attachment. Reason: " + errorThrown);
        }
    });
};

learninglog.utils.addFormattedDatesToCurrentPosts = function () {

    for (var i=0,j=learninglog.currentPosts.length;i<j;i++) {
        this.addFormattedDatesToPost(learninglog.currentPosts[i]);
    }
};

learninglog.utils.addFormattedDatesToPost = function (post) {

    post.formattedCreatedDate = this.formatDate(post.createdDate);
    post.formattedModifiedDate = this.formatDate(post.modifiedDate);

    for (var i=0,j=post.comments.length;i<j;i++) {
        post.comments[i].formattedCreatedDate = this.formatDate(post.comments[i].createdDate);
        post.comments[i].formattedModifiedDate = this.formatDate(post.comments[i].modifiedDate);
    }
};

learninglog.utils.addFormattedDatesToCurrentPost = function () {
    this.addFormattedDatesToPost(learninglog.currentPost);
};

learninglog.utils.formatDate = function (longDate) {

    if (longDate <= 0) {
        return none;
    }

    var d = new Date(longDate);
    var hours = d.getHours();
    if (hours < 10) hours = '0' + hours;
    var minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getDate() + " " + blog_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
};

learninglog.utils.addEscapedCreatorIdsToComments = function (post) {

    var comments = post.comments;
    for (var i=0,j=comments.length;i<j;i++) {
        comments[i].escapedCreatorId = escape(comments[i].creatorId);
    }
};

learninglog.utils.attachProfilePopup = function () {

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
};

learninglog.utils.parsePermissions = function () {

    var roleMapList = [];

    $.ajax({
        url: "/direct/learninglog-role/" + learninglog.startupArgs.blogSiteId + ".json",
        dataType: "json",
        async: false,
        cache: false,
        success: function (roles) {

            for (var r in roles.data) {
                roleMapList.push({'sakaiRole': r, 'llRole': roles.data[r]});
            }
        },
        error: function (xmlHttpRequest, status, errorThrown) {
            alert("Failed to get posts. Reason: " + errorThrown);
        }
    });

    return roleMapList;
};

learninglog.utils.savePermissions = function () {

    var boxes = $('.blog_role_radiobutton:checked');
    var myData = {'siteId':learninglog.startupArgs.blogSiteId};

    for (var i=0,j=boxes.length;i<j;i++)
        myData[boxes[i].name] = boxes[i].value;

    $.ajax( {
        url: "/direct/learninglog-role/new",
        type: 'POST',
        data: myData,
        timeout: 30000,
        dataType: 'text',
        success: function (result) {
            location.reload();
        },
        error: function (xmlHttpRequest, status, error) {
            alert("Failed to create meeting. Status: " + status + '. Error: ' + error);
        }
    });

    return false;
};

learninglog.utils.savePostAsDraft = function (isAutosave) {
    return this.storePost('PRIVATE', false, isAutosave);
};

learninglog.utils.publishPost = function () {

    if (!confirm(blog_publish_post_message)) {
        return false;
    }

    this.storePost('READY', true);
};

learninglog.utils.storePost = function (visibility, isPublish, isAutosave) {

    var title = $('#blog_title_field').val();
    if (title.length < 4) {
        alert("You must add a title of at least 4 characters.");
        return false;
    }

    var content = learninglog.sakai.getEditorData('blog_content_editor');
    if (content === '') {
        alert("You need to add some content!");
        return false;
    }

    $('#blog_visibility_field').val(visibility);
    if (isPublish) {
        $('#blog_mode_field').val('publish');
    }

    if (isAutosave && isAutosave == true) {
        $('#blog_autosave_field').val('yes');
    } else {
        $('#blog_autosave_field').val('no');
    }

    $('#ll_post_form').submit();
};

learninglog.utils.autosaveComment = function () {

    if(!learninglog.sakai.isEditorDirty('blog_content_editor')) {
        return 0;
    }

    return this.storeComment('AUTOSAVE');
};

learninglog.utils.saveCommentAsDraft = function () {
    return this.storeComment('PRIVATE');
};

learninglog.utils.publishComment = function () {
    this.storeComment('READY');
};

learninglog.utils.storeComment = function (visibility) {

    var comment = {
        'id': $('#blog_comment_id_field').val(),
        'postId': learninglog.currentPost.id,
        'content': learninglog.sakai.getEditorData('blog_content_editor'),
        'siteId': learninglog.startupArgs.blogSiteId,
        'visibility': visibility
    };

    $.ajax( {
        url: "/direct/learninglog-comment/new",
        type: 'POST',
        data: comment,
        timeout: 30000,
        dataType: 'text',
        success: function (id) {

            $('#blog_comment_id_field').val(id);

            if (visibility === 'AUTOSAVE') {

                learninglog.sakai.resetEditor('blog_content_editor');

                // Flash the autosaved message
                $('#learninglog_autosaved_message').show();
                setTimeout(function() {
                        $('#learninglog_autosaved_message').fadeOut(200);
                    },2000);
            } else {
                learninglog.switchState('userPosts',{'userId':learninglog.currentPost.creatorId});
            }
        },
        error: function (xmlHttpRequest, status, error) {
            alert("Failed to save comment. Status: " + status + '. Error: ' + error);
        }
    });
};

learninglog.utils.recyclePost = function (postId) {

    if (!confirm(blog_delete_post_message)) {
        return false;
    }

    $.ajax( {
        url: "/direct/learninglog-post/" + postId + "/recycle",
        dataType: 'text',
        cache: false,
        success: function (result) {
            learninglog.switchState('home');
        },
        error: function (xmlHttpRequest, status, error) {
            alert("Failed to recycle post. Status: " + status + '. Error: ' + error);
        }
    });
};

learninglog.utils.deleteSelectedPosts = function () {

    var selected = $('.blog_recycled_post_checkbox:checked');

    if (selected.length <= 0) {
        // No posts selected for deletion
        return;
    }

    if (!confirm(blog_really_delete_post_message)) {
        return false;
    }

    var postIds = '';

    for (var i=0,j=selected.length;i<j;i++) {
        postIds += selected[i].id;
        if(i < (j - 1)) postIds += ",";
    }

    $.ajax( {
        url: "/direct/learninglog-post/remove?posts=" + postIds + "&site=" + learninglog.startupArgs.blogSiteId,
        dataType: 'text',
        success: function (result) {
            learninglog.switchState('home');
        },
        error: function (xmlHttpRequest, status, error) {
            alert("Failed to delete selected posts. Status: " + status + '. Error: ' + error);
        }
    });
};

learninglog.utils.restoreSelectedPosts = function () {

    var selected = $('.blog_recycled_post_checkbox:checked');

    if (selected.length <= 0) {
        // No posts selected for restoration
        return;
    }

    var postIds = '';

    for (var i=0,j=selected.length;i<j;i++) {
        postIds += selected[i].id;
        if (i < (j - 1)) {
            postIds += ",";
        }
    }

    $.ajax( {
        url: "/direct/learninglog-post/restore?posts=" + postIds,
        dataType: 'text',
        success: function (result) {
            learninglog.switchState('home');
        },
        error: function (xmlHttpRequest, status, error) {
            alert("Failed to restore selected posts. Status: " + status + '. Error: ' + error);
        }
    });
};

learninglog.utils.getCurrentUserPermissions = function () {

    var permissions = null;
    $.ajax( {
        url: "/direct/site/" + learninglog.startupArgs.blogSiteId + "/userPerms/learninglog.json",
        dataType : "json",
        async: false,
        cache: false,
        success: function (perms, status) {
            permissions = perms.data;
        },
        error: function (xHR, stat, error) {
            alert("Failed to get the current user learninglog permissions. Status: " + stat + ". Error: " + error);
        }
    });

    $.ajax( {
        url: "/direct/site/" + learninglog.startupArgs.blogSiteId + "/userPerms/site.upd.json",
        dataType: "json",
        async: false,
        cache: false,
        success: function (perms, status) {
            permissions = permissions.concat(perms.data);
        },
        error: function (xHR, stat, error) {
            alert("Failed to get the current user site permissions. Status: " + stat + ". Error: " + error);
        }
    });

    return permissions;
};

learninglog.utils.findPost = function (postId) {

    var post = null;

    if (!learninglog.currentPosts) {

        $.ajax( {
            url: "/direct/learninglog-post/" + postId + ".json",
            dataType: "json",
            async: false,
            cache: false,
            success: function (p, status) {
                post = p;
            },
            error: function (xmlHttpRequest, stat, error) {
                alert("Failed to get the post. Status: " + stat + ". Error: " + error);
            }
        });
    } else {

        for (var i=0,j=learninglog.currentPosts.length;i<j;i++) {
            if (learninglog.currentPosts[i].id === postId) {
                post = learninglog.currentPosts[i];
            }
        }
    }

    return post;
};

learninglog.utils.deleteComment = function (commentId) {

    if (!confirm(blog_delete_comment_message)) {
        return false;
    }

    $.ajax( {
        url: "/direct/learninglog-comment/" + commentId + "?siteId=" + learninglog.startupArgs.blogSiteId,
        type: 'DELETE',
        success: function (text, status) {
            learninglog.switchState('userPosts',{'userId': learninglog.currentPost.creatorId});
        },
        error : function (xmlHttpRequest, status, error) {
            alert("Failed to delete comment. Status: " + status + ". Error: " + error);
        }
    });
};

learninglog.utils.renderTrimpathTemplate = function (templateName, contextObject, output) {

    var templateNode = document.getElementById(templateName);
    var firstNode = templateNode.firstChild;
    var template = null;

    if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4)) {
        template = templateNode.firstChild.data.toString();
    } else {
        template = templateNode.innerHTML.toString();
    }

    var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

    var render = trimpathTemplate.process(contextObject);

    if (output) {
        document.getElementById(output).innerHTML = render;
    }

    return render;
};
