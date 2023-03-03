{ | modules |
	var pat;
	modules.use {
		pat = Pbind(
			\instrument, Pkey(\tri),
			\root, 6,
			\degree, Pseq([ 0, 0, 0, 3, -2, -2, 0, 4, -4, -7, -7 ], inf),
			\degree, Pkey(\degree) + [ 0, 0.005, 0.01, 3, 5, 9 ],
			\dur, Pseq([ 4, 2, 2, 1, 1, 1, 1, 4, 4, 4 ], inf) * Pwhite(1.0, 2.0, inf) * 2,
			\timescale, Pkey(\dur) * Pwhite(1.0, 4.0, inf),
			\atk, Pwhite(0.5, 0.9, inf),
			\release, 0.2,
			\ffRLo, [ 1.8005, 1.5, 1.25, 1.125, 1.125 ] * Pwhite(0.9, 1.1, inf),
			\ffRHi, Pwhite(0.0, 2.0, inf) + [ 1.0, 0.5, 0.3, 0.3, 0.2 ],
			\ffAtk, 0.5,
			\ffRelease, Pwhite(0.0, 1.0, inf) + 0.1,
			\q, Pexprand(0.05, 0.84, inf),
			\amp, [ 3, 3, 3, 1, 1, 1 ].normalizeSum * 0.dbamp,
			\noiseamp, [ 0.2, 0.1, 0.1, 0.1, 0.1 ],
			// \mtranspose, Pseq([ [ -7, -7, -7, 0, 0, 0 ], 3, 0, -2, [ -7, -7, -7, 0, 0, 0 ] ].rotate(0), 1).stutter(16),
			\mtranspose, Pseq([ 4, 4, -3, -3, 0, -2, -2 ], inf).stutter(8),
			\octave, [ 4, 4, 4, 5, 5, 5, 3, 3, 3 ] - 1,
			\vibrate, Pexprand(0.05, 0.75, inf),
			\vibdepth, Pexprand(1.0, 1.5, inf) - 1.0,
		);

		//Create the FX chain
		pat = Pfx(pat, ~filtDelay.name);
		pat = Pfx(pat, ~tanhDist.name);
		pat = Pfx(pat, ~balance.name);
		pat = Pfx(pat, ~reverb.name);
		pat = Pfx(pat, ~sineDist.name, \amp, 0.dbamp);
	};
	pat;
};
