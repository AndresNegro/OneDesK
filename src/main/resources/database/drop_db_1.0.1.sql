-- Deshace la version 1.0.1. Se corre antes que drop_db_1.0.0.sql.

ALTER TABLE Persona_Rol
    DROP CONSTRAINT fk_Rol_ID_Persona_Rol;

ALTER TABLE Persona_Rol
    DROP CONSTRAINT fk_Persona_ID_Persona_Rol;


DROP TABLE Persona_Rol;

DROP TABLE Rol;
