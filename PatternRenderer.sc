PatternRenderer : CodexHybrid {
	var <incrementer, <options, folder;
	var nRenderer, renderRoutine, server;

	initHybrid {
		incrementer = CodexIncrementer.new(
			"pattern-render.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		);
		options = server.options.copy
		.recHeaderFormat_(incrementer.extension)
		.verbosity_(-1)
		.sampleRate_(48e3)
		.recSampleFormat_("int24");
	}

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
					while({this.prIsRendering}, {1e-4.wait});
				};
			}).play;
		}/*ELSE*/{"Warning: Render already in progress".postln};
	}

	stop {
		this.stopRenderN;
		this.stopRender;
	}

	stopRenderN { if(this.isRenderingN, { nRenderer.stop }) }

	stopRender { if(this.prIsRendering, { renderRoutine.stop }) }

	reset { this.stop }

	prIsRendering { ^renderRoutine.isPlaying }

	isRendering { ^(this.prIsRendering or: {this.isRenderingN}) }

	isRenderingN { ^nRenderer.isPlaying }

	fileTemplate_{ | newTemplate |
		incrementer.fileTemplate = newTemplate;
		options.recHeaderFormat = incrementer.extension;
	}

	folder_{ | newFolder | incrementer.folder = newFolder.mkdir }

	folder { ^incrementer.folder }

	fileTemplate { ^incrementer.fileTemplate }

	/*getScore { | duration(1) |
		var score = Score.new;
		score.add([0, [\d_recv, modules.synthDef.asBytes]]);
		modules.pattern(duration)
		.asScore(duration).score.do{ | bundle |
			score.add(bundle);
		};
		score.add([duration, [\d_free, modules.synthDef.name]]);
		score.sort;
		^score;
	}*/

	getScore { | duration(1.0) |
		var score = modules.pattern(duration)
		.asScore(duration); 
		score.score = [[0, [\d_recv, modules.synthDef.asBytes]]]++score.score;
		score.add([duration*1.005, [\d_free, modules.synthDef.name.asString]]);
		^score;
	}

	render { | duration(1.0), normalize(false) |
		if(this.isRendering.not, {
			renderRoutine = forkIfNeeded{
				var oscpath = PathName.tmp+/+
				UniqueID.next++".osc";
				var path = incrementer.increment;
				this.getScore(duration).recordNRT(
					oscpath, path, nil,
					options.sampleRate,
					options.recHeaderFormat,
					options.recSampleFormat,
					options, "", duration,
					{this.cleanUp(oscpath, path, normalize)};
				);
			};
		}, {"Warning: Render already in progress".postln});
	}

	cleanUp { | oscpath, filepath, normalize(false) |
		oscpath !? {File.delete(oscpath)};
		this.renderMessage(filepath);
		renderRoutine = nil;
		if(normalize, {
			filepath.normalizePathAudio(0.8);
		});
		if(modules.cleanup.isEmpty.not, {
			modules.cleaunp.do(_.value);
			modules.cleanup.clear;
		});
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
}
