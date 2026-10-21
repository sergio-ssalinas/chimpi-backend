package com.lavabarber.api.entity.enums;

public enum RolInterno {
    ADMIN,
    /** Atiende turnos (operacion diaria) y puede operar su propia caja. */
    EMPLEADO,
    /** Solo caja y punto de venta: su propia caja, sus ventas y facturar. Sin operacion diaria ni mantenimiento. */
    CAJERO
}
