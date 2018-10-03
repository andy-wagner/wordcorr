#!/bin/bash

if [ $# -lt 5 ]
then
        echo "Usage: $0 <document file (each doc in a line)> <sample size (abs number of docs to use for co-occurrence estimation)> <output file> <frequency cutoff min %ge (0-100)> (typical value: 5) <frequency cutoff max %ge (0-100) (typical value: 90)>"
        exit
fi

DOCFILE=$1
SAMPLE_SIZE=$2
OUTFILE=$3
MIN=$4
MAX=$5

if [ ! -e $DOCFILE.$SAMPLE_SIZE ]
then
	cat $DOCFILE | gshuf | head -n$SAMPLE_SIZE > $DOCFILE.$SAMPLE_SIZE
fi

echo "Finding co-occurrences between words..."
nohup java Cooccur $DOCFILE.$SAMPLE_SIZE $3 $MIN $MAX

sort -nr -k3 $OUTFILE > $OUTFILE.s
head -n50 $OUTFILE.s
