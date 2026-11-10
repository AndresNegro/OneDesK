-- Los roles que existen. Los ids son los que declara el enum Rol: si se agrega uno, va en los dos lados.

INSERT INTO Rol (ID, nombre) VALUES (1, 'USUARIO');
INSERT INTO Rol (ID, nombre) VALUES (2, 'EMPLEADO');
INSERT INTO Rol (ID, nombre) VALUES (3, 'ADMINISTRADOR');


-- Las personas que ya estaban se quedan con el rol que les corresponde por su tipo, que hasta
-- esta version era de donde salia el rol.

INSERT INTO Persona_Rol (ID_PERSONA, ID_ROL) SELECT ID, 1 FROM Usuario;
INSERT INTO Persona_Rol (ID_PERSONA, ID_ROL) SELECT ID, 2 FROM EmpleadoIndoor;
INSERT INTO Persona_Rol (ID_PERSONA, ID_ROL) SELECT ID, 3 FROM Administrador;
