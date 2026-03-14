package com.foreign.relation.editable.models;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "parent")
public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    String name;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL,
            orphanRemoval = true)
    List<Child> children;

    protected Parent() {}
}
