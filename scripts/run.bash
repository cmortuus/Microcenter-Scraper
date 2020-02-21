#!/bin/sh
while true
do
java -jar build/libs/MicrocenterScrape-fat-1.1.jar
pkill -f firefox
done
