#! /bin/bash -e

:> eval.txt

for i in *paramstudy*; do 

  #continue

  params=$(echo -n $i | sed -n -e 's/train.._paramstudy_\([0-9]\+\)_\([0-9]\+\)_\(-\?[0-9]\+\)_\(-\?[0-9]\+\)_.onsets.eval/\1 \2 \3 \4/p')
  fm=$(sed -n -e 's/F-Measure: \(.*\)/\1/p' $i)
  fn=$(echo -n $i | sed -n -e 's/\(train..\).*/\1/p')


  echo $fn $params $fm >> eval.txt

done

exit 

gnuplot -p <<"EOF"

plot  \
  "eval.txt" u 5 t "delta" w l, \
  "eval.txt" u 4 t "alpha" w l, \
  "eval.txt" u 3 t "w" w l, \
  "eval.txt" u 2 t "m" w l, \
  "eval.txt" u (10000*$6) t "fmeasure"

EOF
