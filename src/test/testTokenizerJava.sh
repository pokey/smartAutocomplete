#!/bin/bash

out="src/test/tokenizer.java.out"
gold="src/test/tokenizer.java.gold"

java -cp classes:lib/* smartAutocomplete.test.TestTokenizer java > $out

if ! diff -u $gold $out 
then
  echo "[FAIL]"
  exit -1
else
  echo "[PASS]"
  rm -rf $out
fi
