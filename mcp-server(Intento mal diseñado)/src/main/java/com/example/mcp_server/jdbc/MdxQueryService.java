package com.example.mcp_server.jdbc;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service// marca la clase como un componente de servicio en Spring
public class MdxQueryService {
    //Variables de conexión
    private final String url = "jdbc:o3:thin://localhost:7777";
    private final String user = "user";
    private final String password = "user";
// Método para ejecutar una consulta MDX y devolver los resultados como una lista de mapas
    public List<Map<String, Object>> executeMdxQuery(String mdxQuery) throws SQLException {//Toma la consulta MDX como un String.
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // La conexión se maneja manualmente
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(mdxQuery)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object columnValue = rs.getObject(i);
                    row.put(columnName, columnValue);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            // Propagar la excepción para que el controlador la maneje
            throw e;
        }

        return rows;
    }
}