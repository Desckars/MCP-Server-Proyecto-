package o3.utec.mcp_o3.config;

import org.springframework.stereotype.Component;
import o3.utec.mcp_o3.ProyectoIdeApplication;

import java.util.concurrent.atomic.AtomicBoolean;//Boolean atomico para manejo de multihilos

// Componente para inyectar instrucciones del sistema en la primera ejecución de la sesión.
@Component
public class InjectInstrccion {
    private final AtomicBoolean instructionsShown = new AtomicBoolean(false);
    
    // Verifica si es la primera ejecución de la sesión. Si es así, retorna las instrucciones del sistema.
    public String checkAndInjectInstructions() {
        // Verificar y marcar como mostrado en una operación atómica
        if (!instructionsShown.getAndSet(true)) {
            String instructions = ProyectoIdeApplication.getsystemprompt();
            
            if (instructions == null || instructions.isEmpty()) {
                return "WARNING: System instructions could not be loaded Call getSystemInstructions() to retry.";
            }
            
            return instructions;
        }
        
        return "";
    }
    
    //Verifica si las instrucciones ya fueron mostradas en esta sesión.     
    public boolean areInstructionsShown() {
        return instructionsShown.get();
    }    
    //Obtiene un recordatorio breve si las instrucciones aún no se han mostrado.
    public String getQuickReminder() {
        if (!instructionsShown.get()) {
            return "TIP: Call getSystemInstructions() to see complete usage guidelines.";
        }
        return "";
    }
}
