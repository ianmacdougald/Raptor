+ Raptor {
	*contribute { | versions |
		var folder = Main.packages.asDict.at(\Raptor)+/+"contributions";
		//Example modules to ship by default
		versions.add(\example -> (folder+/+"example"));

		//02-03-2023 (March 2nd, 2023): Adding an ambience patch
		versions.add('ian-ambience1' -> (folder+/+"ian-ambience1"));
	}
}