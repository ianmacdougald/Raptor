// var maxBuffers = 256;
// var pathArray  = "russell-files".ianAudioPath.getAudioPaths;
//
// var selectArr = {
// 	if(pathArray.size > maxBuffers, {
// 		pathArray.select({|item, index| index < maxBuffers});
// 		}, {
// 			pathArray;
// 	});
// };
//
// var bufCollectYield  = { |arr|
// 	selectArr.value.collect({|item|
// 		(type: \allocRead, path: item).yield;
// 	});
// };
//
// {
// 	{|duration|
//
// 		var tendbuf;
// 		var tendtime;
// 		var tenddur;
// 		var tendstrum;
// 		var tendpos;
// 		var tendrate;
//
// 		Pproto({
// 			~bufArray = bufCollectYield.value;
// 			},
//
// 			//A pattern
// 			Pbind(
// 				\instrument, \renderGrains,
// 				\type, \note,
//
// 				\out, 0,
//
// 				\bufNum, Pn(Plazy({|ev|
// 					var start = tendbuf ?? {(ev.bufArray.size - 1).rand};
// 					var end = (ev.bufArray.size - 1).rand;
// 					var time = (start + end) * exprand(2.0, 10.0);
// 					tendtime = end;
// 					Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
// 				}), inf),
//
// 				\buf, Pfunc({|ev|
// 					ev.bufArray[ev.bufNum % ev.bufArray.size].bufNum
// 				}),
//
// 				\pos,Pn(Plazy({|ev|
// 					var start = tendpos ?? {1.0.rand};
// 					var end = 1.0.rand;
// 					var time = exprand(1e-4, 8.0);
// 					tendpos = end;
// 					Pn(Pseg([start, end]*0.5, Pn(time, inf), \exp), 1)
//
// 				}), inf),
//
// 				\dur, Pn(Plazy({|ev|
// 					var start = tendtime ?? {exprand(1e-3, 1.0)};
// 					var end = exprand(1e-3, 1.0);
// 					var time = (start + end) * exprand(2.0, 10.0);
// 					tendtime = end;
// 					Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
// 				}), inf) * 0.5,
//
// 				\windowAmp, Penv([0, 1, 1, 0], [0.01, 1, 0.01].normalizeSum * duration, \welch),
// 				\ampDB, -3,
//
// 				\rate, Pn(Plazy({|ev|
// 					var start = tendrate ?? {exprand(0.1, 4.0)};
// 					var end = exprand(0.1, 4.0);
// 					var time = exprand(1e-3, 2.0);
// 					tendrate = end;
// 					Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
// 				}), inf),
//
// 				\timescale, Pkey(\dur) * Pn(Plazy({|ev|
//
// 					var start = tendtime ?? {exprand(1e-3, 2.0)};
// 					var end = exprand(1e-3, 2.0);
// 					var time = (start + end) * exprand(2.0, 8.0);
//
// 					tendtime = end;
// 					Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
//
// 				}), inf) * [1, 0.5, 0.25, 0.125, 0.125] * 4,
//
// 				\strum, Pn(Plazy({|ev|
//
// 					var start = tendstrum ?? {rrand(0, 1e-2)};
// 					var end = rrand(0, 1e-2);
// 					var time = (start + end) + 1e-4 * exprand(2.0, 8.0);
//
// 					tendstrum = end;
// 					Pn(Pseg([start, end], Pn(time, inf), \exp), 1)
//
// 				}), inf),
//
// 				\pan, Pwhite(-1.0, 1.0, inf)
// 		));
// 	};
// };

var bufpath = "russell-files".ianAudioPath.getAudioPaths;asd
Pproto({
	~bufArray = bufpath.collect({|item|
		(type: \allocRead, path: item).yield;
	});
}, Pbind(
	\instrument, \renderPlayBuf,
	\dur, 0.2,
	\rate, Pexprand(0.1, 2.0, inf),
	\pan, Pwhite(-1.0, 1.0),
	\ampDB, Prand([-12, 3], inf)
))