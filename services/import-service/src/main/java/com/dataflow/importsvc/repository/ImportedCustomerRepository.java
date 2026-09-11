package com.dataflow.importsvc.repository;

import com.dataflow.importsvc.domain.ImportedCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportedCustomerRepository extends JpaRepository<ImportedCustomer, Long> {
    List<ImportedCustomer> findByImportJobId(String importJobId);
    long countByImportJobId(String importJobId);
}
