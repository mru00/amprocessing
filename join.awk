function join(a,start,end,sep,    result,i) {
    sep   = sep   ? sep   :  " "
    start = start ? start : 1
    end   = end   ? end   : sizeof(a)
    if (sep == SUBSEP) # magic value
       sep = ""
    result = a[start]
    for (i = start + 1; i <= end; i++)
        result = result sep a[i]
    return result
} 


function sizeof(a,   i,n) { for(i in a) n++ ; return n }
