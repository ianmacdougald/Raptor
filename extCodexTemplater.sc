+ CodexTemplater {
	raptor { | templateName("pattern") |
		this.makeTemplate(
			templateName, 
			thisMethod.filenameString.dirname+/+"Raptor.scd"
		);
	}
}
