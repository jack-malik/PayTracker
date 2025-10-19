PayTracker

HSBC PofC - by Jack Malik

1/. PROJECT REQUIREMENTS

The project is pom-based and requires access to Maven and Internet connection. The project requires IntellJ Idea IDE to build and run.

2/. PROJECT BUILD AND RUNNING

2.1/. Please import project to IntelliJ 2.2/. Please configure it by selecting Java version and the language level 2.3/. Compile and build. 2.4/. When started the PayTracker will display links to be used to access the web interface (URL and port #) as well as the H2 database console.

3/. PAYTRACKER services

PayTracker supports posting payments in 3 currencies: USD, CAD, GBP as defined by enumerated types

1/. http://localhost:51234/paytracker/payment/<CURRENCY CODE>/<AMOUNT> ex:
    /paytracker/USD/500
2/. http://localhost:51234/paytracker/currencies

3/. http://localhost:51234/paytracker/payers

4/. http://localhost:51234/paytracker/payments

4/. PAYTRACKER IMPLEMENTATON

* PayTracker makes use of H2 database.
* When executed for the first time it will attempt to find paytracker.mv.db file in 'user.home' directory.
* If database does not exist it will be created. It will contain 3 tables: PAYMENT, CURRENCY, PAYER
  Please see 'schema.sql' file under project 'resources' directory
* If PayTracker is killed and restarted it will use the existing database created earlier.
  The content of the existing database will be used to continue tracking.
* PayTracker will display in the console the aggregated payments for each currency used.
* The user can view content of the H2 database by login into the database using link of the form:
  http://192.168.*.*:8082 (the actual internal address 192.168.*.* is displayed on the console
* The main loop implemented in the PayTracker.track() method will run for 30 minutes and if the app
  is not killed during that time it will gracefully exit closing all opened resources to avoid memory
  leaks.

5/. H2 DATABASE

PayTracker uses H2 database to store data. It creates H2 database 'paytracker.mv.db' in ${user.home}
directory if such database does not exist. User can view the content of tables by logging to localhost:8082.
When logging into database set:
   5.1/. Saved Settings: Generic H2 (Embedded)
   5.2/. Setting Name  : Generic H2 (Embedded)
   5.3/. Driver Class  : org.h2.Driver
   5.4/. User Name     : sa
   5.5/. Password      : (empty)

When PayTracker exists the H2 database remains on disk. When PayTracker starts again user can continue
with all data from previous run safely stored in the existing database. PayTracker opens the existing
database which is ready to be used.
