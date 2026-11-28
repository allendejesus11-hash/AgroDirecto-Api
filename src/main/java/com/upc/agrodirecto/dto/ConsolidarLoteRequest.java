package com.upc.agrodirecto.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record ConsolidarLoteRequest(@NotEmpty @Size(max=100) List<@NotNull Integer> idsCargas,
 @NotNull Integer idTransportista, @NotNull @Positive Double fleteTotal) {}
