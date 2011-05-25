#! /bin/bash -e

# mru, 2011-05

ant

BD=~/onset_detection

qsub -t 1-20 -d $BD $BD/run_pbs_single.sh


