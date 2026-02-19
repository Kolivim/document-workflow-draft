package ru.kolivim.document.workflow.repository;

import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collection;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Collection<? extends Document> findByStatus(Status status);

    Collection<? extends Document> findByAuthor(String author);

    Collection<? extends Document> findByCreateTimeAfter(ZonedDateTime createTimeAfter);

    Collection<? extends Document> findByCreateTimeBefore(ZonedDateTime endDate);
}
