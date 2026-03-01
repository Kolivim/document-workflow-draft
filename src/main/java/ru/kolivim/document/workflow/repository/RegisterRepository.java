package ru.kolivim.document.workflow.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kolivim.document.workflow.entity.Register;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Long> {

    /**
     * Атомарная вставка записи в реестр утверждений с проверкой на дубликаты
     * @param documentId идентификатор документа, для которого создается запись в реестре
     * @return количество вставленных строк, для случая существования записи вернется 0
     * */
    @Query(value = "INSERT INTO doc_workflow.register (id) VALUES (:documentId) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    int insertIfNotExists(@Param("documentId") Long documentId);


    default int insertIfNotExist(Long documentId) {

        if (!existsById(documentId)) {
            Register register = new Register();
            register.setId(documentId);
            save(register);
            flush();
            return 1;
        }

        return 0;
    }


    /**
     * Подсчет количества записей в реестре для указанного документа
     * @param documentId идентификатор документа
     * @return количество записей
     */
    @Query("SELECT COUNT(r) FROM Register r WHERE r.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

}
