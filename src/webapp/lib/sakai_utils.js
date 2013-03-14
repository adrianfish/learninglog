var SakaiUtils;

(function()
{
	if(SakaiUtils == null)
		SakaiUtils = new Object();
		
	SakaiUtils.getCurrentUser = function() {
		var user = null;
		jQuery.ajax( {
	 		url : "/direct/user/current.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(u) {
				user = u;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user. Status: " + stat + ". Error: " + error);
			}
	  	});

		return user;
	}

	SakaiUtils.getProfileMarkup = function(userId) {
		var profile = '';

		jQuery.ajax( {
	       	url : "/direct/profile/" + userId + "/formatted",
	       	dataType : "html",
	       	async : false,
			cache: false,
		   	success : function(p) {
				profile = p;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
			}
	   	});

		return profile;
	}
	
	SakaiUtils.readCookie = function(name) {
    	var nameEQ = name + "=";
    	var ca = document.cookie.split(';');
    	for(var i=0;i < ca.length;i++) {
        	var c = ca[i];
        	while (c.charAt(0)==' ') c = c.substring(1,c.length);
        	if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    	}
    	return null;
	}
	
	SakaiUtils.getParameters = function() {
		var arg = new Object();
		var href = document.location.href;

		var paramString = '';
		
		if ( href.indexOf( "?") != -1) {
			var paramString = href.split( "?")[1];
		}
		else {
			// No url params. Let's try the cookie
			var paramString = SakaiUtils.readCookie('sakai-tool-params');
		}
			
		if(paramString.indexOf("#") != -1)
			paramString = paramString.split("#")[0];
				
		var params = paramString.split("&");

		for (var i = 0; i < params.length; ++i) {
			var name = params[i].split( "=")[0];
			var value = params[i].split( "=")[1];
			arg[name] = unescape(value);
		}
	
		return arg;
	}

	SakaiUtils.getCurrentUserPermissions = function(siteId,scope) {
		var permissions = null;
		jQuery.ajax( {
	 		url : "/direct/site/" + siteId + "/userPerms/" + scope + ".json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms,status) {
				permissions = perms.data;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user permissions. Status: " + stat + ". Error: " + error);
			}
	  	});
	  	
	  	return permissions;
	}

	SakaiUtils.getSitePermissionMatrix = function(siteId,scope) {
        var perms = [];

        jQuery.ajax( {
            url : "/direct/site/" + siteId + "/perms/" + scope + ".json",
            dataType : "json",
            async : false,
            cache: false,
            success : function(p) {
                for(role in p.data) {
                    var permSet = {'role':role};

                    for(var i=0,j=p.data[role].length;i<j;i++) {
                        var perm = p.data[role][i].replace(/\./g,"_");
                        eval("permSet." + perm + " = true");
                    }

                    perms.push(permSet);
                }
            },
            error : function(xmlHttpRequest,stat,error) {
                alert("Failed to get permissions. Status: " + stat + ". Error: " + error);
            }
        });

        return perms;
    }

	SakaiUtils.savePermissions = function(siteId,checkboxClass,callback) {
        var boxes = $('.' + checkboxClass);
        var myData = {};
        for(var i=0,j=boxes.length;i<j;i++) {
            var box = boxes[i];
            if(box.checked)
                myData[box.id] = 'true';
            else
                myData[box.id] = 'false';
        }

        jQuery.ajax( {
            url : "/direct/site/" + siteId + "/setPerms",
            type : 'POST',
            data : myData,
            timeout: 30000,
            async : false,
            dataType: 'text',
            success : function(result) {
                callback();
            },
            error : function(xmlHttpRequest,status,error) {
                alert("Failed to create meeting. Status: " + status + '. Error: ' + error);
            }
        });

        return false;
    }
	
	SakaiUtils.renderTrimpathTemplate = function(templateName,contextObject,output) {
		var templateNode = document.getElementById(templateName);
		var firstNode = templateNode.firstChild;
		var template = null;

		if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4))
  			template = templateNode.firstChild.data.toString();
		else
   			template = templateNode.innerHTML.toString();

		var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

   		var render = trimpathTemplate.process(contextObject);

		if (output)
			document.getElementById(output).innerHTML = render;

		return render;
	}

	SakaiUtils.setupFCKEditor = function(textarea_id,width,height,toolbarSet,siteId) {
		var oFCKeditor = new FCKeditor(textarea_id);

		oFCKeditor.BasePath = "/library/editor/FCKeditor/";
		oFCKeditor.Width  = width;
		oFCKeditor.Height = height;
		oFCKeditor.ToolbarSet = toolbarSet;
		
		var collectionId = "/group/" + siteId + "/";
		
		oFCKeditor.Config['ImageBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['ImageUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId;

		oFCKeditor.Config['CurrentFolder'] = collectionId;

		oFCKeditor.Config['CustomConfigurationsPath'] = "/library/editor/FCKeditor/config.js";
		oFCKeditor.ReplaceTextarea();
		
		try {
        	if(window.frameElement) {
            	setMainFrameHeight(window.frameElement.id);
        	}
		} catch(err) {
			// This is likely under an LTI provision scenario
		}
	}
	
	SakaiUtils.setupCKEditor = function(textarea_id,width,height,toolbarSet,siteId) {
		// CKEDITOR.basePath already set
		
		if (CKEDITOR.instances[textarea_id]) {
			CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
		}
		
		if ('Default' === toolbarSet) {
			toolbarSet = 'Full';
		}
		
		var collectionId = "/group/" + siteId + "/";
		
		// used to point to file manager/browser 
		fckBasePath = "/library/editor/FCKeditor/";
	
		CKEDITOR.replace(textarea_id, {
			width:width,
			height:height,
			toolbar:toolbarSet,
			skin: 'v2',
			customConfigurationsPath:"/library/editor/ckeditor/config.js",
			/* using FCK browser: see https://jira.sakaiproject.org/browse/SAK-17885 */
			filebrowserBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html",
			filebrowserImageBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId,
			filebrowserLinkBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId,
			filebrowserFlashBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId,
			filebrowserImageUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId,
			filebrowserFlashUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId,
			filebrowserLinkUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId
		});
		
		CKEDITOR.instances[textarea_id].on('instanceReady',function (e) {
			try {
            	if(window.frameElement) {
                	setMainFrameHeight(window.frameElement.id);
            	}
			} catch(err) {
				// This is likely under an LTI provision scenario
			}
        });
	}
	
	SakaiUtils.setupWysiwygEditor = function(textarea_id,width,height,toolbarSet,siteId) {
		if ('FCKeditor' === wysiwygEditor) {
			SakaiUtils.setupFCKEditor(textarea_id,width,height,toolbarSet,siteId);
		} else if ('ckeditor' === wysiwygEditor) {
			SakaiUtils.setupCKEditor(textarea_id,width,height,toolbarSet,siteId);
		}
	}
	
	SakaiUtils.getWysiwygEditor = function(textarea_id) {
		if ('FCKeditor' === wysiwygEditor) {
			return FCKeditorAPI.GetInstance(textarea_id);
		} else if ('ckeditor' === wysiwygEditor) {
			return CKEDITOR.instances[textarea_id];
		}
	}
	
	SakaiUtils.getEditorData = function(textarea_id) {
		if ('FCKeditor' === wysiwygEditor) {
			return SakaiUtils.getWysiwygEditor(textarea_id).GetXHTML(true);
		} else if ('ckeditor' === wysiwygEditor) {
			return SakaiUtils.getWysiwygEditor(textarea_id).getData();
		} else {
            return $('#' + textarea_id).val();
        }
	}
	
	SakaiUtils.resetEditor = function(textarea_id) {
		if ('FCKeditor' === wysiwygEditor) {
			SakaiUtils.getWysiwygEditor(textarea_id).ResetIsDirty();
		} else if ('ckeditor' === wysiwygEditor) {
			SakaiUtils.getWysiwygEditor(textarea_id).resetDirty();
		} else {
			blogTextAreaChanged = false;
		}
	}
		
	SakaiUtils.isEditorDirty = function(textarea_id) {
		if ('FCKeditor' === wysiwygEditor) {
			return SakaiUtils.getWysiwygEditor(textarea_id).IsDirty();
		} else if ('ckeditor' === wysiwygEditor) {
			return SakaiUtils.getWysiwygEditor(textarea_id).checkDirty();
		} else {
			return blogTextAreaChanged;
		}
	}

}) ();
