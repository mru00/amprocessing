#! /bin/bash -ex

export CLASSPATH=build/classes:build/classes/jopt-simple.jar
WAVS=data/*.wav

ant

for f in data/*.wav; do
  mainclass=at.cp.jku.teaching.amprocessing.Runner
  basename=data/$(basename $f .wav)
  java $mainclass -i $f -g $basename.onsets -t $basename.bpms -o output
  cat output/$(basename $basename).onsets.eval
done

onsets=output/onsets.all
tempii=output/tempo.all

for f in output/*.onsets.eval; do
  echo -n $f ' '
  awk -f join.awk --source '{arr[++i]=$2} END{print join(arr)}' $f
done > $onsets

(

gnuplot <<EOF
set terminal png
set style data boxes
set boxwidth 0.4
plot "$onsets" using 5 title 'P', "$onsets" using 6 title 'R', "$onsets" using 7 title 'F'
EOF
) > output/onsets.png

for f in output/*.tempo.eval; do
  echo -n $f ' '
  cat $f
  echo
done > $tempii

(
gnuplot <<EOF
set terminal png
plot "$tempii" using 2 title 'P', "$tempii" using 3 title 'R'
EOF
) > output/tempo.png
