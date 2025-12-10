package com.example.mcp_server.jdbc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController//marca la clase como un controlador de Spring que maneja peticiones web y automáticamente serializa los objetos de retorno a JSON o XML
@RequestMapping("/api/mdx")//URL base para todas las rutas dentro de este controlador.
public class MdxQueryController {

    private final MdxQueryService mdxQueryService;

    @Autowired//n el constructor le dice a Spring que busque una instancia de MdxQueryService (que está anotada con @Service) y la "inyecte" automáticamente cuando se cree una instancia de este controlador.
    public MdxQueryController(MdxQueryService mdxQueryService) {
        this.mdxQueryService = mdxQueryService;
    }

    @PostMapping("/query")//Mapea este método para manejar las peticiones POST
    public ResponseEntity<?> executeQuery(@RequestBody String mdxQuery) {//Indica que el cuerpo de la petición HTTP entrante (el body) debe ser capturado como una cadena de texto y asignado a la variable mdxQuery. Esto es donde la consulta MDX es recibida.
        try {
            List<Map<String, Object>> result = mdxQueryService.executeMdxQuery(mdxQuery);
            return ResponseEntity.ok(result);
        } catch (SQLException e) {
            // Devolver un error HTTP 500 con un mensaje útil si la consulta falla
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al ejecutar la consulta MDX: " + e.getMessage());
        }
    }
}
