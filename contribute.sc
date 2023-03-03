+ Raptor {
	*contribute { | versions |
		var folder = Main.packages.asDict.at(\Raptor)+/+"contributions";
		//Example modules to ship by default
		versions.add(\example -> (folder+/+"example"));
	}
}