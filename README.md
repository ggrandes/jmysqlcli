# jMySQLCli

jmysqlcli is a simple command line client to query a mysql server, free as beer and open source (Apache License, Version 2.0).

---

## DOC

## Requirements, Installation and Running

* Java Runtime (8 o newer): https://jdk.java.net/java-se-ri/14
* This software run in [Portable Mode](https://en.wikipedia.org/wiki/Portable_application), you only need copy jar file in a folder to run.

#### Usage Example (command line)

    java -Djmysqlcli.verbose=true -Djmysqlcli.config=/opt/jmysqlcli/config.properties -jar /opt/jmysqlcli/jmysqlcli-x.x.x.jar

#### Config file example (properties):

    # JDBC Connection URL
    url=jdbc:mysql://localhost/test?user=test&password=test
    # The query statement
    query=SELECT 'OK' AS TEST
    # default is show header row
    #header.wanted=true
    # default value is semicolon
    #column.separator=;
    # default value is LF
    #row.separator=\n

#### References:

* JDBC Connection URL [syntax](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html)
* Query statement [syntax](https://dev.mysql.com/doc/refman/8.0/en/select.html)

---
Inspired in [mysql-client](https://linux.die.net/man/1/mysql), this is a Java-minimalistic version.
