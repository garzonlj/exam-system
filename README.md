# Sistema de Evaluaciones Distribuidas


### Tecnologías (del diagrama de pizarrón)
| Componente | Tecnología |
|---|---|
| Servidores de app | **WildFly 31** (2 contenedores) |
| ORM | **Hibernate 6** como implementación de **JPA** |
| Mensajería | **RabbitMQ** con protocolo **AMQP** |
| Bases de datos | **PostgreSQL** (2 instancias) |
| Email | **JavaMail** (jakarta.mail) |
| API | **JAX-RS** REST |
| Frontend | HTML + CSS + JavaScript |

### Patrón de transacciones distribuidas: SAGA
La presentación de un examen es una **saga de 3 fases**:
1. **Fase 1** → Persistir en `exams_db` (submission + respuestas) via `ExamsPU`
2. **Fase 2** → Persistir en `students_db` (nota del estudiante) via `StudentsPU`
3. **Fase 3** → Publicar en RabbitMQ (desacoplado por tiempo) para notificación email

**Compensación**: Si la Fase 2 falla, la Fase 1 se marca con estado `ERROR_GRADE`.

## Estructura del proyecto
```
exam-system/
├── docker-compose.yml
├── docker/
│   ├── init-exams.sql        # Esquema DB exámenes
│   ├── init-students.sql     # Esquema DB estudiantes
│   └── nginx.conf            # Proxy reverso
├── wildfly-app1-logica/      # WildFly 1: Lógica + API REST
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/exam/
│       ├── model/            # Entidades JPA (Hibernate)
│       ├── service/          # ExamService (transacciones distribuidas)
│       ├── boundary/         # JAX-RS REST endpoints
│       └── messaging/        # RabbitMQConnection (producer AMQP)
├── wildfly-app2-datos/       # WildFly 2: Consumer + Email
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/exam/
│       ├── model/            # Entidades JPA (Hibernate)
│       └── messaging/        # ExamResultConsumer + EmailService
└── frontend/
    └── index.html            # SPA con HTML/CSS/JS
```

## Inicio rápido

### Prerrequisitos
- Docker Desktop instalado
- Maven 3.8+ (para compilar)
- JDK 17+

### 1. Compilar los proyectos
```bash
# Compilar WildFly 1
cd wildfly-app1-logica
mvn clean package -DskipTests

# Compilar WildFly 2
cd ../wildfly-app2-datos
mvn clean package -DskipTests
```

### 2. Configurar email 
Editar `docker-compose.yml` y reemplazar:
```yaml
MAIL_USER=tu-correo@gmail.com
MAIL_PASS=tu-app-password-gmail
```
> Para Gmail necesitas una **App Password** 
> Ve a: Cuenta Google → Seguridad → Verificación en 2 pasos → App passwords

### 3. Levantar todos los servicios
```bash
cd exam-system
docker-compose up -d
```

### 4. Esperar inicialización (~2 minutos)
```bash
docker-compose logs -f wildfly-logica   # Ver logs WildFly 1
docker-compose logs -f wildfly-datos    # Ver logs WildFly 2
```

### 5. Acceder
| Servicio | URL |
|---|---|
| **Frontend** | http://localhost |
| **WildFly 1 Admin** | http://localhost:9990 |
| **WildFly 2 Admin** | http://localhost:9991 |
| **RabbitMQ Management** | http://localhost:15672 (admin/admin123) |

## API REST

Base URL: `http://localhost/api/exams`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/` | Lista todos los exámenes |
| GET | `/{id}` | Obtiene examen con preguntas |
| GET | `/students/all` | Lista estudiantes |
| GET | `/students/{id}/grades` | Notas de un estudiante |
| POST | `/{id}/submit` | **Envía respuestas** (transacción distribuida) |

### Ejemplo: Enviar examen
```bash
curl -X POST http://localhost/api/exams/1/submit \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "answers": {
      "1": 1,
      "2": 4,
      "3": 7
    }
  }'
```

## Flujo completo del sistema

```
1. Usuario abre http://localhost
2. Frontend carga exámenes via GET /api/exams (WildFly 1)
3. Usuario selecciona examen y estudiante
4. Usuario responde preguntas
5. Frontend POST /api/exams/{id}/submit
   └── WildFly 1 (ExamService):
       ├── FASE 1: INSERT exam_submissions + student_answers → postgres-exams
       ├── FASE 2: INSERT grades → postgres-students
       │   └── Si falla: UPDATE submission status='ERROR_GRADE' (compensación)
       └── FASE 3: Publish message → RabbitMQ queue: email.notification
6. WildFly 2 (ExamResultConsumer):
   ├── Consume mensaje de RabbitMQ (AMQP)
   ├── EmailService.sendExamResult() → SMTP
   ├── UPDATE grades SET email_sent=true → postgres-students
   └── ACK mensaje RabbitMQ
7. Frontend muestra resultado al usuario
```

## Detener servicios
```bash
docker-compose down          # Detener conservando datos
docker-compose down -v       # Detener y borrar volúmenes
```
