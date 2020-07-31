+ CodexTemplater {
	codexRenderer { | templateName("pattern") |
		this.makeExtTemplate(
			templateName,
			"codexRenderer",
			thisMethod.filenameString.dirname+/+"Templates"
		);
	}
}
