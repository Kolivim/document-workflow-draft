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

    /**
     * Атомарная вставка записи в реестр утверждений с проверкой на дубликаты
     * @param documentId идентификатор документа, для которого создается запись в реестре
     * @return количество вставленных строк, для случая существования записи вернется 0
     * */
    @Query(value = "INSERT INTO register (id) VALUES (:documentId) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    /* @Transactional(propagation = Propagation.MANDATORY)  /** Требует существующей транзакции */
    int insertIfNotExists(@Param("documentId") Long documentId);


    /**
     * Подсчет количества записей в реестре для указанного документа
     * @param documentId идентификатор документа
     * @return количество записей
     */
    @Query("SELECT COUNT(r) FROM Register r WHERE r.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

}
