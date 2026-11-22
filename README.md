# Modela 3D

Aplicación Android nativa, offline, para convertir una imagen en un relieve 3D texturizado y exportarlo como paquete ZIP con OBJ, MTL y PNG. Usa OpenGL ES 2.0 y APIs Android; no llama a servidores ni requiere una cuenta.

## Uso
1. Abre la app y toca **Imagen**.
2. Elige una foto. La malla se crea en el dispositivo; arrastra para rotar y pellizca para acercar/alejar.
3. Ajusta el relieve con el control inferior y toca **Exportar** para guardar `modelo-3d.zip`.

## Alcance honesto
Una sola vista 2D no contiene el reverso ni la geometría oculta del objeto. Esta versión crea una placa/relieve cuya altura se estima localmente a partir de luminancia y distancia al centro, conservando la imagen como textura. No es reconstrucción volumétrica con IA ni produce una figura cerrada lista para impresión 3D. Los archivos OBJ/MTL/PNG van juntos en un ZIP.

## APK
GitHub Actions compila un APK de depuración en cada push a `main` y permite ejecución manual. El artefacto se llama `Modela3D-debug-apk`. El build no sustituye la prueba táctil en un teléfono físico.
