+ Templater { 
	patternRendererPath { 
		^(
			Main.packages.asDict.at('PatternRenderer')	
			+/+"templates"
		);
	}

	patternRenderer { | templateName("pattern") |
		//change the template path to look in this directory
		this.class.setTemplatePath(this.patternRendererPath);
		//make the template
		this.class.makeTemplate(
			templateName, 
			path, 
			"patternRenderer"
		);
		//reset the template path to the default
		this.class.resetTemplatePath;
	}

    patternRenderer_cleanup { | templateName("cleanup") |
        this.class.setTemplatePath(this.patternRendererPath); 
        this.class.makeTemplate(templateName, path, "patternRenderer_cleanup"); 
        this.class.resetTemplatePath; 
    }
}
