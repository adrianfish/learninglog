function LearningLogPermissions(data) {

	for(var i=0,j=data.length;i<j;i++) {
		if('learninglog.modify.permissions' === data[i] || 'site.upd' === data[i]) {
			this.modifyPermissions = true;
            break;
        }
	}
}
