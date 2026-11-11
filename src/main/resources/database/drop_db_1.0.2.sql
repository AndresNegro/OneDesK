-- Deshace la version 1.0.2. Se corre antes que drop_db_1.0.1.sql.

ALTER TABLE Producto
    DROP COLUMN version;
