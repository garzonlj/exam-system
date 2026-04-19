-- Base de datos 1: Exámenes
CREATE TABLE IF NOT EXISTS exams (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    passing_score DOUBLE PRECISION DEFAULT 60.0
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT REFERENCES exams(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS answer_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id) ON DELETE CASCADE,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS student_answers (
    id BIGSERIAL PRIMARY KEY,
    exam_submission_id BIGINT NOT NULL,
    question_id BIGINT REFERENCES questions(id),
    selected_option_id BIGINT REFERENCES answer_options(id),
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_submissions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT REFERENCES exams(id),
    student_id BIGINT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    score DOUBLE PRECISION,
    status VARCHAR(50) DEFAULT 'PENDING'
);

-- Datos de ejemplo
INSERT INTO exams (title, description, passing_score) VALUES
('Examen de Java EE', 'Evaluación sobre tecnologías Java Enterprise Edition', 60.0),
('Examen de Bases de Datos', 'Evaluación sobre SQL y diseño de bases de datos', 70.0);

INSERT INTO questions (exam_id, question_text, question_order) VALUES
(1, '¿Qué es JPA?', 1),
(1, '¿Qué es Hibernate?', 2),
(1, '¿Qué es un EJB?', 3),
(2, '¿Qué es una transacción ACID?', 1),
(2, '¿Qué es una clave foránea?', 2);

INSERT INTO answer_options (question_id, option_text, is_correct) VALUES
(1, 'Java Persistence API - ORM estándar de Java EE', true),
(1, 'Java Performance Analyzer', false),
(1, 'Java Package Archive', false),
(2, 'Framework ORM que implementa JPA', true),
(2, 'Base de datos relacional', false),
(2, 'Servidor de aplicaciones', false),
(3, 'Enterprise Java Bean - componente de lógica de negocio', true),
(3, 'Tipo de base de datos', false),
(3, 'Framework de pruebas', false),
(4, 'Atomicidad, Consistencia, Aislamiento, Durabilidad', true),
(4, 'Agilidad, Concurrencia, Integridad, Disponibilidad', false),
(4, 'Autorización, Control, Identificación, Datos', false),
(5, 'Columna que referencia la clave primaria de otra tabla', true),
(5, 'Clave de cifrado de datos', false),
(5, 'Índice de búsqueda rápida', false);
