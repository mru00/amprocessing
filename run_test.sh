#! /bin/bash -ex

export CLASSPATH=build/classes:build/classes/jopt-simple.jar
WAVS=data/*.wav

ant

for f in data/*.wav; do
  mainclass=at.cp.jku.teaching.amprocessing.EvalRunner
  basename=$(basename $f .wav)
  java $mainclass -i $f -g data/$basename.onsets -t data/$basename.bpms -o output -p output/$basename.odf -q
  cat output/$basename.onsets.eval
  (
  gnuplot <<EOF
  set terminal png size 800,600
  plot "output/$basename.odf" using 1:2 with lines, "output/$basename.onsets" using (\$1):(1)
EOF
) > output/$basename.odf.png
done

onsets=output/onsets.all
tempii=output/tempo.all

for f in output/*.onsets.eval; do
  echo -n $f ' '
  awk -f join.awk --source '{arr[++i]=$2} END{print join(arr)}' $f
done > $onsets

(

gnuplot <<EOF
set terminal png size 800,600
set style data histogram
set style histogram gap 2
set xrange [-1:21]
set style fill solid border -1
#set boxwidth 0.4
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
set terminal png size 800,600
plot "$tempii" using 2 title 'P', "$tempii" using 3 title 'R'
EOF
) > output/tempo.png
