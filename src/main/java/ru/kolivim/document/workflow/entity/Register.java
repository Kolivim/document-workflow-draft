package ru.kolivim.document.workflow.entity;

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
    private Long id;

    @OneToOne(mappedBy = "register")
    private Document document;

}
