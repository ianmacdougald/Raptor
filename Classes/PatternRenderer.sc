PatternRenderer : ModuleManager {
	classvar <server;
	var <synthDef, <pattern, <fileIncrementer;
	var nRenderRoutine, renderRoutine, <server;
	var <>sampleRate = 48e3, <>headerFormat = "wav";
	var <>sampleFormat = "int32", <>verbosity = -2;
	var synthDefProcessor;

	*new {|fileIncrementer|
		server = server ? Server.default;
		^super.new.init;
	}

	init {
		fileIncrementer = FileIncrementer.new(
			"pattern-render-.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		);
		synthDefProcessor = SynthDefProcessor.new;
		server = this.class.server;
	}

	*server_{|newServer|
		newServer = newServer ? Server.default;
	}

	loadModules {
		var objects = super.class.loadModules;
		synthDef = objects.synthDef;
		pattern = objects.pattern;
	}

	checkModules {
		this.loadModules;
	}

	synthDef_{|input|
		synthDef.name = this.prFormatSynthName(input);
	}

	render {|duration = 10, normalize = false|
		this.prRenderBackEnd(duration, normalize);
		^this;
	}

	renderN {|n, duration = 10, normalize = false|
		if(this.isRendering.not and: {this.isRenderingN.not}){
			nRenderRoutine = Routine({
				n.do{
					this.prRenderBackEnd(duration.value, normalize);
					while({this.isRendering}, {1e-4.wait});
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
		if(this.isRendering){
			renderRoutine.stop;
		};
		renderRoutine = nil;
	}

	reset {
		this.stop;
	}

	isRendering {
		^renderRoutine.isNil.not;
	}

	isRenderingN {
		^nRenderRoutine.isPlaying;
	}

	fileTemplate_{|newTemplate|
		fileIncrementer.fileTemplate = newTemplate;
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

	*prFormatSynthName {|input|
		var nameString = this.name.asString;
		if(input.name.contains(nameString), {
			^format("%_%", nameString, input.asString).asSymbol;
		});
		^input.asSymbol;
	}

	prFormatSynthName {|input|
		^this.class.prFormatSynthName(input);
	}

	getScore { |duration(1)|
		var score = Score.new;
		score.add([0, [\d_recv, synthDef.asBytes]]);
		pattern.value(duration)
		.asScore(duration).score.do{|bundle|
			score.add(bundle);
		};
		score.sort;
		^score;
	}

	prPrepareToRender {
		this.loadModules;
		synthDefProcessor.add(synthDef);
		this.prCheckFolder;
	}

	prRenderBackEnd {|duration, normalize|
		if(this.isRendering.not, {
			this.prPrepareToRender;
			renderRoutine = forkIfNeeded{
				var oscpath = PathName.tmp +/+ UniqueID.next ++ ".osc";
				var path = fileIncrementer.increment;
				this.getScore(duration).recordNRT(
					oscFilePath: oscpath,
					outputFilePath: path,
					inputFilePath: nil,
					sampleRate: sampleRate,
					headerFormat: headerFormat,
					sampleFormat: sampleFormat,
					options: this.prGetServerOptions,
					completionString: "",
					duration: duration,
					action: {this.prCleanUpRender(oscpath, path, normalize)}
				);
			};
		}, {"Warning: Render already in progress".postln});
	}

	prCleanUpRender { |oscpath, filepath, normalize|
		oscpath !? {File.delete(oscpath)};
		this.prRenderMessage(filepath);
		renderRoutine = nil;
		synthDefProcessor.remove(synthDef);
		if(normalize, {
			// filepath.normalizePathAudio(0.8);
		});
	}

	prRenderMessage { |path|
		format("\n% rendered\n", PathName(path).fileNameWithoutExtension).postln;
	}

	prGetServerOptions {
		^ServerOptions.new
		.sampleRate_(sampleRate)
		.verbosity_(verbosity)
	}

	prCheckFolder {
		var bool = this.folder.pathMatch.isEmpty.not;
		if(bool.not, {
			File.mkdir(this.folder);
			fileIncrementer.reset;
		});
		^bool;
	}

}