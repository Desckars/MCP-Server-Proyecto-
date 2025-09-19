package com.example.ProyectoIde;


import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.ProyectoIde.Service.TestDriveO3;
@SpringBootApplication
public class ProyectoIdeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyectoIdeApplication.class, args);
	}

	// Podemos usar ToolCallbackProvider para registrar nuestras herramientas
	// personalizadas.
	// Aquí registramos TestDriveO3 como herramientas disponibles.
	// También podríamos registrar herramientas individuales si quisiéramos.
	@Bean
	public ToolCallbackProvider Tools( TestDriveO3 testDriveO3) {
		return MethodToolCallbackProvider.builder().toolObjects(testDriveO3).build();
	}


}