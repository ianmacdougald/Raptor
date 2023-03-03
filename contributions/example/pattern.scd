//Define an Event pattern below:
Pbind(
	\instrument, Pkey(\synthDef),
	\dur, 0.2,
	\degree, Pxrand([0, 2, 3, 4, 5, 7, 9, 11, 14], 64),
	\ampDB, Prand([-12, -9, -18, -18], inf),
	\timescale, Pkey(\dur) * Pseq([4, Pn(0.2, 3)], inf),
	\pan, Pseq([0, Pwhite(-1.0, 1.0, 3)], inf),
);
