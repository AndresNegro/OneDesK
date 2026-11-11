-- Version 1.0.2: Producto lleva una version para el bloqueo optimista.
-- Dos compras al mismo tiempo leian el mismo stock, las dos pasaban el control y la segunda pisaba
-- el descuento de la primera: se vendia mas de lo que habia. Con la version, la segunda falla.
-- Se corre despues de create_db_1.0.1.sql e init_db_1.0.1.sql.

ALTER TABLE Producto
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
