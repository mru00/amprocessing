#!/bin/bash -xe

#./find_max.sh > max.onsets.paramstudy.eval


{
awk -f - max.onsets.paramstudy.eval <<"EOF"

/train...onsets.paramstudy...eval/ { 

file = substr($0, 6,2);
alg = substr($0, 27,1);
fmeas = $11
if (!fmeas) { fmeas = "-" }

#print file " " alg " " fmeas

if (alg in data) {
  data[alg] = data[alg] " "
}
data[alg] = data[alg] fmeas
}

END {

for(i=1; i<=7; i++){

  print i " " data[i]
}

}

EOF
}  > grouped_by_alg_autotune.eval


{

  gnuplot -p <<"EOF"
set terminal png size 1024,768
set title "F-Measure for every file grouped by algorithm, autotuned parameters"
set style data histogram
set style histogram clustered gap 5
set style fill solid border -1
set datafile missing "-"
plot for [i=2:21] "grouped_by_alg_autotune.eval" u i t ""

EOF

} > eval_grouped_by_algorithm_autotune.png

