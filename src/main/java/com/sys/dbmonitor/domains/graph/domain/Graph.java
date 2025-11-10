package com.sys.dbmonitor.domains.graph.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "graph")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Graph {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "graph_seq")
    @SequenceGenerator(name = "graph_seq", sequenceName = "SEQ_GRAPH_ID", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 32)
    private GraphCategory category;

    @Column(name = "INFO")
    private String info;

    @Column(name = "TYPE", nullable = false)
    private Integer type;


    @Builder
    public Graph(String name, GraphCategory category, String info, Integer type) {
        this.name = name;
        this.category = category;
        this.info = info;
        this.type = type;
    }

    public void update(String name, GraphCategory category, String info, Integer type) {
        if (name != null) this.name = name;
        if (category != null) this.category = category;
        if (info != null) this.info = info;
        if (type != null) this.type = type;
    }
}
