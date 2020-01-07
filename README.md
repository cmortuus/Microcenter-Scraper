# Find Microcenter Deals
#### To Create the Database
Install MySQL. Here is a good guide for it:
https://support.rackspace.com/how-to/installing-mysql-server-on-ubuntu/

I have come up with a problem regurlarly where MySQL's root password does not configure properly upon the first install. I would sugest you use this guide if that happens to you. https://askubuntu.com/questions/766334/cant-login-as-mysql-user-root-from-normal-user-account-in-ubuntu-16-04

```
GRANT ALL ON MicrocenterItems.* TO 'microcenter'@'localhost';
```
```
FLUSH PRIVILEGES;
```
```
mysql -u root -p
```
```
source sqlCode.sql
```
To create accounts to log in with run
```
UPDATE THIS
```
#### To Scrape the Data 
Download grekodriver and put somewhere in the path. I sugest /usr/local/bin
```
python3 scrape.py
```
or
```
java -jar MicrocenterScraper-fat-1.0.jar
```