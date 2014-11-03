#!/bin/bash
# Runs 3-gram Kneser-Ney on tiny subset of wsj and makes sure it gives what it
# is supposed to.

expectedEntropy="7.783416558701117"
out="src/test/wsj.out"

echo "  Computing entropy..."
rm -rf $out
java -ea -cp classes:lib/* smartAutocomplete.Main -execDir $out \
  -overwriteExecDir -trainPaths src/test/wsj_train.txt \
  -testPaths src/test/wsj_test.txt -splitDocumentByLine true \
  -vocabFile lib/datasets/wsj/vocab.txt -toLower -data.permute false \
  -ngramOrder 3 -featureDomains NgramKN -params.defaultWeight 1 &> /dev/null
entropy=`sed -n -e 's/[[:space:]]//g' -e 's/eval.entropy.mean//p' $out/output.map`

if [ "$entropy" != "$expectedEntropy" ]
then
  echo "Entropy was $entropy. Expected $expectedEntropy." 
  echo "Results in $out"
  echo "[FAIL]"
  exit -1
else
  echo "[PASS]"
  rm -rf $out
fi
