#!/bin/bash

out="src/test/tokenizer.python.out"
gold="src/test/tokenizer.python.gold"

java -cp classes:lib/* smartAutocomplete.test.TestTokenizer python > $out

if ! diff -u $gold $out 
then
  echo "[FAIL]"
  exit -1
else
  echo "[PASS]"
  rm -rf $out
fi
