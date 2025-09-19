//- Copyright Notice
// -----------------------------------------------------------------------
// (C) Copyright 2007 IdeaSoft Uruguay S.R.L.  All Rights Reserved
// THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF IdeaSoft Co.
// The copyright notice above does not evidence any actual or intended
// publication of such source code.
//
// $Id: TestThinDriver.java,v 1.12 2009/07/17 18:31:20 rbotto Exp $
// -----------------------------------------------------------------------

package samples;


import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;

public class TestThinDriver {
	private static int MDX = 0;
	private static int THIN = 1;
	private static int VIEW = 2;
	
	private static boolean testMultiplesMedidas = true;
	private static boolean testFechas = false;


	private  static String[] queries = {
		 "SELECT {Measures.[Units Sold], Measures.[Cost]} ON COLUMNS, {Customers.Customers.[Major Accounts]} ON ROWS FROM Demo WHERE Measures.Discount"/* ,
		 "SELECT NON EMPTY {Customers.[Major Accounts]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM Demo WHERE Measures.[Units Sold]",

			"SELECT {CubeInfo.LastModifiedDate} ON COLUMNS from Demo",
		"SELECT " +
				"NON ZERO {Location.children} ON ROWS, " +
				"CROSSJOIN ({Salesmen.children}, {Customers.[Major Accounts]}) ON COLUMNS " +
				"FROM Demo " +
				"WHERE Measures.[Units Sold]",
"SELECT CrossJoin({[Date].[Date].children}, {[<measures>].[<measures>].[% Profit], [<measures>].[<measures>].[Revenue]}) ON COLUMNS, {{[Products].[Products].children}} ON ROWS FROM [Demo] WHERE ([Customers].[Customers],[Salesmen].[Salesmen],[Location].[Location])",


*/

	};


	private static String getQuery(int i) {
		return queries[i];
	}

	public static void main(String[] args) throws Exception {
		Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
		
		
		
		
		int driverType = MDX;
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("mdx")) {
				driverType = MDX;
			} else if (args[0].equalsIgnoreCase("view")){
				driverType = VIEW;
			} else {
				driverType = THIN;
			}
		}
		
		String url = null;
		if (driverType == MDX) {
			url = "jdbc:o3:mdx://localhost:7777";			
		} else if (driverType == VIEW) {
			url = "jdbc:o3:view://localhost:7777";
		} else {
			url = "jdbc:o3:thin://localhost:7777";						
		}
		
		

		int connectionsCount = 1;
		int queriesCount = queries.length;
		
		for (int a = 0; a < connectionsCount; a++) {
			 java.util.Properties info = new java.util.Properties();

		        // Gets the classloader of the code that called this method, may 
			// be null.

		    info.put("user", "user");
		    info.put("password", "user");
		    info.put("COLUMNS_TYPE", "DIMENSION_LABEL");
		    info.put("MEMBER_BY_LABEL", "false");

			Connection conn = DriverManager.getConnection(url, info);
				Object[] params = null; //new Object[]{"4"};
				
				for (int i = 0; i < queriesCount; i++) {
					String query = getQuery(i);
					System.out.println("Query " + a + "," + i);
					runQuery(driverType, conn, query, params);
				}
				conn.close();
			}


		System.exit(0);
	}
	
	public static void runQuery(int driverType, Connection conn, String query, Object[] params) throws SQLException {
		Statement stmt;
		StringBuilder b = new StringBuilder();
			b.append(query);
			stmt = conn.prepareStatement(query);
			
			if (params != null) {
				int index = 0;
				for (Object p : params) {
					((PreparedStatement) stmt).setObject(++index, p);	
				}
			}
	//		String b = "SELECT CROSSJOIN({Products.Products.children}, {Customers.Customers.children}) ON ROWS, {Location.Location.France.Paris, Location.Location.France.Lille} ON COLUMNS FROM Demo WHERE {[<measures>].[<measures>].Discount}";
	
	//		String b = "SELECT {Location.Location.France.Paris, Location.Location.France.Lille} ON COLUMNS, {Products.Products.children} ON ROWS FROM Demo WHERE {[<measures>].[<measures>].Discount}";

		ResultSet rs;
			rs = ((PreparedStatement) stmt).executeQuery();

		ResultSetMetaData metadata = rs.getMetaData();
		int columnCount = metadata.getColumnCount();
		for (int c = 0; c < columnCount; c++) {
			System.out.print(metadata.getColumnLabel(c + 1) + " | ");
		}
		
		
		System.out.println();
		int rowsCount = 1000;
		int currentRow = 0;
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
		while (currentRow < rowsCount && rs.next()) {
			for (int c = 0; c < columnCount; c++) {
				Object o = metadata.getColumnType(c + 1) == Types.DATE ? rs.getDate(c + 1) : rs.getObject(c + 1);
				if (o instanceof Date) {
					o = format.format((Date) o);
				}
				System.out.print(o + " | ");
			}
			currentRow++;
			System.out.println();
		}
	}
}
