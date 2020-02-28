# Find Microcenter Deals
#### To Create the Database
Install MySQL. Here is a good guide for it:
https://support.rackspace.com/how-to/installing-mysql-server-on-ubuntu/

I have come up with a problem regurlarly where MySQL's root password does not configure properly upon the first install. I would sugest you use this guide if that happens to you. https://askubuntu.com/questions/766334/cant-login-as-mysql-user-root-from-normal-user-account-in-ubuntu-16-04

### Set up MySQL
````
mysql -u root -p
````
```
CREATE USER 'microcenter'@'localhost' IDENTIFIED BY 'YourPassword';
GRANT SELECT, INSERT, DELETE, ON `MicrocenterItems`.* TO 'microcenter'@'localhost';
exit
```
```
mysql -u root -p < MicrocenterItems.sql
```
### To Scrape the Data 
Download grekodriver and put somewhere in the path. I sugest /usr/local/bin
```
java -jar MicrocenterScraper-fat-1.1.jar
```
or 
```
cd scripts && bash run.bash
```

### Make emails work
If you want to make the emailing work then you have to also run MicrocenterEmails-fat-1.1.jar. 

To do this you must set up a gmail configured to enable less secure apps and then put the user name and password into the java code and compile it with that.

## Make it work with the app
Well this is harder. I am building an app that can work as a frontend for this, but you will have to set up https://github.com/Catalyze326/Mezzanine-Server on a server that has a static ip and or a dynamic dns and then modify the code in https://github.com/Catalyze326/Flutter-Server-Frontend to allow you to run the app from your server.