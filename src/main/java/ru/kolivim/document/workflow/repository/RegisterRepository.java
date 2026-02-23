package ru.kolivim.document.workflow.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kolivim.document.workflow.entity.Register;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kolivim.document.workflow.entity.enums.Status;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Long> {

    @Query(value = "INSERT INTO register (id) VALUES (:documentId) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    /* @Transactional(propagation = Propagation.MANDATORY)  /** Требует существующей транзакции */
    int insertIfNotExists(@Param("documentId") Long documentId);

}
