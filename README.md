# AndroidSQLClientServer
Basic Android app that couples with a Java server to scan a barcode (or otherwise obtain data), connect to a remote SQL database, and return a query about that data

JDBC_Test is the Java server that acts as an intermediary between the Android app and the SQL database. It connects using simple Sockets and ServerSockets. 
It takes data from the client to query against the database and returns an ArrayList of the results it found

ProjectInfosphere is the Android app that, in its current form, launches a Barcode Scanner (specifically a version of zxing) through an Intent, retrieves parsed data, passes it to the JDBC_Test server and works with the data that it returns back
