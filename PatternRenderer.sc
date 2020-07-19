PatternRenderer : CodexHybrid {
	var <incrementer, <options, folder, prIsRendering = false;
	var nRenderer, renderRoutine, server;

	initHybrid {
		incrementer = incrementer ?? { CodexIncrementer.new(
			"pattern-render.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		) };
		incrementer.folder.mkdir;
		options = options ?? { server.options.copy
			.recHeaderFormat_(incrementer.extension)
			.verbosity_(-1)
			.sampleRate_(48e3)
			.memSize_(2.pow(19))
			.recSampleFormat_("int24") };
	}

	processSynthDefs { processor.add(this.nameSynthDefs) }

	removeSynthDefs { processor.remove(this.findSynthDefs) }

	*makeTemplates { | templater |
		templater.synthDef;
		templater.patternRenderer;
		templater.patternRenderer_cleanup;
	}

	*defaultModulesPath {
		^this.filenameSymbol.asString.dirname+/+"Defaults";
	}

	renderN { | n(2), duration(1), normalize(false) |
		if(this.isRendering.not){
			nRenderer = Routine({
				n.do{
					this.render(duration.value, normalize);
					while({prIsRendering}, {1e-4.wait});
				};
			}).play;
		}/*ELSE*/{"Warning: Render already in progress".postln};
	}

	stop {
		this.stopRenderN;
		this.stopRender;
	}

	stopRenderN { if(nRenderer.isPlaying, { nRenderer.stop }) }

	stopRender { prIsRendering = false }

	reset { this.stop }

	isRendering { ^(prIsRendering or: {nRenderer.isPlaying}) }

	fileTemplate_{ | newTemplate |
		incrementer.fileTemplate = newTemplate;
		options.recHeaderFormat = incrementer.extension;
	}

	folder_{ | newFolder |
		incrementer.folder = newFolder.mkdir;
		incrementer.reset;
	}

	folder { ^incrementer.folder }

	fileTemplate { ^incrementer.fileTemplate }

	getScore { | duration(1.0) |
		var score = modules.pattern(duration)
		.asScore(duration);
		score.score = [[0, [\d_recv, modules.synthDef.asBytes]]]++score.score;
		score.add([duration*1.005, [\d_free, modules.synthDef.name.asString]]);
		^score;
	}

	render { | duration(1.0), normalize(false) |
		if(this.isRendering.not, {
			if(this.folder.exists.not, { this.folder.mkdir });
			renderRoutine = forkIfNeeded({
				var oscpath = PathName.tmp+/+
				UniqueID.next++".osc";
				var path = incrementer.increment;
				prIsRendering = true;
				this.getScore(duration).recordNRT(
					oscpath, path, nil,
					options.sampleRate,
					options.recHeaderFormat,
					options.recSampleFormat,
					options, "", duration,
					{this.cleanUp(oscpath, path, normalize)};
				);
			});
		}, {"Warning: Render already in progress".postln});
	}

	cleanUp { | oscpath, filepath, normalize(false) |
		oscpath !? {File.delete(oscpath)};
		this.renderMessage(filepath);
		if(normalize, {
			filepath.normalizePathAudio(0.8);
		});
		if(modules.cleanup.isEmpty.not, {
			modules.cleaunp.do(_.value);
			modules.cleanup.clear;
		});
		prIsRendering = false;
	}

	renderMessage { | path |
		format("\n% rendered\n", PathName(path)
			.fileNameWithoutExtension).postln;
	}

	checkFolder {
		var bool = this.folder.exists;
		if(bool.not, {
			File.mkdir(this.folder);
			incrementer.reset;
		});
		^bool;
	}

	normalizeFolder { | level(0.8) |
		PathName(this.folder).files.do{ | file |
			file.fullPath.normalizePathAudio(level);
		};
	}

	clearFolder {
		File.deleteAll(this.folder);
		this.folder = this.folder.copy;
	}
}
