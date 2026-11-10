-- Version 1.0.1: los roles pasan a estar en la base.
-- Rol tiene los que existen y Persona_Rol dice cuales tiene cada persona. Los ids de Rol son los
-- que declara el enum com.OneDesK.enums.Rol, no su posicion: reordenar el enum no cambia los datos.
-- Se corre despues de create_db_1.0.0.sql, y despues va init_db_1.0.1.sql con los roles y la migracion.

CREATE TABLE Rol (
    ID INT NOT NULL,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    PRIMARY KEY (ID)
);

CREATE TABLE Persona_Rol (
    ID_PERSONA INT NOT NULL,
    ID_ROL INT NOT NULL,
    PRIMARY KEY (ID_PERSONA, ID_ROL)
);


ALTER TABLE Persona_Rol
    ADD CONSTRAINT fk_Persona_ID_Persona_Rol
        FOREIGN KEY (ID_PERSONA) REFERENCES Persona(ID)
        ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE Persona_Rol
    ADD CONSTRAINT fk_Rol_ID_Persona_Rol
        FOREIGN KEY (ID_ROL) REFERENCES Rol(ID)
        ON DELETE NO ACTION ON UPDATE CASCADE;
