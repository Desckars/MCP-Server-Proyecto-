// src/test/java/com/example/mcp_server/McpServerApplicationTests.java
package com.example.mcp_server;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Disabled("Deshabilitado hasta que la base de datos O3 esté en ejecución.")
class McpServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
   void testMdxQueryEndpoint() throws Exception {
        // Consulta MDX modificada para que coincida con la estructura esperada por el servidor
        String mdxQuery = "SELECT {Measures.[Units Sold]} ON COLUMNS FROM Demo";

        // Realiza una petición POST al endpoint /api/mdx/query
        mockMvc.perform(post("/api/mdx/query")
                .contentType(MediaType.TEXT_PLAIN)
                .content(mdxQuery))
                .andExpect(status().isOk()); // Espera una respuesta HTTP 200 (OK)
    }
}