package ru.kolivim.document.workflow.entity;

import ru.kolivim.document.workflow.entity.enums.Status;
import lombok.*;
import jakarta.persistence.*;

@Table(name = "register", schema = "doc_workflow")
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Register {

    @Id
    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "register")
//    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    private Document document;

}
