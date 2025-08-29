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

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
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
	 * Default Format? [CSV, HTML, AsciiTable] (default: CSV)
	 */
	public static final String DEFAULT_FORMAT = "CSV";
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
				err.println("### LOADING CONFIG: " + cf);
			}
			try (FileReader f = new FileReader(cf)) {
				p.load(f);
			} catch (IOException e) {
				err.println("### ERROR LOADING CONFIG: " + cf);
				throw e;
			}
			final String url = p.getProperty("url", TEST_URL);
			final String user = getFirstNonNull(System.getenv("MYSQL_USER"), //
					p.getProperty("user"));
			final String password = getFirstNonNull(System.getenv("MYSQL_PASSWORD"), //
					p.getProperty("password"));
			try (Connection conn = createConnection(url, user, password)) {
				final String format = p.getProperty("format.output", DEFAULT_FORMAT);
				final boolean headerWanted = Boolean
						.parseBoolean(p.getProperty("header.wanted", DEFAULT_HEADER_WANTED));
				final String columnSeparator = p.getProperty("column.separator", DEFAULT_COLUMN_SEPARATOR);
				final String rowSeparator = p.getProperty("row.separator", DEFAULT_ROW_SEPARATOR);
				// The order is 1-based; followed by 2, and so on.
				if (!p.getProperty("sql.1", "").isEmpty()) {
					for (int i = 1; i < Short.MAX_VALUE; i++) {
						final String sql = p.getProperty("sql." + i, "");
						final BaseDecorator d = BaseDecorator.getInstance(format, //
								headerWanted, columnSeparator, rowSeparator);
						if (!sql.isEmpty()) {
							execute(conn, sql, d);
						} else {
							break;
						}
					}
				} else {
					final String query = p.getProperty("query", TEST_QUERY);
					final BaseDecorator d = BaseDecorator.getInstance(format, //
							headerWanted, columnSeparator, rowSeparator);
					executeQuery(conn, query, d);
				}
			}
			if (verbose) {
				err.println("### END");
			}
		} catch (Throwable t) {
			err.println("### EXCEPTION: " + t);
			if (verbose) {
				t.printStackTrace(System.err);
			}
		}
	}

	private static Connection createConnection(final String url, final String user, final String password) //
			throws SQLException {
		if (verbose) {
			err.println("### CONNECTING: " + url);
		}
		try {
			if ((user != null) && (password != null)) {
				return DriverManager.getConnection(url, user, password);
			} else {
				return DriverManager.getConnection(url);
			}
		} catch (SQLException e) {
			err.println("### ERROR CONNECTING: " + url);
			throw e;
		}
	}

	private static void executeQuery(final Connection con, //
			final String query, //
			final BaseDecorator d) throws SQLException {
		final int rsType = d.rsType();
		try (Statement stmt = con.createStatement(rsType, ResultSet.CONCUR_READ_ONLY)) {
			if (verbose) {
				err.println("### QUERY: " + query);
			}
			try (final ResultSet rs = stmt.executeQuery(query)) {
				process(rs, d);
			}
		} catch (SQLException e) {
			err.println("### ERROR QUERY: " + query);
			throw e;
		}
	}

	private static void execute(final Connection con, //
			final String sql, //
			final BaseDecorator d) throws SQLException {
		final int rsType = d.rsType();
		try (Statement stmt = con.createStatement(rsType, ResultSet.CONCUR_READ_ONLY)) {
			if (verbose) {
				err.println("### SQL: " + sql);
			}
			boolean haveRS = stmt.execute(sql);
			do {
				if (haveRS) {
					try (final ResultSet rs = stmt.getResultSet()) {
						process(rs, d);
					}
				} else {
					final int rcount = stmt.getUpdateCount();
					err.println("### UPDATE ROW COUNT: " + rcount);
					err.flush();
				}
			} while (haveRS = stmt.getMoreResults());
		} catch (SQLException e) {
			err.println("### ERROR SQL: " + sql);
			throw e;
		}
	}

	private static void process(final ResultSet rs, //
			final BaseDecorator d) throws SQLException {
		// Initialize if needed
		d.init(rs);
		// Show column names
		final ResultSetMetaData md = rs.getMetaData();
		final int columns = md.getColumnCount();
		d.begin();
		if (d.headerWanted) {
			d.headerBegin();
			for (int i = 1; i <= columns; i++) {
				String value = md.getColumnLabel(i);
				if (value == null) {
					value = "NULL";
				}
				d.headerCell(i, value);
			}
			d.headerEnd();
		}
		// Show row data
		while (rs.next()) {
			d.rowBegin();
			for (int i = 1; i <= columns; i++) {
				String value = rs.getString(i);
				if (value == null) {
					value = "NULL";
				}
				d.rowCell(i, value);
			}
			d.rowEnd();
		}
		d.end();
		out.flush();
	}

	private static String getFirstNonNull(final String... values) {
		for (final String v : values) {
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	private static String escapeHTML(final String in) {
		final int len = in.length();
		final StringBuilder sb = new StringBuilder(len + 32);
		for (int i = 0; i < len; i++) {
			final char c = in.charAt(i);
			// https://www.w3.org/TR/html4/sgml/entities.html
			switch (c) {
				case '&':
					sb.append("&amp;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '<':
					sb.append("&lt;");
					break;
				case '"':
					sb.append("&quot;");
					break;
				case '\'':
					sb.append("&apos;");
					break;
				case '\u20AC':
					sb.append("&euro;");
					break;
				case '\u00D1': // Ñ
					sb.append("&Ntilde;");
					break;
				case '\u00F1': // ñ
					sb.append("&ntilde;");
					break;
				case '\u007C': // |
					sb.append("&#124;"); // Markdown
					break;
				default:
					if (c > 127) {
						sb.append("&#").append((int) c).append(';');
					} else {
						sb.append(c);
					}
					break;
			}
		}
		return sb.toString();
	}

	private static String padding(final int inLen, final int desiredLen, final char c) {
		int i = desiredLen - inLen;
		if (i > 0) {
			final StringBuilder sb = new StringBuilder(i + 8);
			while (i-- > 0) {
				sb.append(c);
			}
			return sb.toString();
		}
		return "";
	}

	private static abstract class BaseDecorator {
		final boolean headerWanted;
		final String columnSeparator;
		final String rowSeparator;

		BaseDecorator(final boolean headerWanted, //
				final String columnSeparator, //
				final String rowSeparator) {
			this.headerWanted = headerWanted;
			this.columnSeparator = columnSeparator;
			this.rowSeparator = rowSeparator;
		}

		public static BaseDecorator getInstance(final String format, //
				final boolean headerWanted, //
				final String columnSeparator, //
				final String rowSeparator) {
			switch (format.toUpperCase(Locale.ENGLISH)) {
				case "CSV":
					return new TextDelimitedDecorator(headerWanted, columnSeparator, rowSeparator);
				case "HTML":
					return new HTMLDecorator(headerWanted, rowSeparator);
				case "MARKDOWNTABLE":
					return new MarkdownTableDecorator(headerWanted, rowSeparator);
				case "ASCIITABLE":
					return new AsciiTableDecorator(headerWanted, rowSeparator);
			}
			throw new IllegalArgumentException("unknown value: " + format);
		}

		boolean needWidth() {
			return false;
		}

		int rsType() {
			if (needWidth()) {
				return ResultSet.TYPE_SCROLL_INSENSITIVE;
			}
			return ResultSet.TYPE_FORWARD_ONLY;
		}

		void init(final ResultSet rs) throws SQLException {
		}

		void begin() {
		}

		void headerBegin() {
		}

		abstract void headerCell(final int idx, final String label);

		void headerEnd() {
		}

		void rowBegin() {
		}

		abstract void rowCell(final int idx, final String value);

		void rowEnd() {
		}

		void end() {
		}
	}

	private static class TextDelimitedDecorator extends BaseDecorator {
		int columns;

		TextDelimitedDecorator(final boolean headerWanted, //
				final String columnSeparator, //
				final String rowSeparator) {
			super(headerWanted, columnSeparator, rowSeparator);
		}

		@Override
		void init(final ResultSet rs) throws SQLException {
			final ResultSetMetaData md = rs.getMetaData();
			this.columns = md.getColumnCount();
		}

		@Override
		void headerCell(final int idx, final String label) {
			out.print(label);
			if (idx < columns) {
				out.print(columnSeparator);
			}
		}

		@Override
		void headerEnd() {
			out.print(rowSeparator);
		}

		@Override
		void rowCell(final int idx, String value) {
			out.print(value);
			if (idx < columns) {
				out.print(columnSeparator);
			}
		}

		@Override
		void rowEnd() {
			out.print(rowSeparator);
		}
	}

	private static class HTMLDecorator extends BaseDecorator {
		HTMLDecorator(final boolean headerWanted, //
				final String rowSeparator) {
			super(headerWanted, "", rowSeparator);
		}

		@Override
		void begin() {
			out.print("<!DOCTYPE HTML>");
			out.print(rowSeparator);
			out.print("<HTML>");
			out.print(rowSeparator);
			out.print("<HEAD>");
			out.print(rowSeparator);
			out.print("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
			out.print(rowSeparator);
			out.print("</HEAD>");
			out.print(rowSeparator);
			out.print("<BODY>");
			out.print(rowSeparator);
			out.print("<FONT face=\"Verdana\">");
			out.print(rowSeparator);
			out.print("<TABLE cellspacing=\"0\" cellpadding=\"5\" border=\"1\">");
			out.print(rowSeparator);
		}

		@Override
		void headerBegin() {
			out.print("<TR>");
		}

		@Override
		void headerCell(final int idx, final String label) {
			out.print("<TH align=\"left\">");
			out.print(label);
			out.print("</TH>");
			out.print(columnSeparator);
		}

		@Override
		void headerEnd() {
			out.print("</TR>");
			out.print(rowSeparator);
		}

		@Override
		void rowBegin() {
			out.print("<TR>");
		}

		@Override
		void rowCell(final int idx, String value) {
			out.print("<TD>");
			out.print(escapeHTML(value));
			out.print("</TD>");
			out.print(columnSeparator);
		}

		@Override
		void rowEnd() {
			out.print("</TR>");
			out.print(rowSeparator);
		}

		@Override
		void end() {
			out.print("</TABLE>");
			out.print(rowSeparator);
			out.print("</BODY>");
			out.print(rowSeparator);
			out.print("</HTML>");
			out.print(rowSeparator);
		}
	}

	private static class MarkdownTableDecorator extends BaseDecorator {
		String rowBand;

		MarkdownTableDecorator(final boolean headerWanted, //
				final String rowSeparator) {
			super(headerWanted, "|", rowSeparator);
		}

		void init(final ResultSet rs) throws SQLException {
			final ResultSetMetaData md = rs.getMetaData();
			final int columns = md.getColumnCount();
			// Pre-calculate header/foot rowband
			final StringBuilder sb = new StringBuilder();
			sb.append(columnSeparator);
			for (int i = 1; i <= columns; i++) {
				sb.append(" :--- ").append(columnSeparator);
			}
			rowBand = sb.toString();
		}

		@Override
		void headerBegin() {
			out.print(columnSeparator);
		}

		@Override
		void headerCell(final int idx, final String label) {
			rowCell(idx, label);
		}

		@Override
		void headerEnd() {
			out.print(rowSeparator);
			out.print(rowBand);
			out.print(rowSeparator);
		}

		@Override
		void rowBegin() {
			out.print(columnSeparator);
		}

		@Override
		void rowCell(final int idx, final String value) {
			out.print(' ');
			out.print(escapeHTML(value));
			out.print(' ');
			out.print(columnSeparator);
		}

		@Override
		void rowEnd() {
			out.print(rowSeparator);
		}
	}

	private static class AsciiTableDecorator extends BaseDecorator {
		int[] width;
		String rowBand;

		AsciiTableDecorator(final boolean headerWanted, //
				final String rowSeparator) {
			super(headerWanted, "|", rowSeparator);
		}

		@Override
		boolean needWidth() {
			return true;
		}

		void init(final ResultSet rs) throws SQLException {
			final ResultSetMetaData md = rs.getMetaData();
			final int columns = md.getColumnCount();
			final int[] width = new int[columns + 1];
			// Pre-Calculate column widths
			final int NULL_LENGTH = 4;
			if (headerWanted) {
				final int len = NULL_LENGTH;
				for (int i = 1; i <= columns; i++) {
					width[i] = md.getColumnLabel(i).length();
					if ((width[i] < len) //
							&& (md.isNullable(i) != ResultSetMetaData.columnNoNulls)) {
						width[i] = len;
					}
				}
			}
			while (rs.next()) {
				for (int i = 1; i <= columns; i++) {
					final String v = rs.getString(i);
					final int len = ((v == null) ? NULL_LENGTH : v.length());
					if (width[i] < len) {
						width[i] = len;
					}
				}
			}
			this.width = width;
			// Pre-calculate header/foot rowband
			final StringBuilder sb = new StringBuilder();
			sb.append('+');
			for (int i = 1; i <= columns; i++) {
				sb.append(padding(0, width[i] + 2, '-'));
				if (i < columns) {
					sb.append('+');
				}
			}
			sb.append('+');
			rowBand = sb.toString();
			rs.beforeFirst(); // rewind
		}

		@Override
		void headerBegin() {
			out.print(rowBand);
			out.print(rowSeparator);
			out.print(columnSeparator);
		}

		@Override
		void headerCell(final int idx, final String label) {
			rowCell(idx, label);
		}

		@Override
		void headerEnd() {
			out.print(rowSeparator);
			out.print(rowBand);
			out.print(rowSeparator);
		}

		@Override
		void rowBegin() {
			out.print(columnSeparator);
		}

		@Override
		void rowCell(final int idx, final String value) {
			out.print(' ');
			out.print(value);
			out.print(padding(value.length(), width[idx], ' '));
			out.print(' ');
			out.print(columnSeparator);
		}

		@Override
		void rowEnd() {
			out.print(rowSeparator);
		}

		@Override
		void end() {
			out.print(rowBand);
			out.print(rowSeparator);
		}
	}
}
