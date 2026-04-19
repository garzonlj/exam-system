package com.exam.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "exam_submission_id", nullable = false)
    private Long examSubmissionId;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "graded_at")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime gradedAt;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @PrePersist
    protected void onCreate() { gradedAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public Long getExamSubmissionId() { return examSubmissionId; }
    public void setExamSubmissionId(Long examSubmissionId) { this.examSubmissionId = examSubmissionId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public LocalDateTime getGradedAt() { return gradedAt; }
    public Boolean getEmailSent() { return emailSent; }
    public void setEmailSent(Boolean emailSent) { this.emailSent = emailSent; }
}