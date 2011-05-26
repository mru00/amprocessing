#!/bin/bash -xe


{
awk -f - <(./find_best_parameters_by_algorithm.sh) <<"EOF"

/train...onsets.paramstudy....eval/ { 

  file = substr($0, 6,2);
  alg = substr($0, 27,2);
  fmeas = $11
  if (!fmeas) { fmeas = "-" }

  if (file in data) {
    data[file] = data[file] " "
  }
  data[file] = data[file] fmeas
}

END {
  for (i = 1; i <= 20; i ++ ) {
    ind = sprintf("%02d", i)
    print ind " " data[ind]
  }
}
EOF
}  > grouped_by_file.eval


{

  gnuplot -p <<"EOF"
set terminal png size 1024,768
set style data histogram
set title "F-Measure for every algorithm grouped by file, parameters autotuned per file"
set style histogram clustered gap 5
set style fill solid
set auto x
set boxwidth 0.9
set datafile missing "-"

plot for [i=2:11] "grouped_by_file.eval" u i t ""

EOF

} > eval_grouped_by_file.png
