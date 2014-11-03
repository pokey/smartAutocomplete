#!/bin/bash
# Trains maxent model on frozen version of smartAutocomplete and then tests it
# on Triple.java

expectedEntropy="2.2026355646959748"
out="src/test/triple.out"

echo "  Computing entropy..."
rm -rf $out
java -ea -cp classes:lib/* smartAutocomplete.Main -execDir $out \
  -overwriteExecDir -data.permute true -dataRandom 1 \
  -inPaths lib/datasets/core-smartAutocomplete -tokenizeCode true \
  -testCountPaths src/test/Triple.java -ngramOrder 5 -maxCandidates 1000 \
  -featureDomains NgramKN Recency FileNameKN CommonTokenKN \
  -count 0,1 -tune 0.9,1 -tuningTotal 5000 &> /dev/null
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
