package com.upc.agrodirecto.dto;

import jakarta.validation.constraints.NotNull;

public record PuntosRutaRequest(@NotNull Double latitudOrigen, @NotNull Double longitudOrigen,
                                @NotNull Double latitudDestino, @NotNull Double longitudDestino) {}
