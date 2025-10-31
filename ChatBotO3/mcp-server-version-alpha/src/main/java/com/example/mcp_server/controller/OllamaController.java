package com.example.mcp_server.controller;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;  
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/mcp")
public class OllamaController {

    private final RestTemplate restTemplate = new RestTemplate();//clase de Spring utilizada para hacer llamadas HTTP a servicios externos. 
    //Se crea como una variable final, lo que significa que se inicializa una sola vez y se reutiliza para todas las peticiones.

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestBody Map<String, Object> request) {//Spring toma el cuerpo de la petición (que se espera que sea JSON) y lo convierte automáticamente en un Map<String, Object>
        String url = "http://127.0.0.1:11434/api/generate";

        // ✅ Usa HttpHeaders de Spring
        HttpHeaders headers = new HttpHeaders();//Se crea un objeto para configurar los encabezados de la petición saliente.
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);//Combina el cuerpo de la petición (request) y los encabezados (headers) en un solo objeto que se enviará a Ollama.

        return restTemplate.postForEntity(url, entity, String.class);//envía la petición POST al url 
        //del servidor de Ollama (http://127.0.0.1:11434/api/generate) y devuelve la respuesta de la API como un ResponseEntity.
    }
}
