+ CodexTemplater {
	patternRendererPath {
		^(
			Main.packages.asDict.at('PatternRenderer')
			+/+"Templates"
		);
	}

	patternRenderer { | templateName("pattern") |
		this.makeExtTemplate(
			templateName,
			"patternRenderer",
			this.patternRendererPath
		);
	}

	patternRenderer_cleanup { | templateName("cleanup") |
		this.makeExtTemplate(
			templateName,
			"patternRenderer_cleanup",
			this.patternRendererPath
		);
	}
}
