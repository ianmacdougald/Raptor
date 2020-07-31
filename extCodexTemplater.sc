+ CodexTemplater {
	Raptor { | templateName("pattern") |
		this.makeExtTemplate(
			templateName,
			"Raptor",
			thisMethod.filenameString.dirname
		);
	}
}
