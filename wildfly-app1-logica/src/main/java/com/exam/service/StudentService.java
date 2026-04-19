package com.exam.service;

import com.exam.model.Grade;
import com.exam.model.Student;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * Servicio dedicado exclusivamente a operaciones sobre StudentsDS.
 *
 * Usa REQUIRES_NEW para que cada llamada corra en su propia transacción,
 * evitando el error ARJUNA012140 que ocurre cuando dos datasources locales
 * (no-XA) participan en la misma transacción JTA en WildFly.
 */
@Stateless
public class StudentService {

    @PersistenceContext(unitName = "StudentsPU")
    private EntityManager studentsEM;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Student findStudent(Long studentId) {
        return studentsEM.find(Student.class, studentId);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Student> findAllStudents() {
        return studentsEM.createQuery("SELECT s FROM Student s", Student.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void saveGrade(Grade grade) {
        studentsEM.persist(grade);
        studentsEM.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Grade> findGradesByStudent(Long studentId) {
        return studentsEM.createQuery(
                "SELECT g FROM Grade g WHERE g.studentId = :studentId ORDER BY g.gradedAt DESC",
                Grade.class
        ).setParameter("studentId", studentId).getResultList();
    }
}