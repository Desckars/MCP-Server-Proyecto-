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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import o3.utec.mcp_o3.ProyectoIdeApplication;
import o3.utec.mcp_o3.config.InjectInstrccion;

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

    // Inyector de instrucciones en primera ejecución
    @Autowired
    private InjectInstrccion instructionsInjector;

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
    //System Prompt General
    //Funcionara como un una tool haciendo que el LLM pueda acceder a este contenido cuando sea arranque el MCP
    @Tool(description = "MANDATORY: Call this before using other tools for the first time."+
                        "READ THIS FIRST! Returns comprehensive system instructions for using the MCP O3 Server correctly. " +
                        "Contains important guidelines, MDX syntax rules, and best practices. " +
                        "ALWAYS call this before using other tools for the first time.")
    public String getSystemInstructions() {
        String instructions = ProyectoIdeApplication.getsystemprompt();
        if (instructions == null || instructions.isEmpty()) {
            return "ERROR: System Prompt General no cargado.";
        }
        // Marcar como mostrado para evitar auto-inyección duplicada
        instructionsInjector.checkAndInjectInstructions();
        
        return instructions;
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
        7 - 1 - SELECT {Date.children} ON COLUMNS FROM [CubeName] 

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
    public String executeCustomMdxQuery(@ToolParam(description = "Consulta MDX a ejecutar contra el cubo CubeName") String mdxQuery) {
        // Agregar recordatorio si las instrucciones no se han visto
        String reminder = instructionsInjector.getQuickReminder();
        try {
            Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
            String url = o3ServerUrl;
            Properties info = new Properties();
            info.put("user", o3Username);
            info.put("password", o3Password);
            info.put("COLUMNS_TYPE", o3ColumnsType);
            info.put("MEMBER_BY_LABEL", o3MemberByLabel);

            try (Connection conn = DriverManager.getConnection(url, info)) {
                return reminder + runQuery(conn, mdxQuery, null);                
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
            // AUTO-INJECT: Cargar instrucciones en la primera llamada
            String autoLoadedInstructions = instructionsInjector.checkAndInjectInstructions();
            try {
                Class.forName("com.ideasoft.o3.jdbc.thin.client.O3ThinDriver");
                String url = o3ServerUrl;
                Properties info = new Properties();
                info.put("user", o3Username);
                info.put("password", o3Password);
                info.put("COLUMNS_TYPE", o3ColumnsType);
                info.put("MEMBER_BY_LABEL", o3MemberByLabel);

                try (Connection conn = DriverManager.getConnection(url, info)) {
                    String result;
                    if (cubeName == null || cubeName.trim().isEmpty() || cubeName.equalsIgnoreCase("null")) {
                        result = listAvailableCubes(conn);
                    } else {
                        result = getCubeStructure(conn, cubeName.trim());
                    }
                    return autoLoadedInstructions + result;
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
    
}