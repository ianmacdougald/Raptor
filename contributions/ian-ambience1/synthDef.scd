~tri = SynthDef(\tri, {
	// Declare variables
	var freq, timescale;
	var lo, hi, ffEnv, env;
	var sig;

	// Define (most of the) arguments
	// ...and envelopes
	freq = \freq.kr(400);
	timescale = \timescale.kr(1);

	lo = \ffRLo.kr(1.0);
	hi = \ffRHi.kr(8.0);

	ffEnv = Env.perc(
		\ffAtk.kr(0),
		\ffRelease.kr(1),
		1,
		\ffCurve.kr(-4)
	).ar(Done.none, 1, timescale);

	ffEnv = (lo + (ffEnv * (hi - lo))).abs;

	env = Env.perc(
		\atk.kr(0),
		\release.kr(1),
		1,
		\curve.kr(-4)
	).ar(Done.freeSelf, 1, timescale);

	// Establish the signal chain
	sig = LFTri.ar(freq * [ 1, 0.5, 0.25, Rand(1.0, 5.0).round(1.0) ] * LFTri.kr(ExpRand(1, 8))
		.unipolar(0.2).midiratio,
		mul: [ 2, 1, 0.5, 0.3 ].normalizeSum);

	sig = Splay.ar(sig, Rand(0.0, 1.0));
	sig = sig + PinkNoise.ar(\noiseamp.kr(0.2));
	sig = sig.sin;
	sig = LeakDC.ar(sig.flat);

	sig = RLPF.ar(
		sig,
		(freq * ffEnv).clip(20.0, SampleRate.ir * 0.45),
		1 - \q.kr(0.5).clip(0.0, 1.0)
	);

	sig = sig * env * \amp.kr(-12.dbamp);

	OffsetOut.ar(
		\out.kr(0),
		Pan2.ar(sig, \pan.kr(0));
	);
});

~sineDist = SynthDef(\sineDistortion, {
	var bus = \bus.kr(0);
	var in = In.ar([ bus, bus + 1 ]);
	in = in * LFNoise2.kr(LFNoise1.kr(0.05).exprange(0.1, 0.5)).exprange(3.0, 7.0);
	ReplaceOut.ar(
		bus,
		LeakDC.ar(in).sin * \amp.kr(-3.dbamp);
	);
});

~tanhDist = SynthDef(\tanhDist, {
	var bus = \bus.kr(0);
	var in = In.ar([ bus, bus + 1 ]);
	in = in * (SinOsc.kr(0.025).unipolar(SinOsc.kr(0.03125).unipolar(0.3) + 0.2) + 1.0);
	in = in.tanh;
	ReplaceOut.ar(
		bus,
		in
	);
});

~filtDelay = SynthDef(\filtDelay, {
	var bus = \bus.kr(0);
	var in = In.ar([ bus, bus + 1 ]);
	var sig = RLPF.ar(
		in * 0.64,
		DelayN.kr(
			SinOsc.kr(0.05).exprange(1600, 7200),
			0.1,
			[ 0, 0.1 ]
		),
		0.2
	);

	ReplaceOut.ar(
		bus,
		sig
	);
});

~balance = SynthDef(\balance, {
	var bus = \bus.kr(0);
	var in = In.ar([ bus, bus + 1 ]);
	var cin = in;
	var local = LocalIn.ar(2);

	var sig = Balance2.ar(
		in[0], in[1],
		LFNoise2.kr(0.05).bipolar
	);

	local = DelayC.ar(
		local + sig,
		4.0 + ControlDur.ir,
		4.0 - ControlDur.ir
	);

	LocalOut.ar(local * LFNoise2.kr(0.1).exprange(0.5, 0.9));

	ReplaceOut.ar(
		bus,
		in.blend(local, LFNoise2.kr(0.1.rand).unipolar(0.8) + 0.2 * Line.kr(dur:16))
	);
});

// I copied this from one of Eli Fieldsteel's tutorials or videos or something.
// I use this all the time. It's a beautiful reverb.
~reverb = SynthDef(\reverb, {
	var bus = \bus.kr(0);
	var in = In.ar([ bus, bus + 1 ]);
	var ffreq = LFNoise2.kr(0.03).exprange(900, 3000);
	var decay = LFNoise2.kr(0.04).exprange(2.0, 16.0);
	var sig = LPF.ar(in, ffreq);
	sig = DelayN.ar(sig, 0.3, (0.3.rand + 0.01).clip(0.0, 0.2999));
	sig = CombN.ar(sig, 0.1, ({ Rand(0.01, 0.099) }!32), decay);
	sig = Splay.ar(sig);
	sig = LeakDC.ar(sig);
	5.do {
		sig = AllpassN.ar(sig, 0.1, ({ Rand(0.01, 0.099) }!2), decay * 3 / 4);
	};
	sig = LPF.ar(sig, ffreq);
	ReplaceOut.ar(
		bus,
		in.blend(sig, \wet.kr(0.5)) * \amp.kr(1);
	);
});