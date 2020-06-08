SynthDef.new(\synthDef, {
	var timescale = \timescale.kr(1);
	var env = EnvGen.ar(
		Env.sine(timescale),
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
});
