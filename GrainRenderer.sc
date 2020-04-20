GrainRenderer{
	classvar <mode = \mono;
	classvar server;
	classvar folderVar;
	classvar <fileIncrement;
	classvar <isRendering = false;

	*pr_CollectFilePaths{
		|pathToFolder|

		var pathname = PathName(pathToFolder);
		var filepaths = [];

		if(pathname.folders.isEmpty.not){
			pathname.folders.do{|folder|
				filepaths = filepaths++this.pr_CollectFilePaths(folder.fullPath);
			};
		};

		pathname.files.do{|file|
			filepaths = filepaths.add(file.fullPath.asString);
		};

		^filepaths;
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
			var sig = BufRd.ar(2, buf, phasor);
			var out = sig * env * \ampDB.kr(-12).dbamp;
			OffsetOut.ar(\out.kr(0), Balance2.ar(out[0], out[1], \pan.kr(0)));

		}).store;
	}


	*render{|audioFilesToRender, duration = 20|
		var s = this.pr_CheckServer;

		if(isRendering.not){
			isRendering = true;

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

					}), inf),
					\ampDB, (-9.dbamp * [3, 2, 1, 1, 0.5].squared.normalizeSum).ampdb,
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

				fork{
					var path = this.pr_GetFileName;

					p.render(
						path,
						duration, 48e3, "wav", "int32",
						options: s.options.copy.verbosity_(-2),
						action: {
							fork{
								var buf = Buffer.read(s, path);
								s.sync;
								buf.normalize(0.8);
								s.sync;
								buf.write(path);
								s.sync;
								buf.free;
								s.sync;
								format("% rendered!\n",
									PathName(path).fileNameWithoutExtension)
								.postln;
								isRendering = false;
								nil;
							};
					});
				};
			};
		};
	}

	*renderN{|n = 1, audioFolder, duration = 20|

		Routine({

			n.do{

				this.render(audioFolder, duration.value);

				while({isRendering}, {1e-3.wait});

			};

		}).play;

	}

	*pr_GetFileName{
		var origfile = "grain-render_%.wav";
		var path = this.folder, filename;

		if(fileIncrement.isNil){
			var f_inc = 0;
			filename = format(origfile, f_inc);

			if(File.exists(filename)){

				while({File.exists(filename)}, {
					f_inc = f_inc + 1;

					filename = format(origfile, f_inc);
					filename = path +/+ filename;

				});

			}/*ELSE*/{
				filename = path +/+ filename;
			};

			fileIncrement = f_inc ? 0;

		}/*ELSE*/{
			fileIncrement = fileIncrement + 1;

			filename = format(origfile, fileIncrement);
			filename = path +/+ filename;

			if(File.exists(filename)){
				this.pr_GetFileName;
			};
		};

		^filename;
	}

	*pr_ResetFileIncrement{
		fileIncrement = nil;
	}

	*folder{
		if(folderVar.isNil or: {folderVar != GrainRenderer_AudioPath.dailyPath}){
			this.pr_MakeFolder;
		};

		^folderVar;
	}

	*pr_MakeFolder{
		folderVar = GrainRenderer_AudioPath.dailyPath;

		if(File.exists(folderVar).not){
			File.mkdir(folderVar);
		};
	}

	*folderPath_{|newpath|
		GrainRenderer_AudioPath.path_(newpath);
		^newpath;
	}

	*mode_{|newMode|
		if(newMode!=\mono and: {newMode!=\stereo}){
			Error("Unrecognized mode. Try \mono or \stereo").throw;
		};

		mode = newMode;
	}
}


GrainRenderer_AudioPath : FileConfigurer{
	classvar internalPath, date;

	*defaultPath{
		^("~/Desktop/audio/grain-renders".standardizePath);
	}

	*dailyPath{
		var path = this.path;

		date = Date.getDate;

		path = path +/+ date.month++"-"++date.day;
		^path;
	}

	*path{
		if(internalPath.isNil){
			internalPath = super.path;
		};
		^internalPath;
	}
}