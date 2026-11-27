package com.upc.agrodirecto.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
public record UbicacionLoteRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
                                   @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
                                   LocalDateTime eta) {}
