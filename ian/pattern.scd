//Define Function below...
{ | modules, duration(1) |
	var tendtime = nil;
	var tenddur = nil;
	var tendstrum = nil;
	var tendpos = nil;
	Pproto({
		//Collect folder of mono audio files into buffers
		~bufArray = "kitchen-grains".ianAudioPath.getPaths
		.collect({|item|
			(type: \allocRead, path: item).yield;
		}).flat;

		//Add a function to to the clean up module (a list).
	        modules.cleanup.add({~bufArray.do(_.free)});

	}, Pbind(
		\instrument, modules.synthDef.name,
		\type, \note,
		\bufnum, Pn(Plazy({ | ev |
			var size = ev.bufArray.size;
			var pat = Pbrown(0.0, size - 1, 1.0, inf);
			pat;
		}), inf),
		\buf, Pfunc({ | ev |
			ev.bufArray[ ev.bufnum.asInteger ];
		}), 
		\pos, Pn(Plazy({ | ev |
			var start = tendpos ?? {1.0.rand};
			var end = 1.0.rand;
			var time = exprand(1e-4, 8.0);
			tendpos = end;
			Pn(Pseg([start, end]*0.5, Pn(time, inf), \exp), 1)
		}), inf),
		\dur, Pn(Plazy({ | ev |
			var lo = 1/256, hi = 1;
			var start = tendtime ?? {exprand(lo, hi)};
			var end = exprand(lo, hi);
			var time = (start + end) * exprand(2.0, 20);
			tendtime = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf),
		\ampDB, (6.dbamp * [3, 2, 1, 1, 0.5].squared.normalizeSum
			* Pn(Pseg([0, 1, 1, 0], Pseq([0.05, 1, 0.05].normalizeSum * duration, inf)), 1)
		).ampdb,
		\out, 0,
		\rate, [0.2, 0.4, 2.0, 4.0, 9.0] * exprand(0.5, 1.25),
		\timescale, Pkey(\dur) * Pn(Plazy({ | ev |
			var start = tendtime ?? {exprand(0.125, 2.0)};
			var end = exprand(0.125, 2.0);
			var time = (start + end) * exprand(1e-4, 8.0);
			tendtime = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf) * [1, 0.5, 0.25, 0.125, 0.125] * 4,
		\strum, Pn(Plazy({ | ev |
			var start = tendstrum ?? {rrand(0, 1e-2)};
			var end = rrand(0, 1e-2);
			var time = (start + end) + 1e-4 * exprand(2.0, 8.0);
			tendstrum = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf),
		\pan, Pwhite(-1.0, 1.0, inf)
	));

};
