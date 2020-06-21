PatternRenderer : Hybrid {
	var fileIncrementer, <options;
	var nRenderRoutine, renderRoutine, <server;

	initHybrid {
		fileIncrementer = FileIncrementer.new(
			"pattern-render-.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		);
		options = server.options.copy
		.recHeaderFormat_(fileIncrementer.extension)
		.verbosity_(-1)
		.sampleRate_(48e3)
		.recSampleFormat_("int24");
	}

	makeTemplates {
		templater.synthDef;
		templater.patternRenderer;
		templater.patternRenderer_cleanup;
	}

	render { | duration = 10, normalize = false |
		this.prRender(duration, normalize);
	}

	renderN { | n = 2, duration = 10, normalize = false |
		if(this.isRendering.not){
			nRenderRoutine = Routine({
				n.do{
					this.prRender(duration.value, normalize);
					while({this.prIsRendering}, {1e-4.wait});
				};
				nRenderRoutine = nil;
			}).play;
		}/*ELSE*/{"Warning: Render already in progress".postln};
	}

	reset {
		this.stopRenderN;
		this.stopRender;
	}

	stopRenderN {
		if(this.isRenderingN){
			nRenderRoutine.stop;
		};
		nRenderRoutine = nil;
	}

	stopRender {
		if(this.prIsRendering){
			renderRoutine.stop;
		};
		renderRoutine = nil;
	}

	prIsRendering { ^renderRoutine.isNil.not; }

	isRendering { ^(this.prIsRendering or: {this.isRenderingN}); }

	isRenderingN { ^nRenderRoutine.isPlaying; }

	fileTemplate_{  | newTemplate |
		fileIncrementer.fileTemplate = newTemplate;
		options.recHeaderFormat = fileIncrementer.extension;
	}

	folder_{ | newFolder | fileIncrementer.folder = newFolder; }

	fileTemplate { ^fileIncrementer.fileTemplate; }

	folder {
		if(fileIncrementer.isNil.not){
			^fileIncrementer.folder;
		};
		^nil;
	}

	getScore { | duration(1) |
		var score = Score.new;
		score.add(this.getSynthDefBundle(modules.synthDef));
		modules.pattern(duration, modules.synthDef.name)
		.asScore(duration).score.do{|bundle|
			score.add(bundle);
		};
		score.add([duration, [\d_free, modules.synthDef.name]]);
		score.sort;
		^score;
	}

	getSynthDefBundle { | synthDef |
		^[0, [\d_recv, synthDef.asBytes]];
	}

	prepareToRender {
		this.loadModules;
		this.checkFolder;
	}

	prRender { | duration, normalize(true) |
		if(this.isRendering.not, {
			this.prepareToRender;
			renderRoutine = forkIfNeeded{
				var oscpath = PathName.tmp+/+
				UniqueID.next++".osc";
				var path = fileIncrementer.increment;
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
		modules.cleaunp.do(_.value);
	}

	renderMessage { |path|
		format("\n% rendered\n", PathName(path).fileNameWithoutExtension).postln;
	}

	checkFolder {
		var bool = this.folder.exists;
		if(bool.not, {
			File.mkdir(this.folder);
			fileIncrementer.reset;
		});
		^bool;
	}
}
