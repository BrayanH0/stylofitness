# Manual de Arranque — StyloFitness

## Requisitos previos

Instalar antes de continuar:

| Herramienta | Version | Descarga |
|---|---|---|
| Java JDK | 21 | https://adoptium.net/ |
| Node.js | 18 o superior | https://nodejs.org/ |
| Oracle Database XE | 21c | https://www.oracle.com/database/technologies/xe-downloads.html |
| Angular CLI | 17 | `npm install -g @angular/cli` |

---

## 1. Configurar la base de datos Oracle

Conectate como SYSDBA y ejecuta:

```sql
CREATE USER gym IDENTIFIED BY 123456;
GRANT CONNECT, RESOURCE, CREATE SESSION TO gym;
GRANT UNLIMITED TABLESPACE TO gym;
```

Luego carga los datos de prueba (opcional pero recomendado):

```
sqlplus gym/123456@localhost:1521/xe < seed_data.sql
```

---

## 2. Variables de entorno del backend

El backend necesita estas variables de entorno para arrancar. Hay dos formas de configurarlas:

### Opcion A — Editar el archivo .env (recomendado)

Abre el archivo `.env.example`, copialo como `.env` y completa los valores:

```
JWT_SECRET=MiClaveSecretaParaJWTGym2024QueDebeSerDeAlMenos256BitsDeLongitudParaHS256
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USERNAME=gym
DB_PASSWORD=123456
STRIPE_SECRET_KEY=sk_test_TU_CLAVE_STRIPE_AQUI
STRIPE_WEBHOOK_SECRET=whsec_TU_WEBHOOK_SECRET_AQUI
APP_FRONTEND_URL=http://localhost:4200
```

> Obtén tus claves en https://dashboard.stripe.com/test/apikeys (modo TEST).

### Opcion B — Setear variables en la terminal (Windows CMD)

```cmd
set JWT_SECRET=MiClaveSecretaParaJWTGym2024QueDebeSerDeAlMenos256BitsDeLongitudParaHS256
set DB_URL=jdbc:oracle:thin:@localhost:1521:xe
set DB_USERNAME=gym
set DB_PASSWORD=123456
set STRIPE_SECRET_KEY=sk_test_TU_CLAVE_STRIPE_AQUI
set STRIPE_WEBHOOK_SECRET=whsec_TU_WEBHOOK_SECRET_AQUI
set APP_FRONTEND_URL=http://localhost:4200
```

### Opcion C — Usar el script incluido

Edita `start_backend.bat` con tus valores y ejecutalo directamente.

---

## 3. Arrancar el backend

```cmd
cd gym_back\gym
mvnw.cmd spring-boot:run
```

Primera vez descarga dependencias Maven (~2-3 minutos). Las siguientes veces es rapido.

El backend queda disponible en: **http://localhost:8080**

Verificar que esta corriendo: abrir http://localhost:8080/api/auth/login en el navegador debe devolver un error 405 (Method Not Allowed) — eso confirma que esta activo.

---

## 4. Arrancar el frontend

Abrir otra terminal (el backend debe seguir corriendo):

```cmd
cd proy_gym\gym_front
npm install
npm start
```

`npm install` solo es necesario la primera vez o cuando cambia `package.json`.

El frontend queda disponible en: **http://localhost:4200**

---

## 5. Cuentas de prueba

Estas cuentas estan en `seed_data.sql`. Despues de cargarlo:

| Rol | DNI | Descripcion |
|---|---|---|
| Admin | 99999999 | Acceso total al dashboard |
| Personal (Trainer) | 88888888 | Gestion de clases |
| Cliente | 33333333 | Usuario estandar |
| Cliente | 55555555 | Usuario estandar |
| Cliente | 66666666 | Usuario estandar |

La password de cada cuenta esta definida en `seed_data.sql`.

---

## 6. Tarjetas de prueba Stripe

Para probar pagos en modo test:

| Resultado | Numero de tarjeta | Fecha | CVV |
|---|---|---|---|
| Pago exitoso | 4242 4242 4242 4242 | Cualquier fecha futura | Cualquier |
| Pago rechazado | 4000 0000 0000 0002 | Cualquier fecha futura | Cualquier |

---

## 7. Puertos por defecto

| Servicio | URL |
|---|---|
| Backend (Spring Boot) | http://localhost:8080 |
| Frontend (Angular) | http://localhost:4200 |
| Oracle DB | localhost:1521 — SID: xe |

---

## Problemas comunes

**Error: puerto 8080 ocupado**
```cmd
netstat -ano | findstr :8080
taskkill /F /PID <numero_de_pid>
```

**Error de conexion Oracle**
- Verificar que el servicio OracleServiceXE este corriendo en Servicios de Windows
- Verificar usuario/password con: `sqlplus gym/123456@localhost:1521/xe`

**npm install falla**
- Verificar version de Node: `node --version` (debe ser 18+)
- Borrar `node_modules` y `package-lock.json` y volver a ejecutar `npm install`

**El backend arranca pero el frontend no conecta**
- Verificar que `APP_FRONTEND_URL` en las variables apunta a `http://localhost:4200`
- Revisar CORS en la consola del navegador (F12)
