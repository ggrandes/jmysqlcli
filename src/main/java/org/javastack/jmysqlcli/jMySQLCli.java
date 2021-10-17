/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.javastack.jmysqlcli;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class jMySQLCli {
	/**
	 * Property name to Enable Verbose Mode
	 */
	public static final String PROP_NAME_VERBOSE = "jmysqlcli.verbose";
	/**
	 * Property name to Define config file
	 */
	public static final String PROP_NAME_CONFIG = "jmysqlcli.config";
	/**
	 * Default Verbose? (default: false)
	 */
	public static final String DEFAULT_VERBOSE = "false";
	/**
	 * Default Config file? (default: "jmysqlcli.properties")
	 */
	public static final String DEFAULT_CONFIG = "jmysqlcli.properties";
	/**
	 * Default Header Wanted? (default: true)
	 */
	public static final String DEFAULT_HEADER_WANTED = "true";
	/**
	 * Default Column Separator (default: ';')
	 */
	public static final String DEFAULT_COLUMN_SEPARATOR = ";";
	/**
	 * Default Row Separator (default: LF)
	 */
	public static final String DEFAULT_ROW_SEPARATOR = "\n";
	/**
	 * Default Dummy/Test URL connection
	 */
	public static final String TEST_URL = "jdbc:mysql://localhost/test?user=test&password=test";
	/**
	 * Default Dummy/Test QUERY
	 */
	public static final String TEST_QUERY = "SELECT 'OK' AS TEST";

	private static final boolean verbose = Boolean
			.valueOf(System.getProperty(PROP_NAME_VERBOSE, DEFAULT_VERBOSE));
	private static final String configFile = System.getProperty(PROP_NAME_CONFIG, DEFAULT_CONFIG);

	public static void main(final String[] args) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			//
			final Properties p = new Properties();
			final File cf = new File(configFile).getCanonicalFile();
			if (verbose) {
				System.err.println("### LOADING CONFIG: " + cf);
			}
			try (FileReader f = new FileReader(cf)) {
				p.load(f);
			} catch (IOException e) {
				System.err.println("### ERROR LOADING CONFIG: " + cf);
				throw e;
			}
			final String url = p.getProperty("url", TEST_URL);
			try (Connection conn = createConnection(url)) {
				final String query = p.getProperty("query", TEST_QUERY);
				final boolean headerWanted = Boolean
						.parseBoolean(p.getProperty("header.wanted", DEFAULT_HEADER_WANTED));
				final String columnSeparator = p.getProperty("column.separator", DEFAULT_COLUMN_SEPARATOR);
				final String rowSeparator = p.getProperty("row.separator", DEFAULT_ROW_SEPARATOR);
				executeQuery(conn, query, headerWanted, columnSeparator, rowSeparator);
			}
			if (verbose) {
				System.err.println("### END");
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	private static Connection createConnection(final String url) throws SQLException {
		if (verbose) {
			System.err.println("### CONNECTING: " + url);
		}
		try {
			return DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.err.println("### ERROR CONNECTING: " + url);
			throw e;
		}
	}

	private static void executeQuery(final Connection con, //
			final String query, //
			final boolean headerWanted, //
			final String columnSeparator, //
			final String rowSeparator) throws SQLException {
		try (Statement stmt = con.createStatement()) {
			if (verbose) {
				System.err.println("### QUERY: " + query);
			}
			final ResultSet rs = stmt.executeQuery(query);
			final ResultSetMetaData md = rs.getMetaData();
			final int columns = md.getColumnCount();
			// Show column names
			if (headerWanted) {
				for (int i = 1; i <= columns; i++) {
					System.out.print(md.getColumnLabel(i));
					if (i < columns) {
						System.out.print(columnSeparator);
					}
				}
				System.out.print(rowSeparator);
			}
			// Show row data
			while (rs.next()) {
				for (int i = 1; i <= columns; i++) {
					System.out.print(rs.getObject(i));
					if (i < columns) {
						System.out.print(columnSeparator);
					}
				}
				System.out.print(rowSeparator);
			}
			System.out.flush();
		} catch (SQLException e) {
			System.err.println("### ERROR QUERY: " + query);
		}
	}
}
