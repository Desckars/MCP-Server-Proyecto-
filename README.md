Para mandar una consulta a Gemini desde el servidor MCP hay que modificar en la carpeta resources el archivo application.properties
en la seccion de "google.gemini.api-key" camiar por la Api-Key de tu proyecto de Gemini.
Esta la consigues de la siguiente forma:
Inicias sesion en Google AI Studio y entras a este link" https://aistudio.google.com/apikey?hl=es-419&_gl=1*177foj1*_ga*MjExNDEwMjU2Mi4xNzU2MzI4NDEz*_ga_P1DBVKWT6V*czE3NTY5NTI0MzMkbzIkZzAkdDE3NTY5NTI0MzQkajU5JGwwJGg3MTIyNDQwODQ."
O vas al apartado de Get API Key.
Ahi hay un boton que dice +crear clave de API y la clave generada es la que ira en ese parte.

Para ejecutar el servidor MCP
Necesitas tener Node.js en las variables de entorno del sistema(esto en Windows) asi como
Maven 
JDK
Ejecutar:
mvn clean package
Y despues:
mvn spring-boot:run
