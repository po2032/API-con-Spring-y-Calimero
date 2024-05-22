# Ejemplo uso Calimero y Spring

Ejemplo sencillo y no óptimo de integración de la librería [Calimero](https://github.com/calimero-project/) 
Se basa en el uso del proyecto de [herramientas](https://github.com/calimero-project/calimero-tools) de Calimero, el proyecto de [introducción](https://github.com/calimero-project/introduction) y las librerías de herramientas y core para usarlas desde Spring


# Preparando en el entorno

Clonar los proyectos siguientes:
- https://github.com/calimero-project/calimero-tools (Ejecución de comandos contra KNX desde línea de comandos).
- https://github.com/calimero-project/introduction (Para iniciar un dispositivo KNX virtual).

Arrancar un dispositivo  en el proyecto de introducción:

> `./gradlew run -DmainClass=PushButtonDevice`

Ahora ya se puede ejecutar comandos con la herramienta "Calimero Tools" **(Abrir un nuevo terminar y entrar en el proyecto de tools)**. Los comandos posibles se pueden ver en la documentación
- **discover**, Discover KNXnet/IP devices, search

- **describe**, KNXnet/IP device self-description, describe

- **scan**, Determine the existing KNX devices on a KNX subnetwork

- ipconfig, KNXnet/IP device address  configuration

- **monitor**, Open network monitor (passive) for KNX network traffic

- **read**, Read a value  using KNX process communication, read

- **write**, Write a value  using KNX process communication, write

- groupmon, Open group monitor for KNX process communication, monitor

- trafficmon, KNX link-layer traffic & link status monitor

- get, Read a KNX property, get

- set, Write a KNX property, set

- properties, Open KNX property client,

- info, Send an LTE info command, info

- baos, Communicate with a KNX BAOS device

- devinfo, Read KNX device information

- mem, Access KNX device memory

- progmode, Check/set device(s) in programming mode

- restart, Restart a KNX interface/device

- import, Import datapoints from a KNX project (.knxproj) or group addresses file (.xml|.csv)mentación, son:

Algunos ejemplos de ejecución:

Información del dispositivo:

> gradle run --args "devinfo 224.0.23.12 1.1.10"

Escanear red:

> gradle  run --args "groupmon 224.0.23.12"

Leer estado de grupo:

>  gradle  run --args "read 1/0/3 switch 224.0.23.12"

Escribir estado del grupo:

> gradle  run --args "write 1/0/3 switch on 224.0.23.12"



# Proyecto

El código que se proporciona usa las librerías calimero tools y calimero core, se encuentran en la carpeta libs del proyecto.

Se ha creado unos cuantos "entry point" en la clase  NetworkKNX, en la subruta "/network", estos método son unas pruebas conceptuales para comprobar la integración entre Spring y Calimero.
Los "entry points" son:

- /network/search: Escanea la red devolviendo los dispositivos de la red KNX.
- /network/infodevice: Obtiene información del dispositivo 1/1/10
- /network/on: Activa el pulsador del grupo 1/0/3.
- /network/of: Desactiva el pulsador del grupo 1/0/3.
- /network/state: Obtiene en estado del pulsador (on/off) del grupo 1/0/3.

Para probar la aplicación, es necesario que se tenga funcionando el ejemplo de pulsador del proyecto "introduction":

./gradlew run -DmainClass=PushButtonDevice`

Arrancar el servidor web desde gradle, con la tarea bootRun, abrir el navegador y probar en localhost:8080 o 127.0.0.1:8080 los "entry points" anteriores.

En el desarrollo, se pueden comprobar las ejecuciones comparando con la ejecución con "tools".

# Problema IP y dispositivo.

Cada dispositivo KNXIP ha de tener una ip y un puerto, el puerto por defecto es el 3671, con lo que en el equipo solo se puede tener un dispositivo.
Una posible solución en crear adpatadores de red virtuales (virtualbox, vmware, docker...) y usarlos para iniciar en es adaptador otro dispositivo (** tiene una IP en la red principal**)
![Adaptadores de red](https://github.com/pass1enator/CalimeroSpringBasico/blob/master/Conexiones_de_red.png " adaptadores de red")

![Ejemplo configuración](https://github.com/pass1enator/CalimeroSpringBasico/blob/master/configuracion.png " ejemplo configuración")
Se ha creado una clase sencilla, a partir de los ejemplos: PushButtonDevice que gestiona la creación de un dispostivo dándole alguna de las IP de los adaptadores creados, de forma que se pueda tener varios (se ha creado también la clase Light, que es igual, para intentar tener un entorno gráfico.)

En el main se crean dos dispositivos en adaptadores diferentes y se lanzan, al igual que el servidor web:

```java
Light l1 = new Light(1,1,40," Ejemplo 1","192.168.1.132");
Light l2= new Light(1,1,12," Ejemplo 2","192.168.1.10");
l1.start();
l2.start();
SpringApplication.run(App.class, args);
```
