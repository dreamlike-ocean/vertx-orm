package io.github.dreamlike.backend.test.entity;

import jakarta.persistence.Transient;

public class BaseEntity {
    private String needExist;

    @Transient
    private int dontNeedExist;

    public String getNeedExist() {
        return needExist;
    }

    public void setNeedExist(String needExist) {
        this.needExist = needExist;
    }

    public int getDontNeedExist() {
        return dontNeedExist;
    }

    public void setDontNeedExist(int dontNeedExist) {
        this.dontNeedExist = dontNeedExist;
    }
}
