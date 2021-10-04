#!/bin/bash

i=0

while [ $i -le 100 ]
do
  echo Number: $i | kafka-console-producer --broker-list localhost:9092 --topic test
  let "i+=1" 
  sleep 1
done