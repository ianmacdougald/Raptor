GrainCollector : GrainRenderer{

/*	classvar bufferFolderPath;
	classvar buffers;

	*loadBuffers{ |pathToFolder|

		var s = this.pr_CheckServer;

		if(File.exists(pathToFolder).not){
			Error("Path does not exist").throw;
		};

		buffers !? {
			buffers.do(_.free);
		};

		buffers = this.pr_CollectFilePaths(pathToFolder);
		buffers = buffers.collect({|item|
			Buffer.read(s, item);
		});

	}

	*defaultBufferPath{
		^GrainRenderer_AudioPath.path;
	}

	*render{this.shouldNotImplement(thisMethod)}

	*pr_CallSynth{

		SynthDef.new(\GrainCollector_PlayBuf, {
			var buf = \buf.kr(0);
			var rate = \rate.kr(1.0);
			var dur = BufDur.kr(buf) / rate;
			var env = EnvGen.ar(Env([0, 1, 1, 1, 0], [0.1, 1, 1, 0.1].normalizeSum),
				timeScale: dur,
				doneAction: Done.freeSelf
			);
			var sig = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate);
			var out = sig * env * \ampDB.kr(-12).dbamp;
			Out.ar(\out.kr(0), Balance2.ar(out[0], out[1], \pan.kr(0)))
		}).store;

		SynthDef.new(\GrainCollector_DetectSilence, {
			var in = In.ar(\in.kr(0), 2);
			DetectSilence.ar(in, \amp.kr(0.0001), \time.kr(0.1), doneAction: Done.freeSelf);
			Silent.ar;
		}).store;

	}

	*pr_CheckServer{
		var return = super.pr_CheckServer;
		this.pr_LoadSynthDef;
		^return;
	}*/

	//
	// *playCollection{||
	//
	//
	// 	if(pathToFolder!=bufferFolderPath){
	//
	// 		pathToFolder = pathToFolder ? this.defaultBufferPath;
	//
	// 		this.loadBuffers(pathToFolder);
	//
	// 	};
	//
	//
	//
	// }
	//
	// *recordCollection{ |path, loops = 24, waitFunction = 0.001, metaLoops = 2,
	// 	metaWaitFunction = 0.1, rateFunction = 1, bufFunction = 0,
	// 	panFunction = 0, ampDBFunction = -12, pathToFolder, out = 0, target, addAction|
	//
	// 	var server = this.pr_CheckServer;
	// 	var group = Group.new(target, addAction);
	// 	var silenceSynth;
	//
	// 	path = path ?? {this.pr_GetFileName};
	//
	// 	^forkIfNeeded{
	//
	// 		server.record(path);
	// 		server.sync;
	//
	// 		silenceSynth = Synth(\GrainCollector_DetectSilence, [
	// 			\in, out
	// 		]).onFree({
	// 			server.stopRecording;
	// 		});
	//
	// 		server.sync;
	//
	// 		this.playCollection(
	// 			loops,
	// 			waitFunction,
	// 			metaLoops,
	// 			metaWaitFunction,
	// 			rateFunction,
	// 			bufFunction,
	// 			panFunction,
	// 			ampDBFunction,
	// 			pathToFolder,
	// 			out,
	// 			group,
	// 			nil
	// 		);
	//
	// 	};
	//
	// }
	//
	//
	// *renderN{
	// 	this.shouldNotImplement(thisMethod);
	// }

}

GrainCollector_AudioPath : GrainRenderer_AudioPath{

	/**defaultPath{
		^("~/Desktop/audio/grain-collections".standardizePath);
	}
*/
}