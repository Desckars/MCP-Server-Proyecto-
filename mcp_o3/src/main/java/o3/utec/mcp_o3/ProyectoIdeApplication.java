package o3.utec.mcp_o3;


import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import o3.utec.mcp_o3.service.TestDriveO3;
@SpringBootApplication
public class ProyectoIdeApplication {

	private static String systemprompt;

	public static void main(String[] args) {
		cargarsystemprompt();
		SpringApplication.run(ProyectoIdeApplication.class, args);
	}

	private static void cargarsystemprompt() {
		try {
			ClassPathResource resource = new ClassPathResource("systemprompt/Generales.md");
			systemprompt = new String(resource.getInputStream().readAllBytes());			
		} catch (IOException e) {			
			systemprompt = "";
		}
	}
    
    public static String getsystemprompt() {
        return systemprompt;
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