#! /bin/bash

#./find_max.sh > max.onsets.paramstudy.eval

{
awk -f - max.onsets.paramstudy.eval <<"EOF"

        function median(c,v,  j) { 
           asort(v,j); 
           if (c % 2) return j[(c+1)/2]; 
           else return (j[c/2+1]+j[c/2])/2.0; 
        } 

/train...onsets.paramstudy...eval/ { 

  file = substr($0, 6,2);
  alg = substr($0, 27,1);
  fmeas = $11
  if (!fmeas) { fmeas = "-" }

  if (alg in data) {
    data[alg] = data[alg] "\n"
  }
  data[alg] = data[alg] alg " " file " " $0
}

END {
  for(i=1; i<=7; i++){
    print data[i]
  }
}

EOF
}  > grouped_by_alg.eval


