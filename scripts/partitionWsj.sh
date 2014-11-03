#!/bin/bash

rm -rf wsjCat
mkdir wsjCat
catDir() {
   dir=text/$1
   cat $dir/* > wsjCat/$1.txt
}

for i in {0..9}
do
   catDir 0$i
   catDir 1$i
done
catDir 20
catDir 21
catDir 22
catDir 23
catDir 24

rm -rf partitioned
mkdir partitioned
cd wsjCat
cat 00.txt 01.txt 02.txt 03.txt 04.txt 05.txt 06.txt 07.txt 08.txt 09.txt \
    10.txt 11.txt 12.txt 13.txt 14.txt 15.txt 16.txt 17.txt 18.txt 19.txt \
    20.txt > ../partitioned/train.txt
cat 21.txt 22.txt > ../partitioned/dev.txt
cat 23.txt 24.txt > ../partitioned/test.txt
cd ..
rm -rf wsjCat
