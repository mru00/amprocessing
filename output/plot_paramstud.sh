#! /bin/bash


gnuplot -p <<"EOF"
set key autotitle columnhead

plot "train01.onsets.paramstudy.eval" using 1 w l, \
  for [i=2:7] "" u i w l, \
  "" u (100*$8) w l,\
  "" u (100*$9) w l,\
  "" u (100*$10) w l lw 3

EOF
