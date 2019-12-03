# Extensiones para App Inventor

El código fuente de las extensiones se encuentra en:

```
appinventor/components/src/fvarrui/appinventor/components
```

## Extensiones disponibles

| Componente | Visible | Descripción                                                  |
| ---------- | ------- | ------------------------------------------------------------ |
| Sleepyhead | No      | Añade un retardo (en milisegundos) en la ejecución de bloques. |

## Compilar las extensiones

Requisitos para poder compilar el código:

- **Java 8+**
- **Ant 1.10+**

Clonar el repositorio:

```bash
git clone https://github.com/fvarrui/AppInventor-Extensions.git
```

Cambiar al directorio `AppInventor-Extensions/appinventor`:

```bash
cd AppInventor-Extensions/appinventor
```

Ejecutar `ant` para compilar las extensiones:

```bash
 ant clean extensions
```

Esto generará el fichero `fvarrui.appinventor.components.aix` en `appinventor/components/build/extensions`, que podremos importar en nuestro proyecto de App Inventor para poder usar los nuevos componentes.

> Cambiar en las rutas `/` por `\` en Windows.
