SynthDef.new(\synthDef, {
	OffsetOut.ar(\out.kr(0), Pan2.ar(SinOsc.ar(\freq.kr(400)) * EnvGen.ar(
		Env.perc, 
		timeScale: \timescale.kr(1), 
		doneAction: Done.freeSelf, 
		levelScale: \ampDB.kr(-12).dbamp
	), \pan.kr(0)));
});
