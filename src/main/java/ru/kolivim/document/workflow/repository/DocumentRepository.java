package ru.kolivim.document.workflow.repository;

import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Entity> {

    Collection<? extends Document> findByStatus(Status status);

    Collection<? extends Document> findByAuthor(String author);

    Collection<? extends Document> findByCreateDateAfter(ZonedDateTime createTimeAfter);

    Collection<? extends Document> findByCreateDateBefore(ZonedDateTime endDate);

    @Query("SELECT d.id FROM Document d WHERE d.id IN :ids")
    List<Long> findAllExistingIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.status = :newStatus, d.updateDate = CURRENT_TIMESTAMP " +
            "WHERE d.id = :id AND d.status = :expectedStatus")                                                          /* + "RETURNING d.status */
    int updateStatusIfExpected(@Param("id") Long id,
                               @Param("expectedStatus") Status expectedStatus,
                               @Param("newStatus") Status newStatus);

    @Query("SELECT d.status FROM Document d WHERE d.id = :id")
    Optional<Status> findStatusById(@Param("id") Long id);

    Document getReferenceById(Long documentId);

}
