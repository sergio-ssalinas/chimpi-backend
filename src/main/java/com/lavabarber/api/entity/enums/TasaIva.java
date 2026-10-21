package com.lavabarber.api.entity.enums;

/** Las tres categorias de IVA vigentes en Paraguay. */
public enum TasaIva {
    DIEZ(10),
    CINCO(5),
    EXENTA(0);

    private final int porcentaje;

    TasaIva(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getPorcentaje() {
        return porcentaje;
    }
}
