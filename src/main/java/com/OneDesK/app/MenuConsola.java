package com.OneDesK.app;
import com.OneDesK.evento.*;
import com.OneDesK.modelo.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuConsola {
    private final Scanner sc;
    private final List<Producto> productos;
    private final List<Usuario> usuarios;
    private final List<EmpleadoIndoor> empleados;
    private final List<Indoor> indoors;
    private final List<RegistroProduccion> registrosProduccion;
    private final Runnable onSalir;

    public MenuConsola(Scanner sc, List<Producto> productos, List<Usuario> usuarios,
                       List<EmpleadoIndoor> empleados, List<Indoor> indoors,
                       List<RegistroProduccion> registrosProduccion, Runnable onSalir) {
        this.sc = sc;
        this.productos = productos;
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.indoors = indoors;
        this.registrosProduccion = registrosProduccion;
        this.onSalir = onSalir;
    }

    public void loop() {
        while (true) {
            limpiar();
            System.out.println("===== ONEDESK — MENU PRINCIPAL =====");
            System.out.println("1. Operar como Usuario");
            System.out.println("2. Operar como Empleado Indoor");
            System.out.println("3. Gestion / Altas");
            System.out.println("4. Ver simulacion en vivo");
            System.out.println("0. Salir");
            int op = leerInt("Opcion: ", 0, 4);
            switch (op) {
                case 1 -> menuUsuario();
                case 2 -> menuEmpleado();
                case 3 -> menuGestion();
                case 4 -> verSimulacion();
                case 0 -> { onSalir.run(); return; }
            }
        }
    }

    // ================= ROL USUARIO =================

    private void menuUsuario() {
        Usuario u = elegirUsuario();
        if (u == null) return;
        while (true) {
            limpiar();
            System.out.println("===== USUARIO: " + u + " =====");
            System.out.println("1. Ver productos");
            System.out.println("2. Buscar por genetica");
            System.out.println("3. Ordenar por precio");
            System.out.println("4. Realizar compra");
            System.out.println("5. Ver mis compras");
            System.out.println("6. Ver mi deuda");
            System.out.println("7. Pagar una compra de la deuda");
            System.out.println("0. Volver");
            int op = leerInt("Opcion: ", 0, 7);
            switch (op) {
                case 1 -> u.verProductos(productos);
                case 2 -> buscarProducto(u);
                case 3 -> {
                    List<Producto> ordenados = u.ordenarPorPrecio(productos);
                    System.out.println("  Productos ordenados por precio:");
                    u.verProductos(ordenados);
                }
                case 4 -> realizarCompra(u);
                case 5 -> verCompras(u);
                case 6 -> System.out.println("  Deuda actual: $" + u.getDeuda().getMonto());
                case 7 -> pagarCompra(u);
                case 0 -> { return; }
            }
            pausa();
        }
    }

    private void buscarProducto(Usuario u) {
        String q = leerTexto("Genetica a buscar: ");
        List<Producto> r = u.buscarPorGenetica(q, productos);
        if (r.isEmpty()) System.out.println("  Sin resultados para \"" + q + "\".");
        else u.verProductos(r);
    }

    private void verCompras(Usuario u) {
        if (u.getCompras().isEmpty()) {
            System.out.println("  Sin compras.");
            return;
        }
        for (int i = 0; i < u.getCompras().size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + u.getCompras().get(i));
        }
    }

    private void realizarCompra(Usuario u) {
        if (productos.isEmpty()) { System.out.println("  No hay productos."); return; }
        List<ItemCompra> items = new ArrayList<>();
        while (true) {
            limpiar();
            System.out.println("=== ARMAR COMPRA ===");
            u.verProductos(productos);
            System.out.println("Elegir producto [1.." + productos.size() + "] o 0 para terminar:");
            int idx = leerInt("Producto: ", 0, productos.size());
            if (idx == 0) break;
            Producto p = productos.get(idx - 1);
            int cant = leerInt("Cantidad: ", 1, p.getStock());
            items.add(new ItemCompra(p, cant));
            System.out.println("  Item agregado. Total parcial: $" + totalItems(items));
        }
        if (items.isEmpty()) { System.out.println("  Compra cancelada."); return; }

        Compra compra = new Compra(LocalDate.now(), false, u);
        for (ItemCompra it : items) compra.addItem(it);
        System.out.println("  Total: $" + compra.getPrecio());
        int pg = leerInt("Pagar ahora? 1=Si (al contado) 0=No (queda en deuda): ", 0, 1);
        if (pg == 1) compra.setPagado(true);
        u.realizarCompra(compra);
        System.out.println("  Compra registrada.");
    }

    private int totalItems(List<ItemCompra> items) {
        int t = 0;
        for (ItemCompra it : items) t += it.getPrecio();
        return t;
    }

    private void pagarCompra(Usuario u) {
        List<Compra> deudas = u.getDeuda().getCompras();
        if (deudas.isEmpty()) { System.out.println("  No tenes compras adeudadas."); return; }
        for (int i = 0; i < deudas.size(); i++) {
            System.out.println("  [" + (i + 1) + "] $" + deudas.get(i).getPrecio() +
                    " — " + deudas.get(i).getFechaCompra());
        }
        int idx = leerInt("Cual pagas? [1.." + deudas.size() + "] o 0 para cancelar: ", 0, deudas.size());
        if (idx == 0) return;
        Compra c = deudas.get(idx - 1);
        u.registrarPago(c);
        System.out.println("  Compra pagada y quitada de la deuda. Deuda restante: $" + u.getDeuda().getMonto());
    }

    // ================= ROL EMPLEADO =================

    private void menuEmpleado() {
        EmpleadoIndoor e = elegirEmpleado();
        if (e == null) return;
        while (true) {
            limpiar();
            System.out.println("===== EMPLEADO: " + e + " =====");
            System.out.println("1. Ver eventos pendientes");
            System.out.println("2. Atender un evento");
            System.out.println("3. Cargar registro de produccion");
            System.out.println("4. Ver eventos atendidos");
            System.out.println("5. Gestionar productos (alta / editar)");
            System.out.println("0. Volver");
            int op = leerInt("Opcion: ", 0, 5);
            switch (op) {
                case 1 -> listarPendientes(e);
                case 2 -> atenderEvento(e);
                case 3 -> cargarRegistro(e);
                case 4 -> verAtendidos(e);
                case 5 -> gestionarProductos(e);
                case 0 -> { return; }
            }
            pausa();
        }
    }

    private void listarPendientes(EmpleadoIndoor e) {
        List<Evento> pend = e.eventosPendientesDeIndoor();
        if (pend.isEmpty()) { System.out.println("  Sin eventos pendientes."); return; }
        for (int i = 0; i < pend.size(); i++) {
            Evento ev = pend.get(i);
            System.out.println("  [" + (i + 1) + "] " + ev.getClass().getSimpleName() +
                    " — planta " + ev.getPlanta().getGenetica() + " (" + ev.getPlanta().getIndoor() + ")");
        }
    }

    private void atenderEvento(EmpleadoIndoor e) {
        List<Evento> pend = e.eventosPendientesDeIndoor();
        if (pend.isEmpty()) { System.out.println("  Sin eventos pendientes."); return; }
        listarPendientes(e);
        int idx = leerInt("Cual atiendes? [1.." + pend.size() + "] o 0 para cancelar: ", 0, pend.size());
        if (idx == 0) return;
        Evento ev = e.tomarEventoPendiente(idx - 1);
        if (ev != null) {
            e.atenderEvento(ev);
            System.out.println("  Atendido: " + ev.getClass().getSimpleName() +
                    " sobre " + ev.getPlanta().getGenetica() + ".");
        }
    }

    private void cargarRegistro(EmpleadoIndoor e) {
        if (e.getSectoresACargo().isEmpty()) { System.out.println("  Sin indoors a cargo."); return; }
        System.out.println("  Indoors a cargo:");
        List<Indoor> sectores = e.getSectoresACargo();
        for (int i = 0; i < sectores.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + sectores.get(i));
        }
        int idx = leerInt("Indoor [1.." + sectores.size() + "]: ", 1, sectores.size());
        String genetica = elegirGenetica(sectores.get(idx - 1));
        if (genetica == null) { System.out.println("  Registro cancelado."); return; }
        int cant = leerInt("Cantidad: ", 1, Integer.MAX_VALUE);
        RegistroProduccion r = e.armarRegistroProduccion(sectores.get(idx - 1), genetica, cant);
        registrosProduccion.add(r);

        Producto p = buscarProducto(genetica);
        if (p == null) {
            System.out.println("  Registro guardado, pero no hay un producto con esa genetica en el catalogo.");
        } else {
            p.setStock(p.getStock() + cant);
            System.out.println("  Stock de " + p.getGenetica() + " actualizado a " + p.getStock() + ".");
        }
        System.out.println("  Registro de produccion guardado: " + r);
    }

    private Producto buscarProducto(String genetica) {
        for (Producto p : productos) {
            if (p.getGenetica().equalsIgnoreCase(genetica)) return p;
        }
        return null;
    }

    private String elegirGenetica(Indoor in) {
        List<String> geneticas = new ArrayList<>();
        for (Planta p : in.getPlantas()) {
            String g = p.getGenetica();
            if (!geneticas.contains(g)) geneticas.add(g);
        }
        if (geneticas.isEmpty()) {
            return leerTexto("Genetica producida (no hay plantas con genetica cargada, escribir): ");
        }
        System.out.println("  Geneticas presentes en " + in + ":");
        for (int i = 0; i < geneticas.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + geneticas.get(i));
        }
        System.out.println("  [" + (geneticas.size() + 1) + "] Otra (escribir)");
        int op = leerInt("Genetica [1.." + (geneticas.size() + 1) + "]: ", 1, geneticas.size() + 1);
        if (op == geneticas.size() + 1) return leerTexto("Genetica producida: ");
        return geneticas.get(op - 1);
    }

    private void gestionarProductos(EmpleadoIndoor e) {
        System.out.println("1. Alta de producto");
        System.out.println("2. Editar stock/precio de un producto");
        int op = leerInt("Opcion [0=cancelar]: ", 0, 2);
        if (op == 1) {
            String genetica = leerTexto("Genetica: ");
            int stock = leerInt("Stock: ", 0, Integer.MAX_VALUE);
            int precio = leerInt("Precio: ", 0, Integer.MAX_VALUE);
            e.agregarProducto(productos, new Producto(genetica, stock, precio));
            System.out.println("  Producto agregado: " + genetica);
        } else if (op == 2) {
            if (productos.isEmpty()) { System.out.println("  No hay productos."); return; }
            for (int i = 0; i < productos.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + productos.get(i));
            }
            int idx = leerInt("Producto [1.." + productos.size() + "] o 0 para cancelar: ", 0, productos.size());
            if (idx == 0) return;
            Producto p = productos.get(idx - 1);
            int st = leerInt("Nuevo stock (0 = no tocar): ", 0, Integer.MAX_VALUE);
            int pr = leerInt("Nuevo precio (0 = no tocar): ", 0, Integer.MAX_VALUE);
            e.modificarProducto(p, st == 0 ? null : st, pr == 0 ? null : pr);
            System.out.println("  Producto actualizado: " + p);
        }
    }

    private void verAtendidos(EmpleadoIndoor e) {
        List<Evento> hechos = e.eventosAtendidos();
        if (hechos.isEmpty()) { System.out.println("  Aun no atendiste ninguno."); return; }
        for (Evento ev : hechos) {
            System.out.println("  - " + ev.getClass().getSimpleName() + " — " + ev.getPlanta().getGenetica());
        }
    }

    // ================= GESTION / ALTAS =================

    private void menuGestion() {
        while (true) {
            limpiar();
            System.out.println("===== GESTION / ALTAS =====");
            System.out.println("1. Alta de Usuario");
            System.out.println("2. Alta de Producto");
            System.out.println("3. Alta de Empleado Indoor");
            System.out.println("4. Alta de Indoor");
            System.out.println("5. Alta de Planta (en un indoor)");
            System.out.println("6. Asignar indoor a empleado");
            System.out.println("7. Listar usuarios y su estado (deuda)");
            System.out.println("0. Volver");
            int op = leerInt("Opcion: ", 0, 7);
            switch (op) {
                case 1 -> altaUsuario();
                case 2 -> altaProducto();
                case 3 -> altaEmpleado();
                case 4 -> altaIndoor();
                case 5 -> altaPlanta();
                case 6 -> asignarIndoorAEmpleado();
                case 7 -> listarUsuarios();
                case 0 -> { return; }
            }
            pausa();
        }
    }

    private void altaUsuario() {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String email = leerTexto("Email: ");
        String pass = leerTexto("Contrasenia: ");
        Usuario u = new Usuario(nombre, apellido, email, pass);
        usuarios.add(u);
        System.out.println("  Usuario registrado: " + u);
    }

    private void listarUsuarios() {
        if (usuarios.isEmpty()) { System.out.println("  No hay usuarios registrados."); return; }
        System.out.println("  Usuarios:");
        for (Usuario u : usuarios) {
            int monto = u.getDeuda().getMonto();
            String estado = monto > 0 ? "EN DEUDA" : "AL DIA";
            String detalle = monto > 0 ? " | Adeuda: $" + monto : "";
            System.out.println("  - " + u + " | " + estado + detalle);
        }
    }

    private void altaProducto() {
        String genetica = leerTexto("Genetica: ");
        int stock = leerInt("Stock: ", 0, Integer.MAX_VALUE);
        int precio = leerInt("Precio: ", 0, Integer.MAX_VALUE);
        productos.add(new Producto(genetica, stock, precio));
        System.out.println("  Producto cargado: " + genetica);
    }

    private void altaEmpleado() {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String email = leerTexto("Email: ");
        String pass = leerTexto("Contrasenia: ");
        int salario = leerInt("Salario mensual: ", 0, Integer.MAX_VALUE);
        EmpleadoIndoor e = new EmpleadoIndoor(nombre, apellido, email, pass, salario);
        empleados.add(e);
        System.out.println("  Empleado cargado: " + e);
    }

    private void altaIndoor() {
        Indoor in = new Indoor();
        indoors.add(in);
        System.out.println("  Indoor creado.");
    }

    private void altaPlanta() {
        if (indoors.isEmpty()) { System.out.println("  No hay indoors. Crea uno primero."); return; }
        for (int i = 0; i < indoors.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + indoors.get(i));
        }
        int idx = leerInt("Indoor [1.." + indoors.size() + "]: ", 1, indoors.size());
        String genetica = leerTexto("Genetica: ");
        int tiempoRegado = leerInt("Tiempo entre riegos (min): ", 1, 999);
        int tiempoLuz = leerInt("Tiempo entre eventos de luz (min): ", 1, 999);
        int tiempoVentilacion = leerInt("Tiempo entre ventilaciones (min): ", 1, 999);
        Planta p = new Planta(genetica, LocalDate.now(), LocalDate.now(), tiempoRegado, tiempoLuz, tiempoVentilacion);
        indoors.get(idx - 1).addPlanta(p);
        p.arrancar();
        System.out.println("  Planta cargada y arrancada.");
    }

    private void asignarIndoorAEmpleado() {
        if (empleados.isEmpty() || indoors.isEmpty()) { System.out.println("  Faltan empleados o indoors."); return; }
        System.out.println("  Empleados:");
        for (int i = 0; i < empleados.size(); i++) System.out.println("  [" + (i + 1) + "] " + empleados.get(i));
        int ie = leerInt("Empleado [1.." + empleados.size() + "]: ", 1, empleados.size());
        System.out.println("  Indoors:");
        for (int i = 0; i < indoors.size(); i++) System.out.println("  [" + (i + 1) + "] " + indoors.get(i));
        int ii = leerInt("Indoor [1.." + indoors.size() + "]: ", 1, indoors.size());
        empleados.get(ie - 1).addIndoor(indoors.get(ii - 1));
        System.out.println("  Indoor asignado al empleado.");
    }

    // ================= SIMULACION =================

    private void verSimulacion() {
        limpiar();
        System.out.println("===== SIMULACION EN VIVO =====");
        if (indoors.isEmpty()) System.out.println("  No hay indoors.");
        for (Indoor in : indoors) {
            System.out.println("-- " + in + " --");
            for (Planta p : in.getPlantas()) {
                System.out.println("   " + p);
            }
            if (in.getPlantas().isEmpty()) System.out.println("   (sin plantas)");
        }
        System.out.println("  Eventos en cola totales: " + totalEventosEnCola());
        pausa();
    }

    private int totalEventosEnCola() {
        int t = 0;
        for (Indoor in : indoors) t += in.colaSize();
        return t;
    }

    // ================= HELPERS =================

    private Usuario elegirUsuario() {
        if (usuarios.isEmpty()) { System.out.println("  No hay usuarios. Registra uno en Gestion/Altas."); return null; }
        for (int i = 0; i < usuarios.size(); i++) System.out.println("  [" + (i + 1) + "] " + usuarios.get(i));
        int idx = leerInt("Usuario [1.." + usuarios.size() + "] o 0 para cancelar: ", 0, usuarios.size());
        if (idx == 0) return null;
        return usuarios.get(idx - 1);
    }

    private EmpleadoIndoor elegirEmpleado() {
        if (empleados.isEmpty()) { System.out.println("  No hay empleados. Registra uno en Gestion/Altas."); return null; }
        for (int i = 0; i < empleados.size(); i++) System.out.println("  [" + (i + 1) + "] " + empleados.get(i));
        int idx = leerInt("Empleado [1.." + empleados.size() + "] o 0 para cancelar: ", 0, empleados.size());
        if (idx == 0) return null;
        return empleados.get(idx - 1);
    }

    private int leerInt(String prompt, int min, int max) {
        while (true) {
            System.out.print("  " + prompt);
            String linea = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(linea);
                if (v >= min && v <= max) return v;
                System.out.println("  Valor fuera de rango [" + min + ".." + max + "].");
            } catch (NumberFormatException e) {
                System.out.println("  Ingresa un numero valido.");
            }
        }
    }

    private String leerTexto(String prompt) {
        System.out.print("  " + prompt);
        String s = sc.nextLine().trim();
        return s.isEmpty() ? "-" : s;
    }

    private void limpiar() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) for (int i = 0; i < 30; i++) System.out.println();
        else System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void pausa() {
        System.out.println("\n  Presiona Enter para continuar...");
        sc.nextLine();
    }
}
