#!/bin/sh
# Trains srilm Kneser-Ney language model on Penn treebank and reports
# perplexity.  Uses standard training setup as described in
# Mikolov, Tomas, et al. "Empirical Evaluation and Combination of Advanced
# Language Modeling Techniques." INTERSPEECH. 2011. 

smooth="-ukndiscount -interpolate -gt3min 1"

wsj=lib/datasets/wsj
lm=$wsj/lm.3bo

# create LM from training data
lib/srilm/bin/i686-m64/ngram-count -debug 1 \
   -text $wsj/partitioned/train.txt \
   -tolower \
   -order 3 \
   $smooth \
   -vocab $wsj/vocab.txt \
   -kn $wsj/kn.txt
   # -lm $lm

# test LM
lib/srilm/bin/i686-m64/ngram -debug 0 \
   -lm $lm \
   -tolower \
   -order 3 \
   -vocab $wsj/vocab.txt \
   -ppl $wsj/partitioned/test.txt

rm -f $lm
