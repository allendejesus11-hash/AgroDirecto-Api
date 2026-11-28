package com.upc.agrodirecto.servicios;

/** Valida pares completos, finitos y dentro de los límites geográficos. */
public final class Coordenadas {
    private Coordenadas() {}
    public static void validar(Double lat, Double lon, boolean opcional) {
        if (opcional && lat == null && lon == null) return;
        if (lat == null || lon == null || !Double.isFinite(lat) || !Double.isFinite(lon)
                || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            throw new IllegalArgumentException("Se requiere un par de coordenadas válido: latitud [-90,90], longitud [-180,180]");
        }
    }
}
