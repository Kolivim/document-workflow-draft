package ru.kolivim.document.workflow.entity;

import ru.kolivim.document.workflow.entity.enums.Action;
import lombok.*;
import org.hibernate.annotations.Cascade;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Table(name = "history", schema = "doc_workflow")
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class History {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author")
    private String author;

    @Column(name = "date")
    private ZonedDateTime date;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "comment")
    private String comment;

    @ManyToOne
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "document_id", referencedColumnName = "id")
    private Document document;

}
