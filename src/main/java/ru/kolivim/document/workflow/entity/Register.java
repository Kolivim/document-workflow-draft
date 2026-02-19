package ru.kolivim.document.workflow.entity;

import ru.kolivim.document.workflow.entity.enums.Status;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "register", schema = "doc_workflow")
public class Register {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.APPROVED;

    @OneToOne(mappedBy = "register")
    private Document document;
}
