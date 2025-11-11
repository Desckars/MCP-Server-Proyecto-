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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestDriveO3 {
    // Configuraciones de conexión
    // La logica es la siguiente:
    // Busca la propiedad o3.server.username. Si NO la encuentras, usa user como valor por defecto (o3.server.username:user).
    // No esta implmentado mesnajes de error por tema de configuracion faltante, se asume que el usuario sabe como configurar estas propiedades.
    @Value("${o3.server.url}")
    private String o3ServerUrl;

    @Value("${o3.server.username}")
    private String o3Username;

    @Value("${o3.server.password}")
    private String o3Password;

    @Value("${o3.server.columnsType}")
    private String o3ColumnsType;

    @Value("${o3.server.memberByLabel}")
    private String o3MemberByLabel;


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
    // ========================================
    // CONNECTION MANAGEMENT
    // ========================================
    
    private Connection getO3Connection() throws Exception {
        Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
        Properties info = new Properties();
        info.put("user", o3Username);
        info.put("password", o3Password);
        info.put("COLUMNS_TYPE", o3ColumnsType);
        info.put("MEMBER_BY_LABEL", o3MemberByLabel);
        return DriverManager.getConnection(o3ServerUrl, info);
    }
     //----------------------------------------------------------------------------------------------------------------
    @Tool(description = "Ejecuta todas las consultas MDX de prueba y retorna los resultados de cada una.")
    public String runAllQueries() {
        StringJoiner fullResults = new StringJoiner("\n\n---\n\n");
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = o3ServerUrl;
            Properties info = new Properties();
            info.put("user", o3Username);
            info.put("password", o3Password);
            info.put("COLUMNS_TYPE", o3ColumnsType);
            info.put("MEMBER_BY_LABEL", o3MemberByLabel);

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
        Execute a custom MDX query against the O3 cube server and return the results.
        Use the following guidelines to construct your MDX queries:
        
        COOMON MDX QUERY PATTERNS:
        1. Simple query by measure: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] 
        2. By dimension: SELECT {Measures.[MeasureName]} ON COLUMNS, {[Dimension].children} ON ROWS FROM [CubeName] 
        3. Multiple measures: SELECT {Measures.[MeasureName1], Measures.[MeasureName2]} ON COLUMNS FROM [CubeName]
        4. With WHERE filter: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] WHERE Measures.[MeasureFilter]
        5. NON EMPTY to amit empty/void variables/values: SELECT NON EMPTY {[Dimension].children} ON ROWS FROM [CubeName]
        6. CROSSJOIN to cross dimensions: CROSSJOIN({[Dimension1].children}, {[Dimension2].[SpecificMember]})
        7. Cube info: SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM [CubeName]


        Interpretention examples (Use these only as a reference for constructing your own queries, do not copy them directly. Use only the cube the user specifies):
        1 - 'show units sold by location' → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [CubeName]
        2 - 'costs and units sold by major accounts' → SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM [CubeName]
        3 - 'earnings by product' → SELECT {Measures.[Revenue]} ON COLUMNS, NON EMPTY {Products.children} ON ROWS FROM [CubeName]
        4 - 'Units sold in France for client types Major Accounts and Minor Accounts'
        SELECT {Customers.[Major Accounts], Customers.[Minor Accounts]} ON COLUMNS, {Location.[France]} ON ROWS  FROM  [CubeName] WHERE  (Measures.[Units Sold])
        5 - 'global vision of each salesman with units sold and commissions earned'
        SELECT {Measures.[Units Sold], Measures.[Commissions]} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
        6 - 'revenue for mountain bikes professional in US for years 2002 and 2003'
        SELECT {Date.Date.[2002], Date.Date.[2003]} ON COLUMNS, {Location.[US]} ON ROWS FROM [CubeName] WHERE  (Products.[Mountain Bikes].[Professional], Measures.[Revenue])
        7 - 'show all major accounts in France with units sold'
       SELECT {Customers.[Major Accounts].children} ON COLUMNS, {Location.[France].children} ON ROWS FROM   [CubeName] WHERE  (Measures.[Units Sold])
        8 - 'children members of salesmen with total number of children'
        SELECT {Measures.children} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
        9 - 'quey involvs 3 dimensions: products, locations y dates. 3 axis visualizacion is a bit complex and what is generaly requerid is to show this info following a bi-dimensional format encapsuling the 3 dimensions'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, {
        (Location.[Brazil], Products.[Mountain Bikes].[Professional]),
        (Location.[Brazil], Products.[Mountain Bikes].[Recreational]),
        (Location.[Spain], Products.[Mountain Bikes].[Professional]),
        (Location.[Spain], Products.[Mountain Bikes].[Recreational])
        } ON ROWS FROM  [CubeName] WHERE (Measures.[Units Sold])
        10 - 'MDX offers the function CrossJoin(). this function offers every possible combinantion from 2 sets/arrays (in other words "cartesian product"). It's commonly used for situations like the one shown before combining 2+ dimensions in a singular axis in a bi-dimensional matrix format'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, CrossJoin({Location.children}, {Products.[Mountain Bikes].children}) ON ROWS FROM  [CubeName] WHERE (Measures.[Units Sold])
        11 - 'Let's suppose we want to see the evolution of cost per unit sold across the years except 2002 for every product line'
        SELECT except(Date.Year.Members, {Date.[2002]}) on COLUMNS, {Products.Line.Members} on ROWS FROM   [CubeName] WHERE  (Measures.[Cost])
        12 - 'Get all cities in France'
        SELECT {Location.[France].children} ON COLUMNS, {} ON ROWS FROM [CubeName]
        13. 'Get all cities accross every location' SELECT {Measures.[Units Sold]} ON COLUMNS, Descendants(Location, Location.City) ON ROWS FROM [CubeName]
        14.'Return the elements in the first set that arrent present in the second one' SELECT Except(Date.Year.Members, {Date.[2002]}) ON COLUMNS, {Products.Line.Members} ON ROWS FROM [CubeName] WHERE (Measures.[Cost])
        15.'Returns available cubes in the server ' SELECT {Cubes} ON COLUMNS FROM SYSCATALOG
        16.'Returns dimensions of a specific cube' SELECT {Dimensions} ON COLUMNS FROM [CubeName] 
        17.'Returns measures of a specific cube' SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
        """)
        /*
        @Tool(description = """
        Ejecuta una consulta MDX específica contra el cubo O3 y retorna los resultados formateados.
        
        PATRONES DE CONSULTA COMUNES:
        1. Consulta simple por medida: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] 
        2. Por dimensión: SELECT {Measures.[MeasureName]} ON COLUMNS, {[Dimension].children} ON ROWS FROM [CubeName] 
        3. Múltiples medidas: SELECT {Measures.[MeasureName1], Measures.[MeasureName2]} ON COLUMNS FROM [CubeName]
        4. Con filtro WHERE: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] WHERE Measures.[MeasureFilter]
        5. NON EMPTY para omitir valores vacíos: SELECT NON EMPTY {[Dimension].children} ON ROWS FROM [CubeName]
        6. CROSSJOIN para cruzar dimensiones: CROSSJOIN({[Dimension1].children}, {[Dimension2].[SpecificMember]})
        7. Info del cubo: SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM [CubeName]


        EJEMPLOS DE INTERPRETACIÓN ( TOMA ESTO SOLO COMO REFERENCIA, NO LO TOMES COMO DATOS PARA CONSULTA, USA SOLO LOS CUBO QUE EL USUARIO TE SOLICITE ):
        1 - 'mostrar ventas por ubicación' → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [CubeName]
        2 - 'costos y unidades para cuentas principales' → SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM [CubeName]
        3 - 'ingresos por producto' → SELECT {Measures.[Revenue]} ON COLUMNS, NON EMPTY {Products.children} ON ROWS FROM [CubeName]
        4 - 'unidades vendidas en France sólo para los tipos de clientes Major Accounts y Minor Accounts.'
        SELECT {Customers.[Major Accounts], Customers.[Minor Accounts]} ON COLUMNS, {Location.[France]} ON ROWS  FROM  [CubeName] WHERE  (Measures.[Units Sold])
        5 - 'visión global del comportamiento de cada uno de los vendedores con respecto a las unidades vendidas y sus comisiones del modelo de ventas independientemente del resto de las dimensiones de análisis'
        SELECT {Measures.[Units Sold], Measures.[Commissions]} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
        6 - 'ver los ingresos (Revenue) por la venta de bicicletas Mountain bikes profesionales en los años 2002 y 2003 en US'
        SELECT {Date.Date.[2002], Date.Date.[2003]} ON COLUMNS, {Location.[US]} ON ROWS FROM [CubeName] WHERE  (Products.[Mountain Bikes].[Professional], Measures.[Revenue])
        7 - 'unidades vendidas en las distintas ciudades de France por parte de los clientes bajo el tipo denominado Major Accounts.'
       SELECT {Customers.[Major Accounts].children} ON COLUMNS, {Location.[France].children} ON ROWS FROM   [CubeName] WHERE  (Measures.[Units Sold])
        8 - 'nos interesa estudiar por tal o cual medida sino por todas aquellas que se tengan definidas en el modelo de análisis.'
        SELECT {Measures.children} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
        9 - 'consulta involucra 3 dimensiones: productos, ubicaciones y fechas. La visualización usando 3 ejes es algo complejo y lo que en general se quiere es presentar esta información siguiendo el formato bi-dimensional y encapsular las 3 dimensiones.'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, {
        (Location.[Brazil], Products.[Mountain Bikes].[Professional]),
        (Location.[Brazil], Products.[Mountain Bikes].[Recreational]),
        (Location.[Spain], Products.[Mountain Bikes].[Professional]),
        (Location.[Spain], Products.[Mountain Bikes].[Recreational])
        } ON ROWS FROM  [CubeName] WHERE (Measures.[Units Sold])
        10 - 'MDX brinda la función CrossJoin(). Esta función produce todas las combinaciones de 2 conjuntos (es decir, un "producto cartesiano"). Su uso común es para situaciones como la presentada arriba combinando 2 o mas dimensiones en un único eje a los efectos de visualizar los datos bajo la forma de una matriz bi-dimensional'
        SELECT {Date.[2001], Date.[2002]} ON COLUMNS, CrossJoin({Location.children}, {Products.[Mountain Bikes].children}) ON ROWS FROM  [CubeName] WHERE (Measures.[Units Sold])
        11 - 'Supongamos que se desea ver la evolución en el tiempo salvo en el año 2002 de los costos por la venta de todas las líneas de bicicletas.'
        SELECT except(Date.Year.Members, {Date.[2002]}) on COLUMNS, {Products.Line.Members} on ROWS FROM   [CubeName] WHERE  (Measures.[Cost])
        12 - 'consultar cuales son las ciudades de Francia sin importar que valores tengan en sus medidas.'
        SELECT {Location.[France].children} ON COLUMNS, {} ON ROWS FROM [CubeName]
        13. 'Obtiene todas las ciudades de todas las ubicaciones.' SELECT {Measures.[Units Sold]} ON COLUMNS, Descendants(Location, Location.City) ON ROWS FROM [CubeName]
        14.'Devuelve los elementos del primer conjunto que NO están en el segundo.' SELECT Except(Date.Year.Members, {Date.[2002]}) ON COLUMNS, {Products.Line.Members} ON ROWS FROM [CubeName] WHERE (Measures.[Cost])
        """);
        */
    public String executeCustomMdxQuery(@ToolParam(description = "Consulta MDX a ejecutar contra el cubo CubeName") String mdxQuery) {
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = o3ServerUrl;
            Properties info = new Properties();
            info.put("user", o3Username);
            info.put("password", o3Password);
            info.put("COLUMNS_TYPE", o3ColumnsType);
            info.put("MEMBER_BY_LABEL", o3MemberByLabel);

            try (Connection conn = DriverManager.getConnection(url, info)) {
                return runQuery(conn, mdxQuery, null);
            }
        } catch (Exception e) {
            return "Error ejecutando consulta MDX: " + e.getMessage() + 
                   "\nConsulta intentada: " + mdxQuery;
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    @Tool(description = "Retrieves information about available cubes on the server, including their dimensions and measures. " +
      "This is useful for building appropriate MDX queries for specific cubes.")
        public String getCubeInformation(@ToolParam(description = "Name of the cube to analyze. Use null to list all available cubes.") 
        String cubeName) {
            
            try {
                Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
                String url = o3ServerUrl;
                Properties info = new Properties();
                info.put("user", o3Username);
                info.put("password", o3Password);
                info.put("COLUMNS_TYPE", o3ColumnsType);
                info.put("MEMBER_BY_LABEL", o3MemberByLabel);

                try (Connection conn = DriverManager.getConnection(url, info)) {
                    if (cubeName == null || cubeName.trim().isEmpty() || cubeName.equalsIgnoreCase("null")) {
                        return listAvailableCubes(conn);
                    } else {
                        return getCubeStructure(conn, cubeName.trim());
                    }
                }
            } catch (Exception e) {
                return "Error retrieving cube information: " + e.getMessage();
            }
        }

    private String listAvailableCubes(Connection conn) {
        StringJoiner result = new StringJoiner("\n");
        result.add("=== AVAILABLE CUBES ===\n");
        
        try {
            String query = "SELECT {Cubes} ON COLUMNS FROM SYSCATALOG";
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
                
                int count = 0;
                while (rs.next()) {
                    result.add("- " + rs.getString(1));
                    count++;
                }
                
                if (count == 0) {
                    result.add("No cubes found.");
                } else {
                    result.add("\nTotal cubes: " + count);
                }
            }
        } catch (SQLException e) {
            result.add("Error listing cubes: " + e.getMessage());
        }
        
        return result.toString();
    }

    private String getCubeStructure(Connection conn, String cubeName) {
        StringJoiner result = new StringJoiner("\n");
        result.add("=== CUBE STRUCTURE: " + cubeName + " ===\n");
        
        // Get dimensions
        result.add("--- DIMENSIONS ---");
        try {
            String dimensionsQuery = "SELECT {Dimensions} ON COLUMNS FROM [" + cubeName + "]";
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(dimensionsQuery)) {
                
                int dimCount = 0;
                while (rs.next()) {
                    result.add("- " + rs.getString(1));
                    dimCount++;
                }
                
                if (dimCount == 0) {
                    result.add("No dimensions found.");
                } else {
                    result.add("Total dimensions: " + dimCount);
                }
            }
        } catch (SQLException e) {
            result.add("Error retrieving dimensions: " + e.getMessage());
        }
        
        // Get measures
        result.add("\n--- MEASURES ---");
        try {
            String measuresQuery = "SELECT {Measures.Members} ON COLUMNS FROM [" + cubeName + "]";
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(measuresQuery)) {
                
                int measCount = 0;
                while (rs.next()) {
                    result.add("- " + rs.getString(1));
                    measCount++;
                }
                
                if (measCount == 0) {
                    result.add("No measures found.");
                } else {
                    result.add("Total measures: " + measCount);
                }
            }
        } catch (SQLException e) {
            result.add("Error retrieving measures: " + e.getMessage());
        }
        
        // Add helpful tips
        result.add("\n--- USAGE TIPS ---");
        result.add("To explore dimension members:");
        result.add("  SELECT {[DimensionName].children} ON ROWS FROM [" + cubeName + "]");
        result.add("\nTo query specific data:");
        result.add("  SELECT {[MeasureName]} ON COLUMNS, {[DimensionName].children} ON ROWS FROM [" + cubeName + "]");
        
        return result.toString();
    }
    //----------------------------------------------------------------------------------------------------------------

}