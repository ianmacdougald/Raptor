Raptor : Codex {
	var <folder, fileName, <>options;
	var extension, i = 0, semaphore;
	var routArray;

	initCodex {
		//Initialize ServerOptions
		options = options ?? {
			Server.default.options.copy
			.verbosity_(-1)
			.sampleRate_(48e3)
			.memSize_(2.pow(19))
			.recSampleFormat_("int24")
		};
		semaphore = semaphore ? Semaphore(1);
		//Use the recording directory by default
		this.folder = folder ?? {
			thisProcess.platform.recordingsDir+/+"raptor-renders";
		};
		fileName ?? { this.fileName = "raptor.wav" };
	}

	folder_{ | newFolder |
		if(newFolder.isString){
			folder = newFolder;
			folder.mkdir;
			i = 0;
		};
	}

	fileName_{ | newName |
		//Get the new extension.
		extension = PathName(newName).extension;
		//If there isn't one, use "wav" as default.
		if(extension.isEmpty){ extension = "wav" };
		//Set the ServerOptions to encode the extension format.
		options.recHeaderFormat = extension;
		//Get the file name without the extension for incrementing.
		newName = newName[0..(newName.size - 1 - extension.size - 1)];
		//If there is a number at the end of the fileName, strip it.
		fileName = newName.noEndNumbers;
		//Offset the incrementer by that number (0 by default).
		i = newName.endNumber;
	}

	fileName { ^(fileName++"."++extension) }

	*makeTemplates {  | templater |
		templater.synthDef;
		templater.pattern;
	}

	*contribute { | versions |
		var toQuark = Main.packages.asDict.at(\Raptor);
		versions.add(\example -> (toQuark+/+"example"));
	}

	renderN { | n(2), duration(1)  |
		routArray = n.collect { this.render(duration.value) };
	}

	stop {
		try { routArray.do(_.stop) };
	}

	fullPath {
		^(folder+/+fileName++i++"."++extension);
	}

	increment {
		while({ this.fullPath.exists }){
			i = i + 1;
		};
		^this.fullPath;
	}

	getScore { | duration(1.0) |
		var score = modules.pattern(duration).asScore(duration, 0, modules.asEvent);
		score.score = [[0, [\d_recv, modules.synthDef.asBytes]]]++score.score;
		score.add([duration, [\d_free, modules.synthDef.name.asString]]);
		^score;
	}

	render { | duration(1.0) |
		^fork {
			var oscpath, path;
			semaphore.wait;
			oscpath = PathName.tmp+/+UniqueID.next++".osc";
			path = this.increment;
			this.getScore(duration).recordNRT(
				oscpath,
				path,
				nil,
				options.sampleRate,
				options.recHeaderFormat,
				options.recSampleFormat,
				options, "", duration,
				{
					oscpath !? { File.delete(oscpath) };
					format("\n% rendered\n", PathName(path).fileName).postln;
					semaphore.signal;
				}
			);
		}
	}

	play { | clock(TempoClock.default) |
		^modules.pattern.play(clock, modules.asEvent);
	}
}
