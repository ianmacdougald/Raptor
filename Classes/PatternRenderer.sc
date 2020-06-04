PatternRenderer : Modular {
	var fileIncrementer, <server, <options;
	var nRenderRoutine, renderRoutine, <server;
	var synthDefProcessor;

	*new {|moduleName, from|
		^super.new(moduleName, from).initPatternRenderer;
	}

	initPatternRenderer {
		fileIncrementer = FileIncrementer.new(
			"pattern-render-.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		);
		synthDefProcessor = SynthDefProcessor.new;
		server = Server.default;
		options = server.options.copy
			.recHeaderFormat_(fileIncrementer.extension)
			.verbosity_(-1)
			.sampleRate_(48e3)
			.recSampleFormat_("int24");
	}

	makeTemplates {
		templater.synthDef;
		templater.patternRenderer;
	}

	server_{|newServer|
		if(newServer.isKindOf(Server), {
			server = newServer;
		});
	}

	render {|duration = 10, normalize = false|
		this.prRender(duration, normalize);
	}

	renderN {|n = 2, duration = 10, normalize = false|
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

	stop {
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

	reset {
		this.stop;
	}

	prIsRendering { 
		^renderRoutine.isNil.not;
	}

	isRendering {
		^(this.prIsRendering or: {this.isRenderingN});
	}

	isRenderingN {
		^nRenderRoutine.isPlaying;
	}

	fileTemplate_{|newTemplate|
		fileIncrementer.fileTemplate = newTemplate;
		options.recHeaderFormat = fileIncrementer.extension;
	}

	folder_{|newFolder|
		fileIncrementer.folder = newFolder;
	}

	fileTemplate {
		^fileIncrementer.fileTemplate;
	}

	folder {
		if(fileIncrementer.isNil.not){
			^fileIncrementer.folder;
		};
		^nil;
	}

	*formatName {|input|
		var nameString = this.name.asString;
		if(input.name.contains(nameString), {
			^format("%_%", nameString, input.asString).asSymbol;
		});
		^input.asSymbol;
	}

	formatName {|input|
		^this.class.formatName(input);
	}

	makeSynthDefBundle {|score, toAdd|
		score.add([0, [\d_recv, toAdd.asBytes]]);
	}

	addSynthDefBundle { |score|
		modules.synthDef.do({ |item|
			this.makeSynthDefBundle(score, item);
		});
	}

	getScore { |duration(1)|
		var score = Score.new;
		this.addSynthDefBundle(score);
		modules.pattern(duration)
		.asScore(duration).score.do{|bundle|
			score.add(bundle);
		};
		score.sort;
		^score;
	}

	prepareToRender {
		this.loadModules;
		synthDefProcessor.add(modules.synthDef);
		this.checkFolder;
	}

	prRender {|duration, normalize(true)|
		if(this.isRendering.not, {
			this.prepareToRender;
			renderRoutine = forkIfNeeded{
				var oscpath = PathName.tmp+/+
				UniqueID.next++".osc";
				var path = fileIncrementer.increment;
				this.getScore(duration).recordNRT(
					oscFilePath: oscpath,
					outputFilePath: path,
					inputFilePath: nil,
					sampleRate: options.sampleRate,
					headerFormat: options.recHeaderFormat,
					sampleFormat: options.recSampleFormat,
					options: options,
					completionString: "",
					duration: duration,
					action: {this.cleanUp(
						oscpath, 
						path, 
						normalize
					)}
				);
			};
		}, {"Warning: Render already in progress".postln});
	}

	cleanUp { |oscpath, filepath, normalize(false)|
		oscpath !? {File.delete(oscpath)};
		this.renderMessage(filepath);
		renderRoutine = nil;
		synthDefProcessor.remove(modules.synthDef);
		if(normalize, {
			filepath.normalizePathAudio(0.8);
		});
	}

	renderMessage { |path|
		format(
			"\n% rendered\n", 
			PathName(path).fileNameWithoutExtension
		).postln;
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
