package com.sms.movies.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("genre")
public record Genre(@Id UUID id, String name) {}
