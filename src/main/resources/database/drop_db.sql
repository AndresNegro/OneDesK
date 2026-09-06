ALTER TABLE RegistroProduccion
    DROP CONSTRAINT fk_Producto_ID_RegistroProduccion;

ALTER TABLE RegistroProduccion
    DROP CONSTRAINT fk_EmpleadoIndoor_ID_RegistroProduccion;


ALTER TABLE Trabaja
    DROP CONSTRAINT fk_Indoor_ID_Trabaja;

ALTER TABLE Trabaja
    DROP CONSTRAINT fk_EmpleadoIndoor_ID_Trabaja;


ALTER TABLE Evento
    DROP CONSTRAINT fk_Planta_ID_Evento;

ALTER TABLE Evento
    DROP CONSTRAINT fk_Indoor_ID_Evento;


ALTER TABLE EmpleadoIndoor
    DROP CONSTRAINT fk_Empleado_ID_EmpleadoIndoor;


ALTER TABLE Empleado
    DROP CONSTRAINT fk_Empleado_ID_Persona;


ALTER TABLE ItemCompra
    DROP CONSTRAINT fk_Producto_ID_ItemCompra;

ALTER TABLE ItemCompra
    DROP CONSTRAINT fk_Compra_ID_ItemCompra;


ALTER TABLE Compra
    DROP CONSTRAINT fk_Usuario_ID_Compra;


ALTER TABLE Planta
    DROP CONSTRAINT fk_Indoor_ID_Planta;


ALTER TABLE Usuario
    DROP CONSTRAINT fk_Deuda_ID_Usuario;

ALTER TABLE Usuario
    DROP CONSTRAINT fk_Persona_ID_Usuario;


DROP TABLE RegistroProduccion;

DROP TABLE Trabaja;

DROP TABLE Evento;

DROP TABLE EmpleadoIndoor;

DROP TABLE Empleado;

DROP TABLE ItemCompra;

DROP TABLE Compra;

DROP TABLE Planta;

DROP TABLE Indoor;

DROP TABLE Producto;

DROP TABLE Usuario;

DROP TABLE Deuda;

DROP TABLE Persona;
