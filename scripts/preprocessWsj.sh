#!/bin/bash
# Takes parsed wsj corpus and outputs text version using only 10000 most
# frequent words; others are mapped to <oov>
# Also partitions according to setup in 
# Mikolov, Tomas, et al. "Empirical Evaluation and Combination of Advanced
# Language Modeling Techniques." INTERSPEECH. 2011. 

cd lib/datasets/wsj

echo "Tokenizing..."
rm -rf text
mkdir text
python ../../../scripts/tokenizeWsj.py wsj text

echo "Partitioning..."
bash ../../../scripts/partitionWsj.sh

echo "Creating vocab.txt..."
../../../lib/srilm/bin/i686-m64/ngram-count -tolower -no-sos -no-eos -order 1 -text \
partitioned/train.txt -write - | sort -nrk2 | head -n10000 | cut -f1 > \
vocab.txt

# echo "Replace oov vocab..."
# python ../../../scripts/replaceOovWsj.py text vocab.txt
# # Redo partitions using <oov> words
# bash ../../../scripts/partitionWsj.sh
