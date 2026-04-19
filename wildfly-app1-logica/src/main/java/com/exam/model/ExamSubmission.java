package com.exam.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "exam_submissions")
public class ExamSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column
    private Double score;

    @Column
    private String status = "PENDING";

    @OneToMany(mappedBy = "examSubmission", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<StudentAnswer> answers;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
