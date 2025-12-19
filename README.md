Este repositorios se divide en 3 secciones

1 - mcp_o3(MCP diseñado por nosotros)

Pasos previos a la Instalación del MCP:
Tener previamente Instalado Claude Desktop

Agregar el MCP a Claude Desktop:
Después de tener Instalado Claude tenemos que dirigirnos a las configuraciones,  un atajo es presionar Ctrl + coma.
Seleccionamos la opción de Desarrollador y le damos a Editar Configuración.
Se nos mostrará en pantalla  la carpeta de Claude.
Abrimos el archivo claude_desktop_config.json.
En el json agregamos lo siguiente:
"MCP_O3_Server": {
      "command": "java",
      "args": [
      "-Dspring.ai.mcp.server.stdio=true",
      "-Dspring.main.banner-mode=off",
      
      "-Do3.server.url=jdbc:o3:mdx://localhost:7777",
      "-Do3.server.username=user",
      "-Do3.server.password=user",
      "-Do3.server.columnsType=DIMENSION_LABEL",
      "-Do3.server.memberByLabel=true",

      "-jar",
        "Ubicacion del /mcp_o3-0.0.3-SNAPSHOT.jar"
      ]
    }

Nota de gran importancia: 
Es necesario cambiar la dirección que se encuentra en la Línea 9 por la dirección en la que se encuentre el .jar del MCP_O3.
Si aparece el siguiente error, la solución es cambiar todos los ”\” por “/”.
Guardamos y reiniciamos Claude Desktop.

Con esto realizado ya estamos prontos para poner en marcha el servidor de O3.

Dejo un link a los resultados de la conversación.
https://claude.ai/share/0dce9a74-2339-4477-9947-bd866e05381c 

2 - consultas_o3(Driver JDBC de Ideasoft que nos proporciono conectividad con el servidor)

3 - chatboto3(Prototipo de chatbot utilizando la API de Claude para integrar a futuro con Ideasoft)
