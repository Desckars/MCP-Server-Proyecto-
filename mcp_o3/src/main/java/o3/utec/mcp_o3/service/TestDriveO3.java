package o3.utec.mcp_o3.service;

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

    @Tool(description = "Ejecuta una consulta MDX específica contra el cubo O3 Demo y retorna los resultados formateados. " +
          "\n\nPATRONES DE CONSULTA COMUNES:" +
          "\n1. Consulta simple por medida: SELECT {Measures.[Units Sold]} ON COLUMNS FROM Demo" +
          "\n2. Por dimensión: SELECT {Measures.[Units Sold]} ON COLUMNS, {Location.children} ON ROWS FROM Demo" +
          "\n3. Múltiples medidas: SELECT {Measures.[Units Sold], Measures.[Cost]} ON COLUMNS FROM Demo" +
          "\n4. Con filtro WHERE: SELECT {Measures.[Units Sold]} ON COLUMNS FROM Demo WHERE Measures.Discount" +
          "\n5. NON EMPTY para omitir valores vacíos: SELECT NON EMPTY {Location.children} ON ROWS FROM Demo" +
          "\n6. CROSSJOIN para cruzar dimensiones: CROSSJOIN({Salesmen.children}, {Customers.[Major Accounts]})" +
          "\n7. Info del cubo: SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM Demo" +
          "\n\nEJEMPLOS DE INTERPRETACIÓN:" +
          "\n- 'mostrar ventas por ubicación' → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM Demo" +
          "\n- 'costos y unidades para cuentas principales' → SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM Demo" +
          "\n- 'ingresos por producto' → SELECT {Measures.[Revenue]} ON COLUMNS, NON EMPTY {Products.children} ON ROWS FROM Demo")
    public String executeCustomMdxQuery(@ToolParam(description = "Consulta MDX a ejecutar contra el cubo Demo") String mdxQuery) {
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = "jdbc:o3:mdx://localhost:7777";
            Properties info = new Properties();
            info.put("user", "user");
            info.put("password", "user");
            info.put("COLUMNS_TYPE", "DIMENSION_LABEL");
            info.put("MEMBER_BY_LABEL", "false");

            try (Connection conn = DriverManager.getConnection(url, info)) {
                return runQuery(conn, mdxQuery, null);
            }
        } catch (Exception e) {
            return "Error ejecutando consulta MDX: " + e.getMessage() + 
                   "\nConsulta intentada: " + mdxQuery;
        }
    }
    @Tool(description = "Obtiene información sobre los cubos disponibles en el servidor O3, incluyendo sus dimensiones y medidas. " +
          "Esto es útil para construir consultas MDX apropiadas para cubos específicos.")
    public String getCubeInformation(@ToolParam(description = "Nombre del cubo a analizar (opcional). Si no se especifica, lista todos los cubos disponibles.") String cubeName) {
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = "jdbc:o3:mdx://localhost:7777";
            Properties info = new Properties();
            info.put("user", "user");
            info.put("password", "user");
            info.put("COLUMNS_TYPE", "DIMENSION_LABEL");
            info.put("MEMBER_BY_LABEL", "false");

            try (Connection conn = DriverManager.getConnection(url, info)) {
                if (cubeName == null || cubeName.trim().isEmpty()) {
                    // Listar todos los cubos disponibles
                    return listAvailableCubes(conn);
                } else {
                    // Obtener información específica del cubo
                    return getCubeStructure(conn, cubeName.trim());
                }
            }
        } catch (Exception e) {
            return "Error obteniendo información de cubos: " + e.getMessage();
        }
    }

    private String listAvailableCubes(Connection conn) {
        StringJoiner result = new StringJoiner("\n");
        result.add("=== CUBOS DISPONIBLES ===");
        
        try {
            // Intentar obtener metadatos de cubos (esto puede variar según la implementación de O3)
            String query = "SELECT {CubeInfo.CubeName} ON COLUMNS FROM $system";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    result.add("- " + rs.getString(1));
                }
            } catch (SQLException e) {
                // Si falla la consulta del sistema, mostrar cubos conocidos
                result.add("- Demo (cubo de ejemplo conocido)");
                result.add("Nota: No se pudieron obtener todos los cubos del sistema. Error: " + e.getMessage());
            }
        } catch (Exception e) {
            result.add("Error listando cubos: " + e.getMessage());
        }
        
        return result.toString();
    }

    private String getCubeStructure(Connection conn, String cubeName) {
        StringJoiner result = new StringJoiner("\n");
        result.add("=== ESTRUCTURA DEL CUBO: " + cubeName + " ===");
        
        try {
            // Obtener información general del cubo
            String infoQuery = "SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM [" + cubeName + "]";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(infoQuery)) {
                
                if (rs.next()) {
                    result.add("Última modificación: " + rs.getString(1));
                }
            } catch (SQLException e) {
                result.add("Advertencia: No se pudo obtener información general del cubo");
            }

            result.add("\nNota: Para obtener dimensiones y medidas específicas, use consultas MDX exploratorias como:");
            result.add("- Para dimensiones: SELECT NON EMPTY {[NombreDimension].children} ON ROWS FROM [" + cubeName + "]");
            result.add("- Para medidas: SELECT {Measures.AllMembers} ON COLUMNS FROM [" + cubeName + "]");
            
        } catch (Exception e) {
            result.add("Error analizando cubo " + cubeName + ": " + e.getMessage());
        }
        
        return result.toString();
    }
}
