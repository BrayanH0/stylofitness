# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This is **not a single app** — it's two independently versioned projects with no shared root tooling (no root `package.json`, no monorepo manager):

- `gym_back/gym/` — Spring Boot 3.5.7 REST API (Java 21, Maven)
- `proy_gym/gym_front/` — Angular 17 SPA (standalone components, Tailwind + Bootstrap)
- `seed_data.sql` (repo root) — test users/data for the Oracle DB
- `MANUAL_ARRANQUE.md` (repo root, Spanish) — full local setup guide; read it before debugging environment issues

There is no CI config and no root build script — each project is run independently from its own directory.

---

## Commands

### Backend (`gym_back/gym/`)

```cmd
.\mvnw.cmd spring-boot:run                                                    REM run dev server (port 8080)
.\mvnw.cmd clean package                                                      REM build jar
.\mvnw.cmd test                                                               REM run all tests
.\mvnw.cmd test -Dtest=ClaseServiceTest                                       REM run a single test class
.\mvnw.cmd test -Dtest=ClaseServiceTest#testCreateClaseRechazaSolapamiento    REM single test method
```

> **Windows CMD requires `.\` prefix** — `mvnw.cmd` alone fails with "no se reconoce". Always use `.\mvnw.cmd`.
>
> **`.mvn/wrapper/maven-wrapper.properties` was missing** — created 2026-06-17. The wrapper downloads Maven 3.9.11. If the wrapper fails, use the cached Maven directly: `C:\Users\USUARIO\.m2\wrapper\dists\apache-maven-3.9.11\a2d47e15\bin\mvn.cmd test` with `JAVA_HOME=C:\Program Files\Java\jdk-21`.

Tests use JUnit 5 + Mockito and live under `src/test/java/com/gym/`.

### Frontend (`proy_gym/gym_front/`)

```cmd
npm install                                            REM required first time or after clone — node_modules missing otherwise
npm start                                              REM ng serve, port 4200
npm run build                                          REM production build (ng build)
npx ng test --watch=false --browsers=ChromeHeadless    REM run all tests headless (one-shot, CI-friendly)
ng test --include='**/auth.service.spec.ts'            REM single spec file
```

> **`npm install` must run before `npm start`** — if `node_modules` is absent, `ng serve` fails with "Could not find '@angular/devkit/build-angular:dev-server' builder's node package".

---

## Architecture

### Backend: layered Spring Boot, JWT-stateless, Oracle

Standard layering under `com.gym`: `controllers` → `services` → `repository` (Spring Data JPA) → `models` (JPA entities). `DTO` holds request/response shapes decoupled from entities; `security` holds the JWT pipeline; `config` holds `SecurityConfig` and `DataInitializer`; `exceptions` has a single `@RestControllerAdvice` (`GlobalExceptionHandler`) translating `IllegalArgumentException`/`ResourceNotFoundException`/`BadRequestException`/validation errors into a consistent JSON error shape.

#### Dependencies (pom.xml)

- Spring Boot 3.5.7, Java 21
- `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-devtools`
- `ojdbc11` — Oracle JDBC
- `stripe-java` v24.7.0
- JWT: `jjwt-api/impl/jackson` v0.11.5
- `@EnableScheduling` active on `GymApplication.java` for cron tasks

#### Auth

`JwtAuthenticationFilter` (runs before `UsernamePasswordAuthenticationFilter`) validates the bearer token and populates the security context; `JwtUtil` issues/parses tokens carrying `dni`, `nombre`, `rol` claims (24h validity, HS256). Passwords are BCrypt-hashed. Roles are plain strings stored on `Usuario.rol`: `ADMIN`, `PERSONAL`, `CLIENTE`. There is no Spring `UserDetailsService`/roles table — authorization checks in `SecurityConfig` use `hasAuthority("ROLE_...")`/`hasAnyAuthority(...)`, while controller-level checks (e.g. `ClaseController`, `PaymentController`) often pull the role straight out of the JWT via `JwtUtil.extractRole` instead of relying on the security context — when changing access rules, check both places.

Special tokens: `JwtUtil.generateTempToken(dni, 15*60*1000)` emits a 15-min token with claim `purpose=post-payment-activation`, validated separately by `validateTempToken()`. Used in pre-registro → pago → exito flow.

`JwtEntryPoint` returns 401 `{error, mensaje}` for unauthenticated requests; `JwtAccessDeniedHandler` returns 403.

#### Per-route authorization

Centralized in `SecurityConfig.filterChain` (path-pattern allow/deny lists) — this is the source of truth for what's public vs authenticated vs role-gated, independent of any `@PreAuthorize` annotations (there are none).

**Public endpoints (no auth required):**
- `/api/auth/**` (login, validate-token, post-pago-login)
- `/api/usuario/pre-registro`, `/api/usuario/existe-dni/**`, `/api/usuario/existe-email`, `/api/usuario/existe-telefono`
- `/api/stripe/webhook`, `/api/webhook/**`
- `/api/payment/create-checkout-session/**`, `/api/payment/confirm`

**Role-gated:**
- `/api/admin/**` → `ROLE_ADMIN` only
- `/api/clases/**` → GET authenticated, POST/PUT/DELETE `ROLE_ADMIN` or `ROLE_PERSONAL`
- `/api/personal/**` → `ROLE_ADMIN` or `ROLE_PERSONAL`
- `/api/inscripciones/**`, `/api/payment/**` → any authenticated

#### DataInitializer

`DataInitializer` seeds a default ADMIN (DNI `99999999`) and PERSONAL (DNI `88888888`) user on every startup if they don't already exist — separate from `seed_data.sql`, which is loaded manually into Oracle for additional test clients.

#### Persistence

Oracle XE via `ojdbc11`, `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-migrates the schema from entities — there's no Flyway/Liquibase). `src/main/resources/migration.sql` exists as a supplementary manual script, not an auto-run migration tool.

#### Config

`application.properties` currently has real credentials and Stripe test keys hardcoded directly, even though `MANUAL_ARRANQUE.md` documents an env-var/`.env` based setup and `application.properties.example` shows placeholders. Prefer the documented env-var pattern:

```
JWT_SECRET=...                   (256+ bits)
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USERNAME=gym
DB_PASSWORD=123456
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
APP_FRONTEND_URL=http://localhost:4200   (dev) or https://... (prod)
```

Key properties: `spring.jpa.show-sql=true`, `spring.jpa.properties.hibernate.format_sql=true`.

---

### Backend: Models (JPA Entities)

**`Usuario`** — table `USUARIO`, PK = `dni` (Long, not generated):
- `dni`: @Id, 8-digit, validated @Min/@Max
- `nombre`, `apellido`: Pattern `^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$`, max 50 chars
- `telefono`: Pattern `^9[0-9]{8}$` (9 digits, starts with 9)
- `email`: @Email
- `fecha_nacimiento`: String `YYYY-MM-DD` (age 18–100 validated in service)
- `direccion`: 5–200 chars
- `fecha_registro`: LocalDate
- `estado`: `ACTIVO`/`INACTIVO`
- `passwordHash`: BCrypt, pattern `^(?=.*[A-Z])(?=.*[0-9]).{8,}$`
- `rol`: `ADMIN`/`PERSONAL`/`CLIENTE`
- `fecha_contratacion`: LocalDate (staff only)
- `@JsonIgnoreProperties({"passwordHash", "password"})`

**`Clase`** — table `CLASES`, PK = `idClase` (auto-increment):
- `nombre`: max 100 chars
- `fechaClase`: java.sql.Date
- `horai`, `horaf`: Pattern `HH:mm`
- `idTrainer`: Long → must reference a `PERSONAL` user
- `estado`: `ACTIVO`/`FINALIZADA`/`INACTIVO`
- `cupo`: 10–30

**`InscripcionClase`** — table `INSCRIPCION_CLASE`, PK = `idInscripcion` (auto-increment):
- `dniUsuario`: Long, `idClase`: Long
- `fechaInscripcion`: LocalDateTime
- `estado`: `ACTIVA`/`CANCELADA`/`FINALIZADA`

**`MembresiaUsuario`** — table `MEMBRESIA_USUARIO`, PK = `id` (auto-increment):
- `dni`: Long (not a FK — allows deleting Usuario without cascading)
- `id_membresia`: Long (`1`=Fit/Basic, `2`=Black/Premium)
- `fecha_inicio`, `fecha_fin`: LocalDate
- `estado`: `ACTIVA`/`VENCIDA`/`CANCELADA`

**`Pago`** — table `PAGOS`, PK = `id` (sequence `PAGO_SEQ`):
- `paymentIntentId`, `sessionId`: String
- `monto`: double, `moneda`: String (`PEN`)
- `email`: String
- `fecha`: LocalDateTime
- `estado`: `COMPLETADO`/`PENDIENTE`
- `userId`: String (DNI as string)
- `plan`: String (`fit_1m`/`fit_3m`/`black_1m`/`black_3m`/`basic`/`premium`)

---

### Backend: Repositories (Spring Data JPA)

**`UsuarioRepository`**:
- `existsByDni(Long)`, `existsByEmail(String)`, `existsByTelefono(String)`
- `findByDni(Long)` — `@Query` native `SELECT * FROM USUARIO WHERE dni = :dni`
- `findByRol(String)`
- `countByEstado(String)`, `countByRol(String)`, `countByFechaRegistroBetween(start, end)`

**`ClaseRepository`**:
- `findByIdTrainer(Long)`
- `findByIdTrainerAndFechaClase(idTrainer, fecha)`
- `findByFechaClaseBeforeAndEstadoEquals(fecha, estado)` — clases pasadas
- `findByFechaClaseAndEstadoEquals(fecha, estado)` — clases hoy

**`InscripcionClaseRepository`**:
- `findByDniUsuario(Long)`, `findByIdClase(Long)`
- `findByDniUsuarioAndIdClase(dni, idClase)`
- `findByDniUsuarioAndIdClaseAndEstado(dni, idClase, estado)`
- `findByDniUsuarioAndEstado(dni, estado)`
- `countByIdClase(Long)`, `countByIdClaseAndEstado(idClase, estado)`
- `existsByDniUsuarioAndIdClase(dni, idClase)`
- `existsByDniUsuarioAndIdClaseAndEstado(dni, idClase, estado)`

**`MembresiaUsuarioRepository`**:
- `findByDni(Long)`
- `countByEstado(String)`
- `findByFechaFinBetween(start, end)` — membresías próximas a vencer
- `countByIdMembresia(Long)` — plan distribution

**`PagoRepository`**:
- `findTopByUserIdOrderByFechaDesc(userId)`
- `existsBySessionId(String)`
- `countByEstado(String)`
- `countPagosCompletados()` — `@Query` COUNT WHERE estado IN ('COMPLETADO', ...)
- `ingresosMensuales()` — `@Query` GROUP BY YEAR, MONTH, SUM(monto)
- `totalIngresos()`, `ingresosMesActual()` — `@Query` SUM

---

### Backend: Services

**`UsuarioService`**:
- `crear_usuario(Usuario)` — synchronized; validates DNI/email/phone unique, age 10–100, BCrypt password; defaults rol=CLIENTE, estado=INACTIVO
- `crear_personal(Usuario)` — same but rol=PERSONAL, estado=ACTIVO, fecha_contratacion=now()
- `login(dni, password)` — verifies password, requires ACTIVO
- `actualizar(usuario)`, `eliminar_usuario(dni)`, `obtenerPorDni(dni)`, `listarPersonal()`
- `existeDni/Email/Telefono()`

**`ClaseService`**:
- `createClase(Clase)` — synchronized; validates trainer is PERSONAL, date not past, future time if today, start time ≥ 06:00, duration ≥ 30 min, no trainer overlap; defaults estado=ACTIVO
- `updateClase(id, data)` — same validations including start ≥ 06:00
- `deleteClase(id)` — soft delete: marks INACTIVO, cancels all ACTIVA inscriptions → CANCELADA
- `validarSolapamiento(trainerId, fecha, horai, horaf, excludeId)` — private, checks overlap by trainer/day/time
- `@Scheduled(cron="0 0 0 * * ?")` `finalizarClasesPasadas()` — daily at midnight, marks past classes FINALIZADA and their inscriptions FINALIZADA

**`InscripcionClaseService`**:
- `inscribirUsuario(dni, idClase)` — synchronized, `@Transactional`; validates clase ACTIVA, not past/in-progress, user has ACTIVA membresia, no duplicate, cupos available, no schedule conflict with user's other ACTIVA inscriptions on same date/time
- `cancelarInscripcion(dni, idClase)` — `@Transactional`; marks CANCELADA; rejects if clase en curso
- `eliminarInscripcion(idInscripcion)` — `@Transactional`, hard delete
- `getCuposDisponibles(idClase)`, `isInscrito(dni, idClase)`, `obtenerEstadoTemporal(clase)` (FUTURA/EN_CURSO/PASADA)

**`MembresiaUsuarioService`**:
- `crearMembresiaPorPlan(dni, plan)` — maps plan string to idMembresia and duration:
  - `basic`/`fit_1m` → idMembresia 1, 1 mes
  - `fit_3m` → idMembresia 1, 3 meses
  - `black_1m`/`premium` → idMembresia 2, 1 mes
  - `black_3m` → idMembresia 2, 3 meses
- synchronized; rejects if user already has ACTIVA membresia
- `crearMembresia(dni, idMembresia)` — 1 month
- `crearMembresiaConDuracion(dni, idMembresia, meses)` — 1–3 months
- `obtenerMembresiActivaDelUsuario(dni)` — filters ACTIVA + fecha_fin >= today, returns max by fecha_inicio

**`PagoService`**:
- `guardarPagoDeSesion(Session)`, `guardarPagoDePaymentIntent(PaymentIntent)` — synchronized, `@Transactional`, deduplicates by sessionId/piId
- `guardarPagoManual(...)` — 4 overloads with optional plan/dni
- `activarUsuarioPorDni(dni)` — sets usuario.estado = ACTIVO
- `obtenerUltimoPagoPorDni(dniStr)`, `obtenerTodosPagos()`

---

### Backend: Controllers

**`AuthController`** (`/api/auth/`):
- `POST /login` — validates 8-digit DNI, returns `LoginResponseDTO` {token, tipo="Bearer", dni, nombre, apellido, email, telefono, estado, rol}. 401 on bad creds or inactive.
- `POST /validate-token` — Bearer header, returns {valid, dni, nombre, rol}
- `POST /post-pago-login` — body {dni, tempToken}; validates tempToken purpose, returns full JWT

**`UsuarioController`** (`/api/usuario/`):
- `GET /` — all users (without passwordHash)
- `GET /existe-dni/{dni}`, `GET /existe-email?email=`, `GET /existe-telefono?telefono=` — {existe: boolean}
- `POST /pre-registro` — creates Usuario INACTIVO, returns `PreRegistroResponse` with tempToken (15 min)
- `PUT /activar/{dni}` — activates user (requires ADMIN or PERSONAL)
- `PUT /perfil/{dni}` — `UsuarioUpdateDTO`, validates ownership or admin, validates currentPassword if changing password

**`AdminController`** (`/api/admin/`, requires `ROLE_ADMIN`):
- Full CRUD usuarios and personal
- `GET /estadisticas/completas` — {totalUsuarios, usuariosActivos, totalPersonal, totalClases, clasesActivas, totalInscripciones, inscripcionesActivas, membresiasActivas, membresiasPorVencer, ingresosTotales, ingresosMesActual, pagosCompletados, planBasico, planPremium, nuevosUsuariosMes}
- `GET /estadisticas/ingresos-mensuales` — [{mes: "YYYY-MM", ingresos}]
- `GET /estadisticas/clases-populares` — [{idClase, nombre, inscripciones, estado}] sorted desc
- `GET /membresias` — [{id, dni, plan, fechaInicio, fechaFin, estado, nombreUsuario}]

**`ClaseController`** (`/api/clases/`):
- `GET /` — all; `GET /trainer/{idTrainer}`; `GET /{id}`
- `POST /` — create (ADMIN/PERSONAL), returns 201
- `PUT /{id}` — update (ADMIN/PERSONAL)
- `DELETE /{id}` — soft delete (ADMIN/PERSONAL)

**`InscripcionController`** (`/api/inscripciones/`):
- `GET /usuario/{dni}` — user's inscriptions (only self or ADMIN/PERSONAL)
- `GET /usuario/{dni}/activas` — active only
- `GET /clase/{idClase}` — enrolled users (ADMIN/PERSONAL)
- `GET /clase/{idClase}/cupos` — {cuposDisponibles}
- `GET /verificar/{dni}/{idClase}` — {inscrito: boolean}
- `POST /inscribir?dniUsuario=X&idClase=Y` — returns 201 with idInscripcion
- `PUT /cancelar?dniUsuario=X&idClase=Y`
- `DELETE /{idInscripcion}` — hard delete (ADMIN/PERSONAL)

**`PaymentController`** (`/api/payment/`):
- `POST /create-checkout-session/{plan}?dni=X` — plans: `fit_1m`/`fit_3m`/`black_1m`/`black_3m`/`basic`/`premium`. Prices hardcoded in centavos (PEN). Returns Stripe Checkout URL.
- `POST /confirm?sessionId=X` — sync fallback post-payment: verifies session paid, saves Pago, creates Membresia, activates Usuario. `@Transactional`.
- `GET /last-payment?dni=X` — last payment (only owner or ADMIN/PERSONAL)
- `GET /all` — all payments (ADMIN/PERSONAL)

**`StripeWebhookController`** (`/api/stripe/`):
- `POST /webhook` — handles `checkout.session.completed` and `payment_intent.succeeded`. Validates webhook signature. Saves Pago, creates Membresia, activates Usuario. Synchronized to avoid duplicates.
- `GET /membresia-activa/{dni}` — returns active MembresiaUsuario (only owner or ADMIN/PERSONAL), 204 if none

#### Payments: dual-path design

`PaymentController.confirm` (sync, post-redirect) and `StripeWebhookController` (async) both call into `PagoService`/`MembresiaUsuarioService` and can each activate a user. This is intentional redundancy: webhooks may not reach localhost in dev. Both paths are deduplicated by `sessionId`.

---

### Backend: DTOs

- `LoginRequest`: `{dni @Pattern("^[0-9]{8}$"), password @NotBlank}`
- `LoginResponseDTO`: `{token, tipo="Bearer", dni, nombre, apellido, email, telefono, estado, rol}`
- `PreRegistroResponse`: `{tempToken, mensaje, dni, nombre, apellido, email, telefono, estado}`
- `UsuarioUpdateDTO`: optional fields nombre, apellido, email, telefono, direccion, fecha_nacimiento, currentPassword, newPassword, estado, rol
- `PasswordChangeDTO`: `{currentPassword @NotBlank, newPassword @NotBlank @Pattern}`

---

### Backend: Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`):
- `MethodArgumentNotValidException` → 400, `{timestamp, status, error, errors{fieldName: message}}`
- `ResourceNotFoundException` → 404, `{timestamp, status, error, message, path}`
- `BadRequestException` → 400 (business errors: membresía inactiva, clase llena, etc.)
- `IllegalArgumentException` → 400
- `HttpMediaTypeNotSupportedException` → 415
- `HttpMessageNotReadableException` → 400
- `RuntimeException`, `Exception` → 500

---

### Backend: Tests

Under `src/test/java/com/gym/`:

| File | What it tests |
|---|---|
| `ClaseServiceTest` | create clase, overlap rejection, past dates, min duration |
| `InscripcionClaseServiceTest` | enrollment, membresia required, cupos, temporal state |
| `MembresiaUsuarioServiceTest` | create membresia, fecha_fin, states |
| `PagoServiceTest` | save payments, duplicate prevention |
| `UsuarioServiceTest` | create usuario, age validation, unique DNI |
| `JwtUtilTest` | generate/validate tokens, extract claims |
| `ClaseControllerAuthTest` | authorization on endpoints |
| `StripeWebhookControllerTest` | webhook processing |

---

### Frontend: Angular 17 standalone, JWT in localStorage

No `NgModule`s — bootstrapped via `app.config.ts` (`ApplicationConfig`) with `provideRouter`, `provideHttpClient(withFetch(), withInterceptors([AuthInterceptor]))`. Routes (`app.routes.ts`) lazy-load every page via `loadComponent()`.

#### Key versions (package.json)

- Angular 17.3.0, TypeScript 5.3.3
- Bootstrap 5.3.3, Tailwind 3.4.19, PostCSS 8.5.14
- Chart.js 4.5.1 (admin dashboard graphs)
- html2canvas 1.4.1 + jspdf 3.0.4 (PDF export in admin)

#### TypeScript config

- `strict: true`, `noImplicitReturns: true`, `noFallthroughCasesInSwitch: true`
- `strictTemplates: true` in angularCompilerOptions

#### Routes (app.routes.ts)

| Path | Component | Guards |
|---|---|---|
| `/` | Principal | — |
| `/interfaz-usuario` | InterfazUsuarioComponent | [AuthGuard] |
| `/personal` | InterfazPersonalComponent | [AuthGuard, PersonalGuard] |
| `/admin` | AdminDashboardComponent | [AuthGuard, AdminGuard] |
| `/ubicacion` | UbicacionComponent | — |
| `/registrar` | RegistrarComponent | — |
| `/pago` | PagoComponent | — |
| `/exito` | ExitoComponent | — |
| `/cancel` | CancelComponent | — |
| `**` | redirect `/` | — |

#### Auth state

Lives entirely in `localStorage` (`token`, `dni`, `nombre`, `rol`, `usuario` blob) — no NgRx/state service. `AuthService` reads/writes directly; `token.helper.ts` provides `isTokenValid`/`clearAuthStorage` used by guards.

Full localStorage keys in `token.helper.ts`:
- `AUTH_KEYS = ['token', 'dni', 'nombre', 'apellido', 'email', 'rol', 'usuario']`
- `SESSION_KEYS = ['datosCliente', 'membresiaSeleccionada', 'ultimoPago', 'tempPassword', 'tempToken']`

#### Guards

- `AuthGuard` — valid JWT, redirects `/` if not
- `AdminGuard` — requires `rol === ADMIN`
- `PersonalGuard` — requires `rol === PERSONAL`

#### AuthInterceptor (functional interceptor)

Attaches `Bearer <token>` to every request except hardcoded `publicEndpoints`:
```typescript
['/api/auth/login', '/api/usuario/pre-registro', '/api/usuario/existe-dni',
 '/api/payment/create-checkout-session', '/api/payment/confirm']
```
Must stay in sync with `SecurityConfig`'s public matchers — they encode the same policy independently on each side.

#### api-config.ts

Single source of truth for all backend endpoints. Add new endpoints here, never inline URLs in services/components.

Covers: `AUTH_*`, `USUARIO_*`, `ADMIN_*`, `CLASES_*`, `INSCRIPCIONES_*`, `PAYMENT_*`, `MEMBRESIA_ACTIVA`.

#### Environments

- `environment.ts`: `{ apiUrl: 'http://localhost:8080', production: false }`
- `environment.prod.ts`: `{ apiUrl: 'http://localhost:8080', production: true }` — note: apiUrl should point to the deployed backend in real prod

---

### Frontend: Services

**`AuthService`**:
- `login(dni, password)` → Observable, saves token/dni/nombre/rol/usuario in localStorage
- `logout()` → `clearAuthStorage()`
- `isLoggedIn()`, `isAdmin()`, `isPersonalOrAdmin()`, `getUserRole()`, `getUserDni()`, `getUserName()`, `getUserInfo()`
- `existeDni(dni)`, `existeEmail(email)`, `existeTelefono(telefono)` — GET
- `validateToken()` → POST /api/auth/validate-token, returns Observable<boolean>

**`PaymentService`**:
- `createCheckoutSession(plan, dni?)` → POST, returns URL string (`responseType: 'text'`)
- `retomarPago(usuario)` in `AuthService` → POST `/api/usuario/retomar-pago`, sends full user object, returns `{tempToken, dni, nombre, mensaje}`

**`AdminService`**:
- CRUD usuarios: `getUsuarios()`, `getUsuario(dni)`, `actualizarUsuario(dni, data)`, `desactivarUsuario(dni)`, `activarUsuario(dni)`, `eliminarUsuario(dni)`
- `getUsuariosPorEstado(estado)`, `getEstadisticasUsuarios()`, `getEstadisticasCompletas()`, `getIngresosMensuales()`, `getClasesPopulares()`
- Personal: `getPersonal()`, `crearPersonal(personal)`, `actualizarPersonal(dni, data)`, `desactivarPersonal(dni)`, `eliminarPersonal(dni)`
- `getMembresias()`

**`LoginModalService`**:
- Subject `open$` observable — navbar subscribes to trigger modal open
- `open()` → emits event

---

### Frontend: Components

**`Navbar`** (shared):
- State: `loginOpen`, `menuOpen`
- Subscribes to `LoginModalService.open$`
- Conditionals: `isLoggedIn()`, `isAdmin()`, `isPersonal()`, `isPersonalOrAdmin()`

**`LoginModalComponent`**:
- Input: `role: 'usuario'|'personal'` = 'usuario'
- Output: `closed`
- Reactive form: DNI 8-digit pattern, password required
- On submit: calls `auth.login`, navigates by rol
- Features: modal focus trap (`modal-focus-trap.ts`), Escape closes, password toggle

**`Footer`** — static

**`modal-focus-trap.ts`** utility:
- `trapFocus(event: KeyboardEvent, container: HTMLElement)` — traps Tab/Shift+Tab within modal

---

### Frontend: Pages

**`Principal`** — Landing page:
- Plan cards: Fit and Black (prices, features, highlighted)
- `elegirPlan(planKey)` → navigates to `/registrar?plan=fit|black`

**`Registrar`** — Pre-registration:
- Validates: DNI 8 digits, email format, telefono 9 digits starting with 9, age 10–100
- Live duplicate checks: `existeDni`, `existeEmail`, `existeTelefono`
- On submit: saves `datosCliente` in localStorage (NO API call here) → navigates `/pago`
- If `existeDni` returns true → calls `POST /api/usuario/retomar-pago` with full form data → if user is INACTIVO (abandoned payment), updates their data in DB, returns new tempToken, saves `{dni, tempToken}` to localStorage, navigates `/pago` → if user is ACTIVO, shows error "inicia sesión"
- Query param `?plan=fit|black` pre-selects plan

**`Pago`** — Payment summary + Stripe redirect:
- Steps: 2 (summary), 3 (Stripe checkout)
- `crearCuentaYpagar()`: if `datosCliente.tempToken` already set (retomar path) → skip pre-registro, go directly to Stripe. If pre-registro fails with "DNI ya registrado" → calls `retomar-pago` to get tempToken, then Stripe.
- Reads `membresiaSeleccionada` from localStorage
- Calculates: subtotal, IGV (18%), total
- On submit: POST `/api/payment/create-checkout-session/{plan}?dni=X` → `window.location = url`

**`Exito`** — Post-payment confirmation:
- Reads `session_id` query param
- POST `/api/payment/confirm?sessionId=X` → activates user, creates membresia
- POST `/api/auth/post-pago-login {dni, tempToken}` → auto-login
- Navigates `/interfaz-usuario` (or `/admin`/`/personal` by role)

**`Cancel`** — Payment cancelled page

**`Ubicacion`** — Gym locations

**`InterfazUsuario`** — Client dashboard:
- Lists available classes with search/filter by date and time
- Enrollment/unenrollment with membership validation
- Shows active membership and enrolled classes

**`InterfazPersonal`** — Trainer dashboard:
- Monthly calendar (navigable)
- Create class modal (form with name, fecha, horai, horaf, cupo)
- List own classes with state filters
- Class state handling: ACTIVO, FINALIZADA, INACTIVO

**`AdminDashboard`** — Admin dashboard:
- Tabs: usuarios, personal, estadísticas, suscripciones
- Stats cards and Chart.js graphs (monthly revenue, popular classes)
- Full CRUD usuarios and personal
- Filters by estado, search by name
- PDF export via html2canvas + jspdf

---

### Frontend: Deployment

**`vercel.json`**:
```json
{
  "buildCommand": "npx ng build --configuration production",
  "outputDirectory": "dist/gym_front/browser",
  "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }],
  "headers": [
    { "source": "/assets/(.*)", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] },
    { "source": "/(.*)\\.js", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] },
    { "source": "/(.*)\\.css", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] }
  ]
}
```

#### Tailwind config

Custom tokens in `tailwind.config.js`:
- `primary: #162839`, `secondary: #006397`, `tertiary: #12283c`
- `accent: #e74c3c` (red), `success: #27ae60`, `warning: #f39c12`
- Font families: Lexend, Montserrat
- Custom spacing: `gutter: 24px`, `container_padding: 30px`

---

### Cross-cutting

CORS is configured once, backend-side, in `SecurityConfig.corsConfigurationSource()` (`localhost:4200`, `*.vercel.app`, `*.trycloudflare.com`, `*.cloudflare.com`). Frontend and backend are deployed/run independently; there's no reverse proxy tying them together locally — the frontend talks to `http://localhost:8080` directly in dev.

---

## Key flows

### Pre-registro → Pago → Activación

```
1. /registrar → form (DNI, nombre, email, teléfono, contraseña, plan)
2. POST /api/usuario/pre-registro → Usuario estado INACTIVO, returns tempToken (15 min)
3. localStorage: datosCliente, membresiaSeleccionada
4. /pago → resumen IGV + total
5. POST /api/payment/create-checkout-session/{plan}?dni=X → Stripe Session URL
6. window.location = URL (Stripe hosted checkout)
7. Success → /exito?session_id=X
8. POST /api/payment/confirm?sessionId=X → saves Pago, creates Membresia, activates Usuario
9. POST /api/auth/post-pago-login {dni, tempToken} → returns JWT
10. localStorage: token, rol → navigate /interfaz-usuario
```

### Login flow

```
1. Navbar "Ingresar" → opens LoginModal via LoginModalService.open$
2. Form: DNI + password
3. POST /api/auth/login → {token, rol, ...}
4. localStorage: token, rol, dni, nombre, usuario
5. Navigate /interfaz-usuario or /admin or /personal by rol
```

### Webhook (async, parallel to sync flow)

```
Stripe → POST /api/stripe/webhook
Backend: verifies signature → saves Pago → creates Membresia → activates Usuario
(Both paths deduplicated by sessionId/paymentIntentId)
```

---

## Gotchas & Design decisions

1. **`Usuario.dni` as PK (Long, not auto-generated)** — 8-digit constraint. All relations store DNI as Long. No FK from MembresiaUsuario or Pago to Usuario table — deleting a user leaves orphan rows.
2. **Roles as plain strings, not enum** — stored as `ADMIN`/`PERSONAL`/`CLIENTE`, manually validated in services.
3. **JWT without Spring UserDetailsService** — roles come from JWT claims, not DB. Role change is not immediately reflected in active tokens.
4. **Dual auth enforcement** — `SecurityConfig` (path-level) AND controller-level `JwtUtil.extractRole` checks. When changing auth rules, update both.
5. **Dual payment paths** — sync `/confirm` + async webhook. Both intentionally can activate a user, both deduplicated. In prod, webhook is the canonical path.
6. **Stripe prices hardcoded in centavos** — in `PaymentController`, not in DB. Change requires code redeploy.
7. **Soft deletes for classes** — `deleteClase` marks INACTIVO, does not remove. Also cancels all ACTIVA inscriptions. No restore path.
8. **Scheduled task midnight** — `@Scheduled(cron="0 0 0 * * ?")` in `ClaseService`. Requires `@EnableScheduling` on `GymApplication`. Marks past classes + their inscriptions FINALIZADA.
9. **Frontend localStorage as sole state** — no NgRx. Multiple-tab edge case not handled. Logout clears all AUTH_KEYS.
10. **AuthInterceptor public endpoint list** — hardcoded in frontend. Must be kept in sync with `SecurityConfig` public matchers; they encode the same policy independently.
11. **tempToken purpose claim** — `purpose=post-payment-activation` differentiates it from regular JWT. Only valid for `/api/auth/post-pago-login`, expires in 15 min.
12. **`application.properties` has real secrets** — despite `.example` and `MANUAL_ARRANQUE.md` showing env-var pattern. Do not commit or expose.
13. **`ddl-auto=update`** — Hibernate auto-migrates schema. `migration.sql` is supplementary/manual only.
14. **DataInitializer** — creates default admin (99999999) and personal (88888888) on every startup if absent. Password in `seed_data.sql`: Admin `Admin123!`, Personal `Personal123!`.
15. **`PaymentController` blocks `localhost:4200`** — was a misguided production guard (`frontendUrl.contains("localhost:4200")` returned 500). Fixed 2026-06-17: condition now only rejects null/blank. Do not re-add the localhost check.
16. **`app.frontend.url` must be `http://localhost:4200` for local dev** — if left as the Vercel URL, Stripe success/cancel redirects go to production instead of local, AND the localhost guard (see #15) blocks checkout session creation entirely.
17. **`InscripcionClaseService` estado check** — fixed 2026-06-17: was checking `"ACTIVA"` but classes are stored as `"ACTIVO"`. Now accepts both. If this breaks again, check `ClaseService.createClase` which stores `estado='activo'` (lowercase).
18. **Error messages in frontend** — `err?.error?.message` must be read BEFORE `err?.error?.error`. The `GlobalExceptionHandler` puts the generic label in `error` ("Solicitud incorrecta") and the specific message in `message`. Wrong order shows the generic label.
19. **`InscripcionController.getInscripcionesByClase`** — enriched 2026-06-17: now returns `nombreUsuario` (nombre + apellido) alongside inscription fields by joining `UsuarioRepository.findByDni`. Frontend modal uses `ins.nombreUsuario`.
20. **Registrar form age limit** — minimum age is 10 years (changed from 18 on 2026-06-17). `maxDate` calculated as `today - 10 years` in `registrar.ts ngOnInit`. Error message: "Debes tener entre 10 y 100 años."
21. **Registrar form validation UX** — submit button has no `[disabled]` attribute; `onSubmit` marks all controls `touched` first, then validates sequentially. Error divs are placed OUTSIDE the `div.relative` icon wrapper — if placed inside, `top-1/2` on the icon shifts when the div grows.
22. **`retomar-pago` endpoint** — added 2026-06-17: `POST /api/usuario/retomar-pago` (public, no auth). Accepts full `Usuario` body. Validates user exists AND is INACTIVO (returns 400 if ACTIVO). Updates nombre/apellido/direccion/fecha_nacimiento directly; checks uniqueness before updating email/telefono (only if changed vs current value). Re-hashes password. Returns `{tempToken, dni, nombre, mensaje}`. Used when user abandoned Stripe mid-payment — lets them update their data and retry without admin intervention. `SecurityConfig` must keep this in permitAll.
23. **pre-registro NOT called from `/registrar` form** — the form submit in `registrar.ts` only saves to localStorage and navigates. `POST /api/usuario/pre-registro` is called from `pago.component.ts` `crearCuentaYpagar()` when the user clicks PAGAR. Do not move it back to the form submit — that caused orphaned INACTIVO users on every abandoned registration.
24. **retomar-pago flow in `pago.component.ts`** — three paths in `crearCuentaYpagar()`: (1) `datosCliente.tempToken` already set → skip pre-registro, go straight to Stripe; (2) pre-registro succeeds → save tempToken → Stripe; (3) pre-registro fails "DNI ya registrado" → call `retomar-pago` → get tempToken → Stripe. Without a valid tempToken, `/exito` fails at `POST /api/auth/post-pago-login`.
25. **Age minimum is 10 years** — changed 2026-06-17 in both frontend (`registrar.ts maxDate`) and backend (`UsuarioService.validarEdad`). Error message: "Debes tener al menos 10 años". Do not revert to 18.
26. **Cupo máximo es 30** — changed 2026-06-17. `@Max(30)` in `Clase.java`, `> 30` check in `interfaz-personal.component.ts createClase()`, `max="30"` in the HTML input. Do not revert to 100.
27. **Restricción horaria clases 06:00** — `ClaseService.createClase` and `updateClase` reject start times before 06:00. Frontend also validates `horai < '06:00'`. Classes crossing midnight are already impossible: existing `fin > inicio` validation rejects any end time ≤ start (00:xx < 23:xx in LocalTime), so no extra upper-bound check needed.
28. **Conflicto horario por usuario en inscripción** — `InscripcionClaseService.inscribirUsuario` checks all ACTIVA inscriptions of the user on the same date. If any overlaps in time with the target class, throws `BadRequestException`: "Ya tienes inscrita la clase 'X' en ese horario. No puedes estar en dos clases a la vez." Uses inline `LocalTime` overlap logic (ini1 < fin2 && ini2 < fin1).

---

## Test users (local / Oracle XE)

| Rol | DNI | Password |
|---|---|---|
| ADMIN | `99999999` | `Admin123!` |
| PERSONAL | `88888888` | `Personal123!` |
| CLIENTE | `73957953` | `Sevenkeyne7` |

`99999999` and `88888888` are seeded by `DataInitializer` on every startup. `73957953` comes from `seed_data.sql`.

---

## Unit tests — current state (2026-06-17)

Reduced from 50 → **14 tests** keeping only business-critical cases. Smoke tests (`should create`) were deleted.

### Backend — 7 tests (`.\mvnw.cmd test` → BUILD SUCCESS)

| File | Method | What it guards |
|---|---|---|
| `ClaseServiceTest` | `testCreateClaseRechazaSolapamiento` | Trainer can't have two classes at overlapping times |
| `ClaseServiceTest` | `testCreateClaseRechazaDuracionMenor30Min` | Class must be ≥ 30 min |
| `InscripcionClaseServiceTest` | `testInscribirSinMembresia` | User without active membresía can't enroll |
| `InscripcionClaseServiceTest` | `testInscribirClaseLlena` | Full class rejects enrollment |
| `UsuarioServiceTest` | `testCrearUsuarioRechazaDNIDuplicado` | DNI uniqueness on registration |
| `UsuarioServiceTest` | `testLoginUsuarioInactivo` | INACTIVO user blocked from login |
| `PagoServiceTest` | `testGuardarPagoManualEvitaDuplicado` | Duplicate Stripe sessionId not saved twice |

### Frontend — 7 tests (`npx ng test --watch=false --browsers=ChromeHeadless` → TOTAL: 7 SUCCESS)

| File | Test description | What it guards |
|---|---|---|
| `auth.service.spec.ts` | Detectar sesión activa con token válido | `isLoggedIn()` returns true for live JWT |
| `auth.service.spec.ts` | Detectar sesión inactiva con token expirado | `isLoggedIn()` returns false for expired JWT |
| `auth.service.spec.ts` | Limpiar localStorage al hacer logout | `logout()` clears token + rol |
| `pago.component.spec.ts` | calcularTotales descompone precio con IGV 18% | subtotal + IGV = total |
| `registrar.spec.ts` | guardarMembresia retorna false sin plan/tipo | Can't proceed to payment without selecting plan |
| `registrar.spec.ts` | guardarMembresia guarda en localStorage plan Black 1 mes | Membership selection persists for payment flow |
| `interfaz-usuario.component.spec.ts` | esPlanBlack true con plan Black | Plan Black gate for exclusive features |
