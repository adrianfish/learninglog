var SakaiUtils;

(function() {

	if(SakaiUtils == null) {
		SakaiUtils = new Object();
    }
		
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
	
	SakaiUtils.renderTrimpathTemplate = function(templateName,contextObject,output) {

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
	}

	SakaiUtils.setupWysiwygEditor = function(textarea_id, width, height) {

        if (CKEDITOR.instances[textarea_id]) {
            CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
        }

        sakai.editor.launch(textarea_id,{},width,height);

        CKEDITOR.instances[textarea_id].on('instanceReady',function (e) {
            resizeMainFrame();
        });
	}
	
	SakaiUtils.getWysiwygEditor = function(textarea_id) {
        return CKEDITOR.instances[textarea_id];
	}
	
	SakaiUtils.getEditorData = function(textarea_id) {
        return SakaiUtils.getWysiwygEditor(textarea_id).getData();
	}
	
	SakaiUtils.resetEditor = function(textarea_id) {
        SakaiUtils.getWysiwygEditor(textarea_id).resetDirty();
	}
		
	SakaiUtils.isEditorDirty = function(textarea_id) {
			return SakaiUtils.getWysiwygEditor(textarea_id).checkDirty();
	}

}) ();
