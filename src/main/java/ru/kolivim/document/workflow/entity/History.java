package ru.kolivim.document.workflow.entity;

import ru.kolivim.document.workflow.entity.enums.Action;
import lombok.*;
import org.hibernate.annotations.Cascade;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "history", schema = "doc_workflow")
public class History {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author")
    private String author;

    @Column(name = "time")
    private ZonedDateTime time;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "comment")
    private String comment;

    @ManyToOne
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "document_id", referencedColumnName = "document_id")
    private Document document;
}
