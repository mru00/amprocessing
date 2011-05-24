#! /bin/bash -e

# mru, 2011-05

ant

BD=/media/co-resident/data/onset_detection
<<<<<<< HEAD
=======

>>>>>>> cfe39894d4875b4e581faf647d11e680bc1d0eb8
qsub -t 1-20 -d $BD $BD/run_pbs_single.sh


