+ Templater { 
	patternRendererPath { 
		^(
			Main.packages.asDict.at('PatternRenderer')	
			+/+"templates"
		);
	}

	patternRenderer { | templateName("pattern") |
		//change the template path to look in this directory
		this.setTemplatePath(this.patternRendererPath);
		//make the template
		this.makeTemplate(
			templateName, 
			"patternRenderer"
		);
		//reset the template path to the default
		this.resetTemplatePath;
	}

    patternRenderer_cleanup { | templateName("cleanup") |
        this.setTemplatePath(this.patternRendererPath); 
        this.makeTemplate(templateName, path, "patternRenderer_cleanup"); 
        this.resetTemplatePath; 
    }
}
