CREATE TABLE Persona (
    id_persona INT AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contraseña VARCHAR(255) NOT NULL,
    PRIMARY KEY(id_persona)
);
 
CREATE TABLE Deuda (
    id_deuda INT NOT NULL AUTO_INCREMENT,
    monto INT NOT NULL,
    PRIMARY KEY(id_deuda)
);
 
CREATE TABLE Usuario (
    id_usuario INT NOT NULL,
    id_deuda INT NOT NULL,
    PRIMARY KEY(id_usuario)
);
 
CREATE TABLE Producto (
    id_producto INT AUTO_INCREMENT,
    precio INT NOT NULL,
    stock INT NOT NULL,
    PRIMARY KEY(id_producto)
);
 
CREATE TABLE Indoor (
    id_indoor INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY(id_indoor) 
);
 
CREATE TABLE Planta (
    id_planta INT NOT NULL AUTO_INCREMENT,
    id_indoor INT NOT NULL,
    genetica VARCHAR(255) NOT NULL,
    fechaPlantado DATE NOT NULL,
    fechaGerminado DATE NOT NULL,
    fechaCosecha DATE NOT NULL,
    tiempoLuz INT NOT NULL,
    tiempoRegado INT NOT NULL,
    tiempoVentilacion INT NOT NULL,
    PRIMARY KEY(id_planta)
);
 
CREATE TABLE Compra (
    id_compra INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    fechaCompra DATE NOT NULL,
    precio INT NOT NULL,
    pagado BOOLEAN NOT NULL,
    PRIMARY KEY(id_compra)
);
 
CREATE TABLE ItemCompra (
    id_itemCompra INT NOT NULL AUTO_INCREMENT,
    id_compra INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    PRIMARY KEY(id_itemCompra)
);
 
CREATE TABLE Empleado (
    id_empleado INT NOT NULL,
    PRIMARY KEY(id_empleado)
);
 
CREATE TABLE EmpleadoIndoor (
    id_empleadoIndoor INT NOT NULL,
    salarioMensual INT NOT NULL,
    PRIMARY KEY(id_empleadoIndoor)
);
 
CREATE TABLE Evento (
    id_evento INT NOT NULL AUTO_INCREMENT ,
    id_indoor INT NOT NULL,
    id_planta INT NOT NULL,
    realizado BOOLEAN NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    PRIMARY KEY(id_evento)
);
 
CREATE TABLE Trabaja (
    id_empleadoIndoor INT NOT NULL ,
    id_indoor INT NOT NULL ,
    PRIMARY KEY (id_empleadoIndoor, id_indoor)
);
  
CREATE TABLE RegistroProduccion (
	id_registroProduccion INT NOT NULL AUTO_INCREMENT ,
    id_empleadoIndoor INT NOT NULL,
    id_indoor INT NOT NULL,
    cantidad INT NOT NULL,
    id_producto INT NOT NULL,
    PRIMARY KEY (id_registroProduccion)
);
 
ALTER TABLE Usuario
    ADD CONSTRAINT fk_Persona_id_persona_Usuario
        FOREIGN KEY (id_usuario) REFERENCES Persona(id_persona)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE Usuario
    ADD CONSTRAINT fk_Deuda_id_deuda_Usuario
        FOREIGN KEY (id_deuda) REFERENCES Deuda(id_deuda)
        ON DELETE NO ACTION ON UPDATE CASCADE;
 
ALTER TABLE Planta
    ADD CONSTRAINT fk_Indoor_id_indoor_Planta
        FOREIGN KEY (id_indoor) REFERENCES Indoor(id_indoor)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE Compra
    ADD CONSTRAINT fk_Usuario_id_usuario_Compra
        FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
        ON DELETE NO ACTION ON UPDATE CASCADE;
 
ALTER TABLE ItemCompra
    ADD CONSTRAINT fk_Compra_id_compra_ItemCompra
        FOREIGN KEY (id_compra) REFERENCES Compra(id_compra)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE ItemCompra
    ADD CONSTRAINT fk_Producto_id_producto_ItemCompra
        FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
        ON DELETE NO ACTION ON UPDATE CASCADE;
 
ALTER TABLE Empleado
    ADD CONSTRAINT fk_Empleado_id_empleado_Persona
        FOREIGN KEY (id_empleado) REFERENCES Persona(id_persona)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE EmpleadoIndoor
    ADD CONSTRAINT fk_Empleado_id_empleado_EmpleadoIndoor
        FOREIGN KEY (id_empleadoIndoor) REFERENCES Empleado(id_empleado)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE Evento
    ADD CONSTRAINT fk_Indoor_id_indoor_Evento
        FOREIGN KEY (id_indoor) REFERENCES Indoor(id_indoor)
        ON DELETE NO ACTION ON UPDATE CASCADE;
 
ALTER TABLE Evento
    ADD CONSTRAINT fk_Planta_id_planta_Evento
        FOREIGN KEY (id_planta) REFERENCES Planta(id_planta)
        ON DELETE NO ACTION ON UPDATE CASCADE;
 
ALTER TABLE Trabaja
    ADD CONSTRAINT fk_EmpleadoIndoor_id_empleadoIndoor_Trabaja
        FOREIGN KEY (id_empleadoIndoor) REFERENCES EmpleadoIndoor(id_empleadoIndoor)
        ON DELETE CASCADE ON UPDATE CASCADE;
 
ALTER TABLE Trabaja
    ADD CONSTRAINT fk_Indoor_id_indoor_Trabaja
        FOREIGN KEY (id_indoor) REFERENCES Indoor(id_indoor)
        ON DELETE NO ACTION ON UPDATE NO ACTION;
        
ALTER TABLE RegistroProduccion
    ADD CONSTRAINT fk_Indoor_id_indoor_RegistroProduccion
        FOREIGN KEY (id_indoor) REFERENCES Indoor(id_indoor)
        ON DELETE NO ACTION ON UPDATE CASCADE;

ALTER TABLE RegistroProduccion
    ADD CONSTRAINT fk_EmpleadoIndoor_id_empleadoIndoor_RegistroProduccion
        FOREIGN KEY (id_empleadoIndoor) REFERENCES EmpleadoIndoor(id_empleadoIndoor)
        ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE RegistroProduccion
    ADD CONSTRAINT fk_Producto_id_producto_RegistroProduccion
        FOREIGN KEY (id_Producto) REFERENCES Producto(id_producto)
        ON DELETE NO ACTION ON UPDATE NO ACTION;