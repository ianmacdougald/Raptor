+ ModuleTemplater { 
	patternRendererPath { 
		^(
			Main.packages.asDict.at('PatternRenderer')	
			+/+"templates"
		);
	}

	patternRenderer { |moduleName("pattern")|
		//change the template path to look in this directory
		this.class.setTemplatePath(this.patternRendererPath);
		//make the template
		this.class.makeTemplate(
			moduleName, 
			path, 
			"patternRenderer"
		);
		//reset the template path to the default
		this.class.resetTemplatePath;
	}
}
