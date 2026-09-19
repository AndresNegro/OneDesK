-- Crea la base que usan los tests, separada de la base real onedesk.
-- Se corre UNA sola vez y conectado como root: OneDesK_user no tiene permiso para crear bases.
-- Despues, las tablas de onedesk_test se crean con create_db.sql, igual que las de onedesk.

CREATE DATABASE onedesk_test;

GRANT ALL PRIVILEGES ON onedesk_test.* TO 'OneDesK_user'@'localhost';
