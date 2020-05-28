PatternRenderer : ModuleManager {
	var <synthDef, <pattern, <fileIncrementer;
	var nRenderRoutine, renderRoutine, <server;
	var <>sampleRate = 48e3, <>headerFormat = "wav";
	var <>sampleFormat = "int32", <>verbosity = -2;
	var synthDefProcessor, repressMessages = false;

	*new {|moduleName(\default), from|
		^super.new(moduleName, from).init;
	}

	init {
		fileIncrementer = FileIncrementer.new(
			"pattern-render-.wav",
			"~/Desktop/audio/pattern-renders".standardizePath
		);
		synthDefProcessor = SynthDefProcessor.new;
		server = Server.default;
	}

	server_{|newServer|
		if(newServer.isKindOf(Server), {server = newServer});
	}

	loadModules {
		super.loadModules;
		synthDef = modules.synthDef;
		pattern = modules.pattern;
	}

	/*synthDef_{|input|
		synthDef.name = this.formatSynthName(input);
	}*/

	render {|duration = 10, normalize = false|
		this.renderBackEnd(duration, normalize);
		^this;
	}

	renderN {|n, duration = 10, normalize = false|
		if(this.isRendering.not and: {this.isRenderingN.not}){
			nRenderRoutine = Routine({
				n.do{
					this.renderBackEnd(duration.value, normalize);
					while({this.isRendering}, {1e-4.wait});
				};
				nRenderRoutine = nil;
			}).play;
		}/*ELSE*/{"Warning: Render already in ogress".postln};
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

	*formatSynthName {|input|
		var nameString = this.name.asString;
		if(input.name.contains(nameString), {
			^format("%_%", nameString, input.asString).asSymbol;
		});
		^input.asSymbol;
	}

	formatSynthName {|input|
		^this.class.formatSynthName(input);
	}

	makeSynthDefBundle {|score, toAdd|
		score.add([0, [\d_recv, toAdd.asBytes]]);
	}

	addSynthDefBundle { |score|
		synthDef.do({ |item|
			this.makeSynthDefBundle(score, item);
		});
	}

	makeSynthDefCollsection {
		if(synthDef.isCollection.not, {
			synthDef = [synthDef];
		});
	}

	synhtDef {
		if(synthDef.size==1){
			^synthDef[0];
		};
		^synthDef;
	}

	getScore { |duration(1)|
		var score = Score.new;
		this.addSynthDefBundle(score);
		pattern.value(duration)
		.asScore(duration).score.do{|bundle|
			score.add(bundle);
		};
		score.sort;
		^score;
	}

	prepareToRender {
		this.loadModules;
		synthDefProcessor.add(synthDef);
		this.checkFolder;
	}

	renderBackEnd {|duration, normalize|
		if(this.isRendering.not, {
			this.prepareToRender;
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
					options: this.getServerOptions,
					completionString: "",
					duration: duration,
					action: {this.cleanUp(oscpath, path, normalize)}
				);
			};
		}, {"Warning: Render already in ogress".postln});
	}

	cleanUp { |oscpath, filepath, normalize|
		oscpath !? {File.delete(oscpath)};
		this.renderMessage(filepath);
		renderRoutine = nil;
		synthDefProcessor.remove(synthDef);
//		if(normalize, {
			// filepath.normalizePathAudio(0.8);
//		});
	}

	renderMessage { |path|
		if(repressMessages.not, {
			format(
				"\n% rendered\n", 
				PathName(path).fileNameWithoutExtension
			).postln;
		});
	}

	repressMessages_{|newbool|
		if(newbool.isKindOf(Boolean), {repressMessages = newbool});
	}

	getServerOptions {
		^ServerOptions.new
		.sampleRate_(sampleRate)
		.verbosity_(verbosity)
	}

	checkFolder {
		if(this.folder.isPath.not, {
			File.mkdir(this.folder);
			fileIncrementer.reset;
			^false;
		});
		^true;
	}

}
