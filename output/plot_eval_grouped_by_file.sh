#!/bin/bash -xe

./find_max.sh > max.onsets.paramstudy.eval


{
awk -f - max.onsets.paramstudy.eval <<"EOF"

/train...onsets.paramstudy...eval/ { 

file = substr($0, 6,2);
alg = substr($0, 27,1);
fmeas = $11

print file " " alg " " fmeas
}

EOF
}  > grouped.eval


{

  gnuplot -p <<"EOF"

set style data histogram

plot "grouped.eval" u 2, "" u 1, "" u 3

EOF

}
