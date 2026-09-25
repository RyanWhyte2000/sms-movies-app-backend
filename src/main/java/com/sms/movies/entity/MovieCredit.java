package com.sms.movies.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

@Table("movie_credits")
public record MovieCredit(@Id @Embedded.Nullable MovieCreditId id) {}
