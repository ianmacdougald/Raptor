SynthDef.new(\synthDef, {
	OffsetOut.ar(
		\out.kr(0),
		Pan2.ar(
			SinOsc.ar(\freq.kr(400)),
			\pan.kr(0),
			Env.perc.ar(
				timeScale: \timescale.kr(1),
				levelScale: \ampDB.kr(-12).dbamp,
				doneAction: Done.freeSelf
			)
		)
	);
});
