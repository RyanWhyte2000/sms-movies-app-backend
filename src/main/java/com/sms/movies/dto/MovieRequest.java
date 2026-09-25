package com.sms.movies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record MovieRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    @Size(max = 2048)
        @Pattern(regexp = "https?://\\S+", message = "must be a valid HTTP or HTTPS URL")
        String posterUrl,
    @NotBlank @Size(max = 200) String director,
    @NotBlank @Size(max = 100) String genre,
    @NotNull LocalDate releaseDate,
    @NotNull @Positive Integer durationMinutes,
    @Size(max = 100) java.util.List<@NotNull UUID> castIds,
    @Size(max = 20) java.util.List<@NotNull UUID> directorIds,
    @Size(max = 20) java.util.List<@NotNull UUID> genreIds) {}
