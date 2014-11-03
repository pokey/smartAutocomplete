#!/bin/bash
curl -X POST --data-binary @data.txt http://localhost:8080/complete?path=/yo/man\&base=hi\&loc=24
