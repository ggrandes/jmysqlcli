# jMySQLCli

jmysqlcli is a simple command line client to query a mysql server, free as beer and open source (Apache License, Version 2.0).

---

## DOC

## Requirements, Installation and Running

* Java Runtime (8 o newer): https://jdk.java.net/java-se-ri/17
* This software run in [Portable Mode](https://en.wikipedia.org/wiki/Portable_application), you only need copy jar file in a folder to run.

#### Usage Example (command line)

    java -Djmysqlcli.verbose=true -Djmysqlcli.config=/opt/jmysqlcli/config.properties -jar /opt/jmysqlcli/jmysqlcli-x.x.x.jar

#### Config file example (properties):

    # JDBC Connection URL
    url=jdbc:mysql://localhost/test?user=test&password=test
    # credentials (alternative method 1)
    user=test
    password=test
    # Method 1, The query statement (only select allowed)
    query=SELECT 'OK' AS TEST
    # Method 2, A list of SQL statements (select, update, delete, etc), the order is 1-based; followed by 2, and so on.
    sql.1=SELECT 'OK' AS TEST
    sql.2=SELECT 'GOOD' AS TEST
    # format output in [CSV,HTML,MarkdownTable,AsciiTable], default is CSV
    format.output=CSV
    # default is show header row
    #header.wanted=true
    # default value is semicolon
    #column.separator=;
    # default value is LF
    #row.separator=\n


###### You can use alternative method 2 for credentials: MYSQL_USER & MYSQL_PASSWORD environment variables. Precedence is ENV, then properties.

#### References:

* JDBC Connection URL [syntax](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-jdbc-url-format.html)
* Query statement [syntax](https://dev.mysql.com/doc/refman/8.0/en/select.html)
* SQL statement [syntax](https://dev.mysql.com/doc/refman/8.0/en/sql-data-manipulation-statements.html)

---
Inspired in [mysql-client](https://linux.die.net/man/1/mysql) and [sqlline](https://github.com/julianhyde/sqlline), this is a Java-minimalistic version.
