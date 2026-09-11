package com.dataflow.importsvc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId; // UUID

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size")
    private long fileSize;

    @Column(nullable = false)
    private String status; // UPLOADED, PROCESSING, COMPLETED, FAILED

    @Column(name = "total_rows", nullable = false)
    private long totalRows;

    @Column(name = "imported_rows", nullable = false)
    private long importedRows;

    @Column(name = "error_rows", nullable = false)
    private long errorRows;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
