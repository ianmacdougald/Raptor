//Define Function below...

{arg ev, duration(1);

	var tendtime = nil;
	var tenddur = nil;
	var tendstrum = nil;
	var tendpos = nil;
	Pproto({

		~bufArray = "russell-files".ianAudioPath.getPaths
		.collect({|item|
			(type: \allocRead, path: item).yield;
		}).flat;

	}, Pbind(
		\instrument, \grainToRender,
		\type, \note,
		\bufnum, Pn(Plazy({|p_ev|
			var size = p_ev.bufArray.size;
			var pat = Pbrown(0.0, size - 1, 1.0, inf);
			pat;
		}), inf),
		\buf, Pfunc({|p_ev|
			p_ev.bufArray[p_ev.bufnum];
		}),
		\pos,Pn(Plazy({|p_ev|
			var start = tendpos ?? {1.0.rand};
			var end = 1.0.rand;
			var time = exprand(1e-4, 8.0);
			tendpos = end;
			Pn(Pseg([start, end]*0.5, Pn(time, inf), \exp), 1)
		}), inf),
		\dur, Pn(Plazy({|p_ev|
			var start = tendtime ?? {exprand(1e-3, 1.0)};
			var end = exprand(1e-3, 1.0);
			var time = (start + end) * exprand(2.0, 20);
			tendtime = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf),
		\ampDB, (6.dbamp * [3, 2, 1, 1, 0.5].squared.normalizeSum
			* Pn(Pseg([0, 1, 1, 0], [0.05, 1, 0.05].normalizeSum * duration), 1)
		).ampdb,
		\out, 0,
		\rate, [0.2, 0.4, 2.0, 4.0, 9.0] * exprand(0.5, 1.25),
		\timescale, Pkey(\dur) * Pn(Plazy({|p_ev|
			var start = tendtime ?? {exprand(0.125, 2.0)};
			var end = exprand(0.125, 2.0);
			var time = (start + end) * exprand(1e-4, 8.0);
			tendtime = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf) * [1, 0.5, 0.25, 0.125, 0.125] * 4,
		\strum, Pn(Plazy({|p_ev|
			var start = tendstrum ?? {rrand(0, 1e-2)};
			var end = rrand(0, 1e-2);
			var time = (start + end) + 1e-4 * exprand(2.0, 8.0);
			tendstrum = end;
			Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
		}), inf),
		\pan, Pwhite(-1.0, 1.0, inf)
	));

};
