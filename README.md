# PayTracker
HSBC PofC - by Jack Malik


1/. PROJECT REQUIREMENTS

The project is pom-based and requires access to Maven and Internet connection.
The project requires IntellJ Idea IDE to build and run.

2/. PROJECT BUILD AND RUNNING

2.1/. Please import project to IntelliJ
2.2/. Please configure it by selecting Java version and the language level
2.3/. Compile and build.
2.4/. When started the PayTracker will display links to be used to access the web interface (URL and port #)
      as well as the H2 database console.

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

5/. PAYTRACKER MISSING

    * The pom.xml does not provide packaging so the application can only be executed from withing IntelliJ
      or any other suitable IDE.
    * Packaging of the app and the required command-line script can easily be provided by extending pom.xml
      and executing 'java -classpath <> .. *.jar' command.
    * The project is lacking unit testing due to time constraints.
