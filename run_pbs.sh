#! /bin/bash -e

# mru, 2011-05

ant

qsub -t 1:20 -d /home/mru/onset_detection /home/mru/onset_detection/run_pbs_single.sh


