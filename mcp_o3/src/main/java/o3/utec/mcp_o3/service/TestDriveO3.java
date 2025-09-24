package o3.utec.mcp_o3.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
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
    
     //----------------------------------------------------------------------------------------------------------------
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

    //----------------------------------------------------------------------------------------------------------------
    @Tool(description = """
        Ejecuta una consulta MDX específica contra el cubo O3 Demo y retorna los resultados formateados.
        
        PATRONES DE CONSULTA COMUNES:
        1. Consulta simple por medida: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] 
        2. Por dimensión: SELECT {Measures.[MeasureName]} ON COLUMNS, {[Dimension].children} ON ROWS FROM [CubeName] 
        3. Múltiples medidas: SELECT {Measures.[MeasureName1], Measures.[MeasureName2]} ON COLUMNS FROM [CubeName]
        4. Con filtro WHERE: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] WHERE Measures.[MeasureFilter]
        5. NON EMPTY para omitir valores vacíos: SELECT NON EMPTY {[Dimension].children} ON ROWS FROM [CubeName]
        6. CROSSJOIN para cruzar dimensiones: CROSSJOIN({[Dimension1].children}, {[Dimension2].[SpecificMember]})
        7. Info del cubo: SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM [CubeName]
        
        EJEMPLOS DE INTERPRETACIÓN:
        1 - 'mostrar ventas por ubicación' → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM Demo
        2 - 'costos y unidades para cuentas principales' → SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM Demo
        3 - 'ingresos por producto' → SELECT {Measures.[Revenue]} ON COLUMNS, NON EMPTY {Products.children} ON ROWS FROM Demo
        4 - 'unidades vendidas en France sólo para los tipos de clientes Major Accounts y Minor Accounts.'
        SELECT {Customers.[Major Accounts], Customers.[Minor Accounts]} ON COLUMNS, {Location.[France]} ON ROWS  FROM  Demo WHERE  (Measures.[Units Sold])
        5 - 'visión global del comportamiento de cada uno de los vendedores con respecto a las unidades vendidas y sus comisiones del modelo de ventas independientemente del resto de las dimensiones de análisis'
        SELECT {Measures.[Units Sold], Measures.[Commissions]} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM Demo
        6 - 'ver los ingresos (Revenue) por la venta de bicicletas Mountain bikes profesionales en los años 2002 y 2003 en US'
        SELECT {Date.Date.[2002], Date.Date.[2003]} ON COLUMNS, {Location.[US]} ON ROWS FROM Demo WHERE  (Products.[Mountain Bikes].[Professional], Measures.[Revenue])
        7 - 'unidades vendidas en las distintas ciudades de France por parte de los clientes bajo el tipo denominado Major Accounts.'
       SELECT {Customers.[Major Accounts].children} ON COLUMNS, {Location.[France].children} ON ROWS FROM   Demo WHERE  (Measures.[Units Sold])
        8 - 'nos interesa estudiar por tal o cual medida sino por todas aquellas que se tengan definidas en el modelo de análisis.'
        SELECT {Measures.children} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM Demo
        9 - 'consulta involucra 3 dimensiones: productos, ubicaciones y fechas. La visualización usando 3 ejes es algo complejo y lo que en general se quiere es presentar esta información siguiendo el formato bi-dimensional y encapsular las 3 dimensiones.'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, {
        (Location.[Brazil], Products.[Mountain Bikes].[Professional]),
        (Location.[Brazil], Products.[Mountain Bikes].[Recreational]),
        (Location.[Spain], Products.[Mountain Bikes].[Professional]),
        (Location.[Spain], Products.[Mountain Bikes].[Recreational])
        } ON ROWS FROM  Demo WHERE (Measures.[Units Sold])
        10 - 'MDX brinda la función CrossJoin(). Esta función produce todas las combinaciones de 2 conjuntos (es decir, un "producto cartesiano"). Su uso común es para situaciones como la presentada arriba combinando 2 o mas dimensiones en un único eje a los efectos de visualizar los datos bajo la forma de una matriz bi-dimensional'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, CrossJoin({Location.children}, {Products.[Mountain Bikes].children}) ON ROWS FROM  Demo WHERE (Measures.[Units Sold])
        11 - 'Supongamos que se desea ver la evolución en el tiempo salvo en el año 2002 de los costos por la venta de todas las líneas de bicicletas.'
        SELECT except(Date.Year.Members, {Date.[2002]}) on COLUMNS, {Products.Line.Members} on ROWS FROM   Demo WHERE  (Measures.[Cost])
        12 - 'consultar cuales son las ciudades de Francia sin importar que valores tengan en sus medidas.'
        SELECT {Location.[France].children} ON COLUMNS, {} ON ROWS FROM Demo
        """)
    public String executeCustomMdxQuery(@ToolParam(description = "Consulta MDX a ejecutar contra el cubo CubeName") String mdxQuery) {
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

    //----------------------------------------------------------------------------------------------------------------
    @Tool(description = "Obtiene información sobre los cubos disponibles en el servidor, incluyendo sus dimensiones y medidas. " +
          "Esto es útil para construir consultas MDX apropiadas para cubos específicos.")
    public String getCubeInformation(@ToolParam(description = "Nombre del cubo a analizar. Si no se especifica, utilizar null para listar todos los cubos disponibles.") String cubeName) {
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = "jdbc:o3:mdx://localhost:7777";
            Properties info = new Properties();
            info.put("user", "user");
            info.put("password", "user");
            info.put("COLUMNS_TYPE", "DIMENSION_LABEL");
            info.put("MEMBER_BY_LABEL", "false");

            try (Connection conn = DriverManager.getConnection(url, info)) {
                if (cubeName == null || cubeName.trim().isEmpty() || cubeName.equalsIgnoreCase("null")) {
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
    //----------------------------------------------------------------------------------------------------------------

}
