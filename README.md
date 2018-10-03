# wordcorr (A light-weight word co-occurrence calculator) 

This project provides a very simple, efficient and light-weight implementation of global word co-occurrence computation. The implementation doesn't use any third-party libraries.

To compile the classes, simply execute (note that you need to have Java installed)
```
javac *java
```

To compute global word-level co-occurrence stats, simply execute
```
sh cooccur.sh <doc file> <num docs to sample> <output file> <min cut-off %ge> <max cut-off %ge>
``` 

The arguments are explained as follows.
1. **Doc File** is a text file, where each line is a document.
2. **Sample size** is the number of documents you want to sample for co-occurrence computation. For some large document collections, this sampling makes the process much faster.
3. **Output file** is a file where you want to store the co-occurrence stats. Each line of this o/p file is a 3-tuple comprising <word 1> <word 2> <co-occurrence weight between this pair>.
4. **Min cut-off** and **Max cut-off** are used to prune the vocabulary. These are specified as percentage values. A 5% min cut-off means that words whose frequencies are lower than that of 5% of the
max collection frequency (the number of times a word occurs in a collection) will be removed, and so is the case for max cut-off.

The program provides a sample input file, named 'dbpedia.subset.txt.gz', which is a subset of a randomly sampled DBPedia abstracts. Simply uncompress this file if you want to test the program on the sample provided.
```
gunzip dbpedia.subset.txt.gz
```

A sample invokation of the program is
```
sh dbpedia.subset.txt 50000 mat.txt 5 90 
```
which runs the program on a 10% subset of the sample file, prunes of words with frequencies less than 5% and higher than 90% of the max collection frequency. The script by default prints on the console the 50 top-most co-occurrences from the output file produced.  

The top-most 10 co-occurrences reported by the above invocation of the program are:
```
species	family	3347.7932
united	states	3048.0063
district	village	2321.0142
genus	family	2183.3386
album	released	2076.6511
census	population	1937.0870
species	genus	1913.2327
family	found	1895.1869
family	moth	1722.2452
directed	film	1704.0616
```


