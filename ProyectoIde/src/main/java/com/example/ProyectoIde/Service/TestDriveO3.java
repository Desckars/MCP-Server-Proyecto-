package com.example.ProyectoIde.Service;

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
import java.util.Properties;
import java.util.StringJoiner;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class TestDriveO3 {
    
	private static int MDX = 0;
	private static int THIN = 1;
	private static int VIEW = 2;
	
	private static boolean testMultiplesMedidas = true;
	private static boolean testFechas = false;
    @Tool(description = "Recibe una consulta MDX y devuelve los resultados como una cadena formateada.")
    public String executeMdxQuery(@ToolParam(description = "Consulta MDX a ejecutar") String mdxQuery) {
        // Aquí iría la lógica para ejecutar la consulta MDX usando el driver O3
        // y devolver los resultados como una cadena formateada.
        // Por ahora, devolvemos una cadena de ejemplo.
        return "Resultados de la consulta MDX: " + mdxQuery;
    }

    // Probare primer QUERY ya precargadas
     private final SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
    
    // Las consultas se mantienen tal cual las proporcionaste
    private final String[] queries = {
         "SELECT {Measures.[Units Sold], Measures.[Cost]} ON COLUMNS, {Customers.Customers.[Major Accounts]} ON ROWS FROM Demo WHERE Measures.Discount",
         "SELECT NON EMPTY {Customers.[Major Accounts]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM Demo WHERE Measures.[Units Sold]",
         "SELECT {CubeInfo.LastModifiedDate} ON COLUMNS from Demo",
         "SELECT NON ZERO {Location.children} ON ROWS, CROSSJOIN ({Salesmen.children}, {Customers.[Major Accounts]}) ON COLUMNS FROM Demo WHERE Measures.[Units Sold]",
         "SELECT CrossJoin({[Date].[Date].children}, {[<measures>].[<measures>].[% Profit], [<measures>].[<measures>].[Revenue]}) ON COLUMNS, {{[Products].[Products].children}} ON ROWS FROM [Demo] WHERE ([Customers].[Customers],[Salesmen].[Salesmen],[Location].[Location])"
    };
    /**
     * Ejecuta todas las consultas MDX de prueba y retorna un resumen de los resultados.
     * @return Una cadena de texto con la salida de todas las consultas.
     */
    @Tool(description = "Ejecuta todas las consultas MDX de prueba y retorna los resultados de cada una.")
    public String runAllQueries() {
        StringJoiner fullResults = new StringJoiner("\n\n---\n\n");
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = "jdbc:o3:mdx://localhost:7777";
            Properties info = new Properties();
            info.put("user", "user");
            info.put("password", "user");
            info.put("COLUMNS_TYPE", "DIMENSION_LABEL");
            info.put("MEMBER_BY_LABEL", "false");

            try (Connection conn = DriverManager.getConnection(url, info)) {
                for (int i = 0; i < queries.length; i++) {
                    fullResults.add("Query #" + (i + 1) + ": " + queries[i]);
                    fullResults.add(runQuery(conn, queries[i], null));
                }
            }
        } catch (Exception e) {
            return "Error en la ejecución de las consultas: " + e.getMessage();
        }
        return fullResults.toString();
    }

    private String runQuery(Connection conn, String query, Object[] params) throws SQLException {
        StringJoiner queryResults = new StringJoiner("\n");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            StringJoiner headerJoiner = new StringJoiner(" | ");
            for (int c = 1; c <= columnCount; c++) {
                headerJoiner.add(metadata.getColumnLabel(c));
            }
            queryResults.add(headerJoiner.toString());

            while (rs.next()) {
                StringJoiner rowJoiner = new StringJoiner(" | ");
                for (int c = 1; c <= columnCount; c++) {
                    Object o = metadata.getColumnType(c) == Types.DATE ? rs.getDate(c) : rs.getObject(c);
                    if (o instanceof Date) {
                        rowJoiner.add(format.format((Date) o));
                    } else {
                        rowJoiner.add(String.valueOf(o));
                    }
                }
                queryResults.add(rowJoiner.toString());
            }
        }
        return queryResults.toString();
    }
}
