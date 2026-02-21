package ru.kolivim.document.workflow.repository;

import jakarta.persistence.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collection;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Entity> {

    Collection<? extends Document> findByStatus(Status status);

    Collection<? extends Document> findByAuthor(String author);

    Collection<? extends Document> findByCreateDateAfter(ZonedDateTime createTimeAfter);

    Collection<? extends Document> findByCreateDateBefore(ZonedDateTime endDate);

}
