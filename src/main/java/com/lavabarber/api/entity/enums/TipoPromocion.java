package com.lavabarber.api.entity.enums;

public enum TipoPromocion {
    /** valor = porcentaje (0-100] sobre los items a los que aplica. */
    PORCENTAJE,
    /** valor = monto en Gs. descontado del total de los items a los que aplica. */
    MONTO_FIJO,
    /** Por cada 2 unidades de un producto, 1 es gratis. Solo alcance PRODUCTO. */
    DOS_POR_UNO
}
