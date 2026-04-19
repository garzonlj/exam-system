-- Base de datos 2: Estudiantes y notas
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    student_code VARCHAR(50) UNIQUE NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grades (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    exam_id BIGINT NOT NULL,
    exam_submission_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    passed BOOLEAN NOT NULL,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email_sent BOOLEAN DEFAULT FALSE
);

-- Datos de ejemplo
INSERT INTO students (first_name, last_name, email, student_code) VALUES
('Carlos', 'Rodríguez', 'carlos.rodriguez@gmail.com', 'EST001'),
('María', 'García', 'maria.garcia@gmail.com', 'EST002'),
('Juan', 'Martínez', 'juan.martinez@gmail.com', 'EST003'),
('Ana', 'López', 'ana.lopez@gmail.com', 'EST004'),
('Pedro', 'Sánchez', 'pedro.sanchez@gmail.com', 'EST005');
