//This class is used for automating the scripting of recording granular streams in non-real time via Prproto.
//The class handles dynamically configuring input sound files and outputting the results

GrainRenderer{
	classvar <mode = \mono;
	classvar server;
	classvar folderVar;
	classvar <isRendering = false;
	classvar renderRoutine;
	classvar <>maxBuffers = 64;
	classvar fileIncrementer;


	*render{|folderOfAudioFiles, duration = 20|
		//An interface for the method pr_ProcessAudio.
		//Here, we add the folder whose files we want to process
		//and we set a duration for the file we want to render.
		var filepaths = this.pr_CollectFilePaths(folderOfAudioFiles).postln;
		this.pr_ProcessAudio(filepaths, duration.value, {

			filepaths
			.collect({|item|
				(type: \allocRead, path: item).yield;
			})

		});
	}


	*renderN{|n = 1, folderOfAudioFiles, duration = 20|
		//If we want to render many functions at once,
		//we use this method, which returns a routine that renders N files
		if(renderRoutine.isNil){

			//We process the filepaths first so we don't
			//have to do it again and again and again.
			//That way, if we are pointing to dozens
			//of folders with hundred or thounsands of files each
			//We can do one intensive computation rather than
			//a thousand trillion bagillion of them
			var filepaths = this.pr_CollectFilePaths(folderOfAudioFiles);

			//Make sure that if we decide to stop
			//the routine with CmdPeriod, we can still render
			//more files without restarting the interpreter.
			CmdPeriod.doOnce({
				renderRoutine = nil;
			});

			//Here is the routie:
			renderRoutine = Routine({

				//process the audio n times
				n.do{

					this.pr_ProcessAudio(filepaths, duration.value, {

						var size, return = [];

						//Here we supply a different function for collecting buffers from the collection of audio files
						//Because we are doing this n times, it really slows things down when a collection of--let's say--1000 files
						//has to be loaded and offloaded and just reloaded again.
						//So fuck it: we'll just do less per N. The batch of rendered files will have a nice amount of diversity in it
						//but we won't have to repeat so many operations every time.
						//One can choose how many maximum buffers can be read into each render by modulating the maxBuffers field.
						if(filepaths.size > maxBuffers){
							var rand_number = rrand(2, maxBuffers);
							rand_number.do{
								return = return.add((type: \allocRead, path: filepaths.choose).yield);
							};
						}/*ELSE*/{
							return = filepaths
							.collect({|item|
								(type: \allocRead, path: item).yield;
							});
						};

						return;

					});
					while({isRendering}, {1e-4.wait});

				};

				renderRoutine = nil;

			}).play;
		}/*ELSE*/{
			if(renderRoutine.isPlaying){
				"Warning: rendering is already taking place. Stop before restarting".postln;
			};
		};

	}

	//If we are rendering (and don't want to use CmdPeriod), we can stop it
	*stopRender{
		if(renderRoutine.isNil.not){
			if(renderRoutine.isPlaying){
				renderRoutine.stop;
				renderRoutine = nil;
			}
		};
	}

	//We want the class to dynamically create folders for every day (a day representing a new session I guess).
	//We do this with a subclass of FileConfigurer, which stores and points to a filepath
	//and also makes new folders and tracks the date
	*folder{
		//If we have not stored today's folder, make the folder
		if(folderVar.isNil or: {folderVar != GrainRenderer_AudioPath.dailyPath}){
			this.pr_MakeFolder;
		};

		//Or if we have deleted the folder, re-make the folder
		if(File.exists(folderVar).not){
			this.pr_MakeFolder;
		};

		^folderVar;
	}

	//Here we set new modes which changes the SynthDefs the patterns call
	*mode_{|newMode|
		if(
			newMode!=\mono
			&& (newMode!=\stereo)
			&& (newMode!=\stereoBalance)
			&& (newMode!=\stereoNoPan)
			&& (newMode!=\monoNoPan)
			&& (newMode!=\stereoToMono)
			&& (newMode!=\monoToStereo)
		){
			Error("Unrecognized mode. Try \mono or \stereo or \stereoBalance").throw;
		};

		mode = newMode;
	}

	//sometimes if a render fails after the isRendering is set to true
	//it is useful to just reset it.
	*reset{
		isRendering = false;
		if(renderRoutine.isRunning){
			renderRoutine.stop;
		};

		if(renderRoutine.isNil.not){
			renderRoutine = nil;
		};
	}


	*pr_ErrorPathMsg{
		Error("Can only render from String, "
			++"PathName, or collections of these.").throw;
	}

	*pr_CollectFilePaths{|pathToFolder|
		var filepaths;

		//if the pathToFolder is a collection
		if(pathToFolder.isCollection and: {pathToFolder.isString.not}){
			//format it such that there are no embedded collections
			//(and also the strings don't flatten to individual characters)
			filepaths = pathToFolder.getPaths;
			// pathToFolder = this.pr_FormatPathCollections(pathToFolder);
		}/*ELSE*/{

			if(pathToFolder.isKindOf(Buffer)){
				filepaths = pathToFolder.path;
			}/*ELSE*/{
				filepaths = pathToFolder.getPaths;
			};

		};

		filepaths = filepaths.select({|item, index|
			var return = false;
			var tmpitem = PathName(item).extension.toLower;

			if(
				tmpitem=="wav" or: {
					tmpitem=="aif"
				} or: {
					tmpitem=="aiff"
				} or: {
					tmpitem=="mp3"
				} or: {
					tmpitem=="oog"
				}
			){

				return = true;

			};

			return;
		});

		^filepaths;
	}

	*pr_CheckServer{
		//make sure the server is booted, a reference is stored and the SynthDef Objects are stored.
		server = server ? Server.default;

		if(server.hasBooted.not){
			Error("Server has not booted!").throw;
		};

		this.pr_LoadSynthDefs;
		^server;
	}

	//Here we store synthdefs...storing means we write them to a file permanently
	//which I guess is less than ideal. But whatever. It's easier.
	*pr_LoadSynthDefs{
		//Make a grain!
		SynthDef.new(\GrainRenderer_GrainMonoNoPan, {
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
			OffsetOut.ar(\out.kr(0), out);
		}).store;

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

		//Do it in stereo
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

		//Do it in stereo but balance rather than pan the signal...
		//this means we are not summing a stereo signal down to mono
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

		SynthDef.new(\GrainRenderer_GrainStereoNoPan, {

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
			OffsetOut.ar(\out.kr(0), out);
		}).store;

		SynthDef.new(\GrainRenderer_GrainStereoToMono, {

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
			OffsetOut.ar(\out.kr(0), out);
		}).store;

		SynthDef.new(\GrainRenderer_GrainMonoToStereo, {

			var timescale = \timescale.kr(1);
			var env = EnvGen.ar(Env.sine(timescale),
				doneAction: Done.freeSelf
			);

			var buf = \buf.kr(0);
			var frames = BufFrames.kr(buf);
			var phasor = Phasor.ar(0, BufRateScale.kr(buf) * \rate.kr(1),
				0, SampleRate.ir * timescale
			) + (\pos.kr(0.0).clip(0.0, 1.0) * frames);
			var sig = BufRd.ar(1, buf, phasor) ! 2;
			var out = sig * env * \ampDB.kr(-12).dbamp;
			OffsetOut.ar(\out.kr(0), out);
		}).store;

		// this.pr_WindowSynthDef.store;

	}

	//this is the big ol' method where audio is processed. It encapsulates a Pproto object with the method .render called on it.
	*pr_ProcessAudio{|filepaths, duration = 20, protoFunc|

		//make sure that the synthdefs are stored on the server
		//There might be a way to do this without ever having booted the server
		//...there certainly is a way to do it in NRT synthesis generally (not just with Pproto)...
		//but I don't know how, so you are stuck booting the server. Boot it!
		var s = this.pr_CheckServer;

		if(s.hasBooted){

			if(isRendering.not){

				//Run a process or just add the current process to the parent one.
				forkIfNeeded{
					var defname;
					var tendtime = nil;
					var tenddur = nil;
					var tendstrum = nil;
					var tendpos = nil;
					var p;

					//Are we processing mono audio files or stereo audio files (with panning) or stereo audio files (with balancnig)?
					//This setting can be change by changing the mode of the class.
					switch(mode,
						\mono, {defname = \GrainRenderer_GrainMono},
						\stereo, {defname = \GrainRenderer_GrainStereo},
						\stereoBalance, {defname = \GrainRenderer_GrainStereoBalance},
						\monoNoPan, {defname = \GrainRenderer_GrainMonoNoPan},
						\stereoNoPan, {defname = \GrainRenderer_GrainStereoNoPan},
						\stereoToMono, {defname = \GrainRenderer_GrainMonoToStereo},
						\monoToStereo, {defname = \GrainRenderer_GrainStereoToMono},
					);

					//Tada!
					p = Pproto({

						~bufArray = protoFunc.value;

					},

					//A pattern
					Pbind(
						\instrument, defname,
						\type, \note,

						\out, 0,

						\bufnum, Pn(Plazy({|ev|

							var size = ev.bufArray.size;

							var pat = Pbrown(0.0, size - 1, 1.0, inf);

							pat;
						}), inf),

						\buf, Pfunc({|ev|
							ev.bufArray[ev.bufnum % ev.bufArray.size];
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
							var time = (start + end) * exprand(2.0, 10.0);

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

						\rate, [0.2, 0.4, 2.0, 4.0, 9.0] * exprand(0.5, 1.25),

						\timescale, Pkey(\dur) * Pn(Plazy({|ev|

							var start = tendtime ?? {exprand(1e-3, 2.0)};
							var end = exprand(1e-3, 2.0);
							var time = (start + end) * exprand(2.0, 8.0);

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

					//Waiting
					s.sync;
					0.005.wait;

					//A new process sort of I guess...this might be redundant
					forkIfNeeded{
						var path;

						fileIncrementer = fileIncrementer ??
						{FileIncrementer.new("grain-render.wav")};

						fileIncrementer.folder = this.folder;

						//Get a unique file name for what we are rendering in the target folder
						path = fileIncrementer.nextFileName;

						//Now rendering occurs so make sure we don't try to render while the class is rendering
						//This is useful for monitoring routines that are rendering N times.
						isRendering = true;

						p.render(
							path,
							duration, 48e3, "wav", "int32",
							options: s.options.copy.verbosity_(-2),
							action: {
								forkIfNeeded{
									//Once the file is written out re-write it with a standard normalization value
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
		};
	}

	//This is a simple method for outputting a standard message that a render
	//has finished succesfully and that we can render again
	*pr_RenderedMessage{
		|path|
		format("\n% rendered\n", PathName(path).fileNameWithoutExtension);
		isRendering = false;
	}

	*pr_MakeFolder{
		//get the path for the folder for today
		folderVar = GrainRenderer_AudioPath.dailyPath;

		//Make it
		if(File.exists(folderVar).not){
			File.mkdir(folderVar);
			//If it's a new folder
			this.pr_ResetIncrement;

		};
	}

	*pr_ResetIncrement{
		if(fileIncrementer.isNil.not){
			fileIncrementer.reset;
		}
	}

	//We can set a new folder path to write to
	//This writes out to a file and becomes permanent, persisting across Interpreter sessions
	*folderPath_{|newpath|
		GrainRenderer_AudioPath.path_(newpath);
		^newpath;
	}

	//Get the current path
	*folderPath{
		^GrainRenderer_AudioPath.path;
	}

	//Set a temporary path...this does not write out to a file
	//folderPath will now return this path until it is reset (set folder path to nil)
	//or the interpreter is restarted.
	*temporaryFolderPath_{|newpath|
		GrainRenderer_AudioPath.temporaryPath_(newpath);
	}

	//If there is a temporaryFolderPath set, get it. This will return nil if none is set.
	*temporaryFolderPath{
		^GrainRenderer_AudioPath.temporaryPath;
	}

}


GrainRenderer_AudioPath : FileConfigurer{
	classvar internalPath, date;
	classvar temporaryPathSet = false;

	//a default path to read and write should there not be one written and not one supplied...
	*defaultPath{
		^("~/Desktop/audio/grain-renders".standardizePath);
	}

	//Here we generate a path for the daily folder;
	*dailyPath{
		var path;

		while({path.isNil}, {
			path = this.path;
		});

		path = path +/+ this.dailyString;
		^path;
	}

	//Here we convert the month and day to a string we want
	*dailyString{
		date = Date.getDate;
		^(
			date.month.asString++"-"
			++date.day.asString++"-"
			++date.year.asString
		);
	}

	//writing a new path and remove any temporary paths set
	*path_{|newpath|

		if(temporaryPathSet){
			temporaryPathSet = false;
			internalPath = nil;
		};

		^super.path_(newpath);

	}

	//set a temporary path that does not write to a file
	*temporaryPath_{ |newpath|

		if(newpath!=nil){
			internalPath = newpath;
			temporaryPathSet = true;
		}/*ELSE*/{

			if(temporaryPathSet){
				temporaryPathSet = false;
				internalPath = nil;

				while({internalPath.isNil}, {
					internalPath = this.path;
				});

			};
		};

	}

	//return the temporary path if it is set.
	*temporaryPath{

		if(temporaryPathSet){
			^internalPath;
		}/*ELSE*/{
			^nil;
		};

	}

	//read a path. If it is not stored,
	//read it from the file and store it and then return it.
	*path{
		if(internalPath.isNil){
			while({internalPath.isNil}, {
				internalPath = super.path;
			});
		};

		^internalPath;
	}

}