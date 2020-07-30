+ CodexTemplater {
	codexRendererPath {
		^(
			Main.packages.asDict.at('PatternRenderer')
			+/+"Templates"
		);
	}

	codexRenderer { | templateName("pattern") |
		this.makeExtTemplate(
			templateName,
			"codexRenderer",
			this.codexRendererPath
		);
	}

	codexRenderer_cleanup { | templateName("cleanup") |
		this.makeExtTemplate(
			templateName,
			"codexRenderer_cleanup",
			this.codexRendererPath
		);
	}
}
