//make synthdef
[
	SynthDef.new(\grainToRender, {
		var timescale = \timescale.kr(1);
		var curveRatio = \curveRatio.kr(0.5) / 2;
		var env = EnvGen.ar(
			Env([0, 1, 1, 0], [curveRatio, 1, curveRatio].normalizeSum, \sine),
			doneAction: Done.freeSelf
		);
		var buf = \buf.kr(0);
		var frames = BufFrames.kr(buf);
		var phasor = Phasor.ar(0, BufRateScale.kr(buf) * \rate.kr(1),
			0, (SampleRate.ir * timescale)
		) + (\pos.kr(0.5)*frames);
		var sig = BufRd.ar(1, buf, phasor.wrap(0, frames));
		var out = sig * env * \ampDB.kr(-12).dbamp;
		out = HPF.ar(out, \ffreq.kr(1500));
		OffsetOut.ar(\out.kr(0),  Pan2.ar(out, \pan.kr(0)))
	}),

	SynthDef(\reverb, {
		XOut.ar(\out.kr(0), \xfade.kr(0.2),
			FieldsteelReverb.ar(
				In.ar(\in.kr(0), 2).sum / 2,
				\ffreq.kr(1500), \decay.kr(4)
			)
		);
	})

];