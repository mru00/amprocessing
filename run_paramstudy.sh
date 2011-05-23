#! /bin/bash -e

export CLASSPATH=build/classes:build/classes/jopt-simple.jar
WAVS=data/*.wav

ant

NUM=0
QUEUE=""
MAX_NPROC=4 # default

function queue {
	QUEUE="$QUEUE $1"
	NUM=$(($NUM+1))
}

function regeneratequeue {
	OLDREQUEUE=$QUEUE
	QUEUE=""
	NUM=0
	for PID in $OLDREQUEUE
	do
		if [ -d /proc/$PID  ] ; then
			QUEUE="$QUEUE $PID"
			NUM=$(($NUM+1))
		fi
	done
}

function checkqueue {
	OLDCHQUEUE=$QUEUE
	for PID in $OLDCHQUEUE
	do
		if [ ! -d /proc/$PID ] ; then
			regeneratequeue # at least one PID has finished
			break
		fi
	done
}


trap 'for i in $QUEUE; do kill $i; done;' EXIT


for f in data/*.wav; do
  mainclass=at.cp.jku.teaching.amprocessing.EvalRunner
  basename=$(basename $f .wav)
  java $mainclass -i $f -g data/$basename.onsets -t data/$basename.bpms -o output -p output/$basename.odf -q &
  	PID=$!
	queue $PID

	while [ $NUM -ge $MAX_NPROC ]; do
		checkqueue
		sleep 2
	done
done

wait

