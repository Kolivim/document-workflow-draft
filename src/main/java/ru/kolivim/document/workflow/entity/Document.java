package ru.kolivim.document.workflow.entity;

import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cascade;

import java.time.ZonedDateTime;
import java.util.Set;

@Entity
@NamedQuery(name = "selectDocument", query = "SELECT e FROM Document e",
        hints = @QueryHint(name = "org.hibernate.fetchSize", value = "100"))
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents", schema = "doc_workflow")
public class Document {

    @Id
    @Column(name = "document_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inner_id")
    private String innerId;

    @Column(name = "author")
    private String author;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "create_time")
    private ZonedDateTime createTime;

    @Column(name = "update_time")
    private ZonedDateTime updateTime;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "document", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("author")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    //@SortComparator(PropertySource.Comparator.class)
    private Set<History> historySet;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "register_id", referencedColumnName = "id")
    private Register register;
}
