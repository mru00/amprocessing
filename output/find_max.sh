#! /bin/bash -e


for i in train*.onsets.paramstudy.eval; do 
awk -f - $i <<"EOF"

BEGIN { max = 0.0; }
{ if ($10 > max && $10 != "NaN" && $10 != "FMEASURE") {max = $10; field = $0;}; }
END { print FILENAME ": " field; }
EOF

done
