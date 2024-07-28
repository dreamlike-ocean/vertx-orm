package entity;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;

public record DBRecord (@Column(name = "customer_name") String name, long id){
}
