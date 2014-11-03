#!/bin/sh
# Trains srilm Kneser-Ney language model on Penn treebank and reports
# perplexity.  Uses standard training setup as described in
# Mikolov, Tomas, et al. "Empirical Evaluation and Combination of Advanced
# Language Modeling Techniques." INTERSPEECH. 2011. 

smooth="-ukndiscount1 -interpolate1 -ukndiscount2 -interpolate2 -ukndiscount3 -interpolate3 -gt3min 1"

wsj=lib/datasets/wsj
dataset=lib/datasets/test
lm=$dataset/lm.3bo

# create LM from training data
lib/srilm/bin/i686-m64/ngram-count -debug 10 \
   -text $dataset/wsj_train.txt \
   -tolower \
   $smooth \
   -order 3 \
   -vocab $wsj/vocab.txt \
   -lm $lm

# test LM
lib/srilm/bin/i686-m64/ngram -debug 10 \
   -lm $lm \
   -tolower \
   -order 3 \
   -vocab $wsj/vocab.txt \
   -ppl $dataset/wsj_test.txt

# rm -f $lm
