package io.github.dreamlike.backend.test.entity;

import jakarta.persistence.Column;

public record DBRecord (@Column(name = "customer_name") String name, long id){
}
