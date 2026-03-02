package ru.kolivim.document.workflow.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.annotations.GenericGenerator;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cascade;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@Table(name = "documents", schema = "doc_workflow")
@Entity
@Setter
@Getter
@Builder
@NamedQuery(name = "selectDocument", query = "SELECT e FROM Document e",
        hints = @QueryHint(name = "org.hibernate.fetchSize", value = "100"))
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inner_id")
    private String innerId;

    @Column(name = "author")
    private String author;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Schema(description = "Дата создания документа")
    @Column(name = "create_date")
    private ZonedDateTime createDate;

    @Schema(description = "Дата обновления статуса документа")
    @Column(name = "update_date")
    private ZonedDateTime updateDate;


    @BatchSize(size = 100)
    @OneToMany(mappedBy = "document", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("author")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    private Set<History> historySet;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id")
    private Register register;

}
