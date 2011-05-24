#!/bin/bash -e


{

  for file in train??.onsets.fixedparam.eval; do
awk -v fn=$file -f - $file <<"EOF"
    function join(array, start, end, sep,    result, i)
     {
         if (sep == "")
            sep = " "
         else if (sep == SUBSEP) # magic value
            sep = ""
         result = array[start]
         for (i = start + 1; i <= end; i++)
             result = result sep array[i]
         return result
     }

BEGIN {
  file = substr(fn, 6,2);
}

NR > 1 { 
  data = data " " $7
}

END {
print file " " substr(data, 1)
}
EOF
done

}  > grouped_by_file.eval


{

  gnuplot -p <<"EOF"
set terminal png size 1024,768
set style data histogram
set title "F-Measure for every algorithm grouped by file, fixed parameters"
set style histogram clustered gap 5
set style fill solid border -1
set datafile missing "-"


plot for [i=2:8] "grouped_by_file.eval" u i t ""

EOF

} > fixedparam_by_file.png
