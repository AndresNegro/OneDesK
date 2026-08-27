
ALTER TABLE RegistroProduccion DROP CONSTRAINT fk_Producto_id_producto_RegistroProduccion;
ALTER TABLE RegistroProduccion DROP CONSTRAINT fk_EmpleadoIndoor_id_empleadoIndoor_RegistroProduccion;
ALTER TABLE Trabaja DROP CONSTRAINT fk_Indoor_id_indoor_Trabaja;
ALTER TABLE Trabaja DROP CONSTRAINT fk_EmpleadoIndoor_id_empleadoIndoor_Trabaja;
ALTER TABLE Evento DROP CONSTRAINT fk_Planta_id_planta_Evento;
ALTER TABLE Evento DROP CONSTRAINT fk_Indoor_id_indoor_Evento;
ALTER TABLE EmpleadoIndoor DROP CONSTRAINT fk_Empleado_id_empleado_EmpleadoIndoor;
ALTER TABLE Empleado DROP CONSTRAINT fk_Empleado_id_empleado_Persona;
ALTER TABLE ItemCompra DROP CONSTRAINT fk_Producto_id_producto_ItemCompra;
ALTER TABLE ItemCompra DROP CONSTRAINT fk_Compra_id_compra_ItemCompra;
ALTER TABLE Compra DROP CONSTRAINT fk_Usuario_id_usuario_Compra;
ALTER TABLE Planta DROP CONSTRAINT fk_Indoor_id_indoor_Planta;
ALTER TABLE Usuario DROP CONSTRAINT fk_Deuda_id_deuda_Usuario;
ALTER TABLE Usuario DROP CONSTRAINT fk_Persona_id_persona_Usuario;

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