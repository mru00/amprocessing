#! /bin/bash -e

# mru, 2011-05
#
# prints the parameter set with the highest f-score

for i in train*.onsets.paramstudy.eval; do 
awk -f - $i <<"EOF"

BEGIN { max = 0.0; }
NR > 1 { if ($10 > max && $10 != "NaN") {max = $10; field = $0;}; }
END { print FILENAME ": " field; }
EOF

done
