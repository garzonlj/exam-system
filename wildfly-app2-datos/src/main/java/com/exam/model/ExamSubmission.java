package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_submissions")
public class ExamSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column
    private Double score;

    @Column
    private String status;

    @PrePersist
    protected void onCreate() { submittedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getExamId() { return examId; }
    public Long getStudentId() { return studentId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Double getScore() { return score; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
