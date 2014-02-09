learninglog.sakai = {};

learninglog.sakai.getCurrentUser = function () {

	var user = null;

	$.ajax( {
 		url: "/direct/user/current.json",
   		dataType: "json",
   		async: false,
   		cache: false,
	   	success: function (u) {
			user = u;
		},
		error: function (xmlHttpRequest, stat, error) {
			alert("Failed to get the current user. Status: " + stat + ". Error: " + error);
		}
  	});

	return user;
};

learninglog.sakai.getProfileMarkup = function (userId) {

	var profile = '';

	$.ajax( {
       	url: "/direct/profile/" + userId + "/formatted",
       	dataType: "html",
       	async: false,
		cache: false,
	   	success: function (p) {
			profile = p;
		},
		error: function (xmlHttpRequest, stat, error) {
			alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
		}
   	});

	return profile;
};

learninglog.sakai.setupWysiwygEditor = function(textarea_id, width, height) {

    if (CKEDITOR.instances[textarea_id]) {
        CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
    }

    sakai.editor.launch(textarea_id,{},width,height);

    CKEDITOR.instances[textarea_id].on('instanceReady', function (e) {
        learninglog.resizeMainFrame();
    });
};

learninglog.sakai.getWysiwygEditor = function (textarea_id) {
    return CKEDITOR.instances[textarea_id];
};

learninglog.sakai.getEditorData = function (textarea_id) {
    return this.getWysiwygEditor(textarea_id).getData();
};

learninglog.sakai.resetEditor = function (textarea_id) {
    this.getWysiwygEditor(textarea_id).resetDirty();
};
	
learninglog.sakai.isEditorDirty = function (textarea_id) {
    return this.getWysiwygEditor(textarea_id).checkDirty();
};
