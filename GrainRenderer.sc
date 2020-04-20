GrainRenderer{
	classvar <mode = \mono;
	classvar server;
	classvar folderVar;
	classvar <fileIncrement;
	classvar <isRendering = false;
	classvar renderRoutine;

	*pr_ErrorPathMsg{
		Error("Can only render from String, PathName, or collections of these.").throw;
	}

	*pr_FormatPathCollections{|collection|

		var return;

		if(collection.isEmpty){
			this.pr_ErrorPathMsg;
		}/*ELSE*/{

			var strings = [];
			var otherCollections = [];

			collection.do{|item, index|

				if(item.isString.not and: {item.class!=PathName}){

					if(item.isCollection){

						item = this.pr_FormatPathCollections(item);
						otherCollections = otherCollections.add(item);

					}/*ELSE*/{
						this.pr_ErrorPathMsg;
					};

				}/*ELSE*/{

					if(item.class==PathName){

						item = item.fullPath;

					};

					strings = strings.add(item);

				};

			};

			return = strings;
			if(otherCollections.isEmpty.not){
				otherCollections.do{
					|item|
					return = return++item;
				};
			};

		};

		^return;
	}

	*pr_CollectFilePaths{
		|pathToFolder|

		var filepaths = [];

		if(pathToFolder.isCollection and: {pathToFolder.isString.not}){
			pathToFolder = this.pr_FormatPathCollections(pathToFolder);
		}/*ELSE*/{

			if(pathToFolder.isKindOf(PathName)){
				pathToFolder = pathToFolder.fullPath;
			};

			pathToFolder = [pathToFolder];

		};

		pathToFolder.do{|item|
			var pathname = PathName(item);


			if(pathname.folders.isEmpty.not){
				pathname.folders.do{|folder|
					filepaths = filepaths++this.pr_CollectFilePaths(folder.fullPath);
				};
			};

			pathname.files.do{|file|
				filepaths = filepaths.add(file.fullPath.asString);
			};
		};

		^filepaths;
	}

	*reset{
		isRendering = false;
	}

	*pr_CheckServer{
		server = server ? Server.default;

		if(server.hasBooted.not){
			Error("Server has not booted!").throw;
		};

		this.pr_LoadSynthDefs;
		^server;
	}

	*pr_LoadSynthDefs{
		SynthDef.new(\GrainRenderer_GrainMono, {

			var timescale = \timescale.kr(1);
			var env = EnvGen.ar(Env.sine(timescale),
				doneAction: Done.freeSelf
			);

			var buf = \buf.kr(0);
			var frames = BufFrames.kr(buf);
			var phasor = Phasor.ar(0, BufRateScale.kr(buf) * \rate.kr(1),
				0, SampleRate.ir * timescale
			) + (\pos.kr(0.0).clip(0.0, 1.0) * frames);
			var sig = BufRd.ar(1, buf, phasor);
			var out = sig * env * \ampDB.kr(-12).dbamp;
			OffsetOut.ar(\out.kr(0), Pan2.ar(out, \pan.kr(0)));

		}).store;

		SynthDef.new(\GrainRenderer_GrainStereo, {

			var timescale = \timescale.kr(1);
			var env = EnvGen.ar(Env.sine(timescale),
				doneAction: Done.freeSelf
			);

			var buf = \buf.kr(0);
			var frames = BufFrames.kr(buf);
			var phasor = Phasor.ar(0, BufRateScale.kr(buf) * \rate.kr(1),
				0, SampleRate.ir * timescale
			) + (\pos.kr(0.0).clip(0.0, 1.0) * frames);
			var sig = BufRd.ar(2, buf, phasor).sum / 2;
			var out = sig * env * \ampDB.kr(-12).dbamp;
			OffsetOut.ar(\out.kr(0), Pan2.ar(out, \pan.kr(0)));

		}).store;

		SynthDef.new(\GrainRenderer_GrainStereoBalance, {

			var timescale = \timescale.kr(1);
			var env = EnvGen.ar(Env.sine(timescale),
				doneAction: Done.freeSelf
			);

			var buf = \buf.kr(0);
			var frames = BufFrames.kr(buf);
			var phasor = Phasor.ar(0, BufRateScale.kr(buf) * \rate.kr(1),
				0, SampleRate.ir * timescale
			) + (\pos.kr(0.0).clip(0.0, 1.0) * frames);
			var sig = BufRd.ar(2, buf, phasor);
			var out = sig * env * \ampDB.kr(-12).dbamp;
			OffsetOut.ar(\out.kr(0), Balance2.ar(out[0], out[1], \pan.kr(0)));

		}).store;

		// this.pr_WindowSynthDef.store;

	}

	*render{|audioFilesToRender, duration = 20|
		var s = this.pr_CheckServer;

		if(File.exists(audioFilesToRender).not){
			Error("path does not point to folder").throw;
		};

		if(isRendering.not){

			forkIfNeeded{
				var defname;
				var tendtime = nil;
				var tenddur = nil;
				var tendstrum = nil;
				var tendpos = nil;
				var p;

				switch(mode,
					\mono, {defname = \GrainRenderer_GrainMono},
					\stereo, {defname = \GrainRenderer_GrainStereo},
					\stereoBalance, {defname = \GrainRenderer_GrainStereoBalance}
				);

				p = Pproto({
					var filepaths = this.pr_CollectFilePaths(audioFilesToRender);

					~bufArray = filepaths
					.collect({|item|
						(type: \allocRead, path: item).yield;
						// })
					});

				},

				Pbind(
					\instrument, defname,
					\type, \note,

					\bufnum, Pn(Plazy({|ev|

						var size = ev.bufArray.size;

						var pat = Pbrown(0.0, size - 1, 1.0, inf);

						pat;
					}), inf),

					\buf, Pfunc({|ev|
						ev.bufArray[ev.bufnum].bufNum;
					}),
					\pos,Pn(Plazy({|ev|

						var start = tendpos ?? {1.0.rand};
						var end = 1.0.rand;
						var time = exprand(1e-4, 8.0);

						tendpos = end;
						Pn(Pseg([start, end]*0.5, Pn(time, inf), \exp), 1)

					}), inf),
					\dur, Pn(Plazy({|ev|

						var start = tendtime ?? {exprand(1e-3, 1.0)};
						var end = exprand(1e-3, 1.0);
						var time = (start + end) * exprand(2.0, 20);

						tendtime = end;
						Pn(Pseg([start, end], Pn(time, inf), \exp), 1)

					}), inf) * {
						if(duration < 1){
							duration;
						}/*ELSE*/{
							1;
						};
					}.value,
					\windowAmp, Penv([0, 1, 1, 0], [0.01, 1, 0.01].normalizeSum * duration, \welch),
					\ampDB, (-9.dbamp * [3, 2, 1, 1, 0.5].squared.normalizeSum * Pkey(\windowAmp)).ampdb,
					\out, 0,

					\rate, [0.2, 0.4, 2.0, 4.0, 9.0] * exprand(0.5, 1.25),

					\timescale, Pkey(\dur) * Pn(Plazy({|ev|

						var start = tendtime ?? {exprand(0.125, 2.0)};
						var end = exprand(0.125, 2.0);
						var time = (start + end) * exprand(1e-4, 8.0);

						tendtime = end;
						Pn(Pseg([start, end], Pn(time, inf), \exp), 1)

					}), inf) * [1, 0.5, 0.25, 0.125, 0.125] * 4,

					\strum, Pn(Plazy({|ev|

						var start = tendstrum ?? {rrand(0, 1e-2)};
						var end = rrand(0, 1e-2);
						var time = (start + end) + 1e-4 * exprand(2.0, 8.0);

						tendstrum = end;
						Pn(Pseg([start, end], Pn(time, inf), \exp), 1)

					}), inf),

					\pan, Pwhite(-1.0, 1.0, inf)
				));

				s.sync;
				0.005.wait;

				forkIfNeeded{
					var path = this.pr_GetFileName;

					isRendering = true;

					p.render(
						path,
						duration, 48e3, "wav", "int32",
						options: s.options.copy.verbosity_(-2),
						action: {
							forkIfNeeded{
								var buf = Buffer.read(s, path);
								s.sync;
								buf.normalize(0.8);
								s.sync;
								buf.write(path);
								s.sync;
								buf.free;
								s.sync;
								s.sync;
								this.pr_RenderedMessage(path);
								// this.pr_WindowRender(path);
								nil;
							};
					});
				};
			};
		};
	}

	/*
	*pr_WindowSynthDef{
	^SynthDef.new(\GrainRenderer_Window, {
	var sig = SoundIn.ar([0, 1]);
	var env = EnvGen.ar(Env([0, 1, 1, 0], [0.01, 1, 0.01].normalizeSum, \welch),
	timeScale: \timescale.kr(1),
	doneAction: Done.freeSelf
	);
	var out = sig * env * \ampDB.kr(0).dbamp;
	Out.ar(\out.kr(0), out)
	});
	}


	*pr_WindowRender{
	|pathToRender|

	forkIfNeeded{

	var score = Score.new, synth;
	var sf = SoundFile.openRead(pathToRender);
	var tmpFile = PathName.tmp +/+ UniqueID.next++".wav";
	var oscpath = PathName.tmp +/+ UniqueID.next++".osc";
	var duration = sf.duration;

	score.add([
	0, [\d_recv, this.pr_WindowSynthDef.asBytes]
	]);

	score.add([
	0.001, (synth = Synth.basicNew(this.pr_WindowSynthDef.name)).newMsg(args: [
	\timescale, duration
	]);
	]);

	score.add([
	duration, [1]
	]);

	score.sort;

	score.recordNRT(
	oscpath, tmpFile,
	sf.path,
	48e3,
	"WAV",
	"int24",
	ServerOptions.new
	.verbosity_(-3)
	.sampleRate_(48e3)
	.memSize_(2.pow(19))
	.numOutputBusChannels_(sf.numChannels)
	.numInputBusChannels_(sf.numChannels)
	.numWireBufs_(2.pow(9)),
	action: {
	sf.close;
	File.delete(sf.path);
	File.copy(tmpFile, sf.path);
	File.delete(tmpFile);
	File.delete(oscpath);
	this.pr_RenderedMessage(sf.path);
	};
	);
	};
	}*/

	*pr_RenderedMessage{
		|path|
		format("\n% rendered\n", PathName(path).fileNameWithoutExtension).postln;
		isRendering = false;
	}

	*renderN{|n = 1, audioFolder, duration = 20|

		if(renderRoutine.isNil){

			CmdPeriod.doOnce({
				renderRoutine = nil;
			});

			renderRoutine = Routine({

				n.do{

					this.render(audioFolder, duration.value);

					while({isRendering}, {1e-4.wait});

				};

				renderRoutine = nil;

			}).play;
		}/*ELSE*/{
			if(renderRoutine.isPlaying){
				"Warning: rendering is already taking place. Stop before restarting".postln;
			};
		}

	}

	*stopRender{
		if(renderRoutine.isNil.not){
			if(renderRoutine.isPlaying){
				renderRoutine.stop;
				renderRoutine = nil;
			}
		};
	}

	*pr_GetFileName{
		// var ap = GrainRenderer_AudioPath;
		var origfile = "grain_render_%.wav";
		// var origfile = "grain-render"++"_"++ap.dayString++"_%.wav";		var origfile = "grain-render"++"_"++ap.dayString++"_%.wav";

		var path = this.folder, filename;

		filename = format(origfile, fileIncrement);
		filename = path +/+ filename;

		if(fileIncrement.isNil){
			filename = this.pr_FindNextFileName(0);
		}/*ELSE*/{
			fileIncrement = fileIncrement + 1;

			filename = format(origfile, fileIncrement);
			filename = path +/+ filename;

			if(File.exists(filename)){
				filename = this.pr_FindNextFileName(fileIncrement);
			};

		};

		^filename;
	}

	*pr_FindNextFileName{ |startingValue|
		// var ap = GrainRenderer_AudioPath;
		var origfile = "grain_render_%.wav";
		var path = this.folder;
		var f_inc = startingValue ?? {0};
		var filename = format(origfile, f_inc);
		filename = path +/+ filename;

		while({File.exists(filename)}, {
			f_inc = f_inc + 1;

			filename = format(origfile, f_inc);
			filename = path +/+ filename;

		});

		fileIncrement = f_inc;
		^filename;

	}

	*pr_ResetFileIncrement{
		fileIncrement = nil;
	}

	*folder{
		if(folderVar.isNil or: {folderVar != GrainRenderer_AudioPath.dailyPath}){
			this.pr_MakeFolder;
		};

		if(File.exists(folderVar).not){
			this.pr_MakeFolder;
		};

		^folderVar;
	}

	*pr_MakeFolder{
		folderVar = GrainRenderer_AudioPath.dailyPath;

		if(File.exists(folderVar).not){
			File.mkdir(folderVar);
			fileIncrement = nil;
		};
	}

	*folderPath_{|newpath|
		GrainRenderer_AudioPath.path_(newpath);
		^newpath;
	}

	*folderPath{
		^GrainRenderer_AudioPath.path;
	}

	*temporaryFolderPath_{|newpath|
		GrainRenderer_AudioPath.temporaryPath_(newpath);
	}

	*temporaryFolderPath{
		^GrainRenderer_AudioPath.temporaryPath;
	}

	*mode_{|newMode|
		if(newMode!=\mono && (newMode!=\stereo) && (newMode!=\stereoBalance)){
			Error("Unrecognized mode. Try \mono or \stereo or \stereoBalance").throw;
		};

		mode = newMode;
	}

	/*	*switchMode{
	if(mode==\mono){
	this.mode_(\stereo);
	format("GrainRenderer switched to \stereo").warn;
	};

	if(mode==\stereo){
	this.mode_(\mono);
	format("GrainRenderer switched to \mono o").warn;
	};
	}*/
}


GrainRenderer_AudioPath : FileConfigurer{
	classvar internalPath, date;
	classvar temporaryPathSet = false;

	*defaultPath{
		^("~/Desktop/audio/grain-renders".standardizePath);
	}

	*dailyPath{
		var path = this.path;

		path = path +/+this.dayString;
		^path;
	}

	*dayString{
		date = Date.getDate;
		^(date.month.asString++"-"++date.day.asString);
	}

	*path_{|newpath|

		if(temporaryPathSet){
			temporaryPathSet = false;
			internalPath = nil;
		};

		^super.path_(newpath);

	}

	*temporaryPath_{ |newpath|

		if(newpath!=nil){
			internalPath = newpath;
			temporaryPathSet = true;
		}/*ELSE*/{
			if(temporaryPathSet){
				temporaryPathSet = false;
				internalPath = this.path;
			};
		}

	}

	*temporaryPath{

		if(temporaryPathSet){
			^internalPath;
		}/*ELSE*/{
			^nil;
		};

	}


	*path{
		if(internalPath.isNil){
			internalPath = super.path;
		};

		^internalPath;
	}

}