#!/bin/sh
while true; do
  java -jar MicrocenterScrape-fat-1.1.jar
  java -jar MicrocenterEmails-fat-1.1.jar
  pkill -f firefox
done
