package io.github.dreamlike.backend.test.entity;

import jakarta.persistence.Column;

public class DBEntity extends BaseEntity {

    @Column(name = "ext_id")
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
