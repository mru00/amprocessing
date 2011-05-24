#! /bin/bash

#./find_max.sh > max.onsets.paramstudy.eval

{
awk -f - max.onsets.paramstudy.eval <<"EOF"

# http://www.unix.com/shell-programming-scripting/98299-awk-median.html#post302280563

function median(v,j) { 
  c = asort(v,j); 
  if (c % 2) return j[(c+1)/2]; 
  else return (j[c/2+1]+j[c/2])/2.0; 
} 

function def(a) {
  if (!a) return "-"
  return a
}

BEGIN {
}

/train...onsets.paramstudy...eval/ { 

  file = substr($0, 6,2);
  alg = substr($0, 27,1);
  fmeas = $11

  m = def($2)
  w = def($3)
  delta = def($4)
  alpha = def($5)

  if (!fmeas) { fmeas = "-" }

  a_m[alg] = a_m[alg] " " m
  a_w[alg] = a_w[alg] " " w
  a_d[alg] = a_d[alg] " " sprintf("%.3lf", delta)
  a_a[alg] = a_a[alg] " " sprintf("%.3lf", alpha)
}

END {
  for(i=1; i<=7; i++){
    split(substr(a_m[i], 1), a2_m, " ")
    split(substr(a_w[i], 1), a2_w, " ")
    split(substr(a_a[i], 1), a2_a, " ")
    split(substr(a_d[i], 1), a2_d, " ")

    print  ""
    print "algorithm: " i
    print a_m[i] " -> " median(a2_m) 
    print a_w[i] " -> " median(a2_w) 
    print a_a[i] " -> " median(a2_a) 
    print a_d[i] " -> " median(a2_d)

  }
}

EOF
}  #> grouped_by_alg.eval


