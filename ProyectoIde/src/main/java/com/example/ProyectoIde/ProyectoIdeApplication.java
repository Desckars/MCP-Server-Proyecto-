package com.example.ProyectoIde;


import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.ProyectoIde.Service.WeatherService;
import com.example.ProyectoIde.Service.TestDriveO3;
@SpringBootApplication
public class ProyectoIdeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyectoIdeApplication.class, args);
	}

	// Podemos usar ToolCallbackProvider para registrar nuestras herramientas
	// personalizadas.
	// Aquí registramos WeatherService y TestDriveO3 como herramientas disponibles.
	// También podríamos registrar herramientas individuales si quisiéramos.
	@Bean
	public ToolCallbackProvider weatherTools(WeatherService weatherService, TestDriveO3 testDriveO3) {
		return MethodToolCallbackProvider.builder().toolObjects(weatherService, testDriveO3).build();
	}


}