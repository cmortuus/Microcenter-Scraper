import os
import time

while True:
    os.system("java -jar MicrocenterScrape-fat-1.0.jar")
    time.sleep(3600)
    os.system("killall firefox")
    os.system("killall java")
