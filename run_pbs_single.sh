#! /bin/bash -e

#PBS -N OnsetDetection
#PBS -l np=1
<<<<<<< HEAD
#PBD -l nodes=1
=======
>>>>>>> cfe39894d4875b4e581faf647d11e680bc1d0eb8
#PBS -m be



# mru, 2011-05
# runs parameter study in parallel, MAX_NPROC processes at once
#

export CLASSPATH=build/classes:build/classes/jopt-simple.jar

f="data/train$(printf %02d ${PBS_ARRAYID}).wav"

cd ~/onset_detection

mainclass=at.cp.jku.teaching.amprocessing.ParamStudyRunner
basename=$(basename $f .wav)
java $mainclass -i $f -g data/$basename.onsets -t data/$basename.bpms -o output -p output/$basename.odf -q