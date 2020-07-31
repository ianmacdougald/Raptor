+ CodexTemplater {
	raptor { | templateName("pattern") |
		this.makeExtTemplate(
			templateName,
			"Raptor",
			thisMethod.filenameString.dirname
		);
	}
}
