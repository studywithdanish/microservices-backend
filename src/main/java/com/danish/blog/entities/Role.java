package com.danish.blog.entities;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
public class Role {

    @Id
    private Integer id;

    private String name;
}
