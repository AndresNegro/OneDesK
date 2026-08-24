package com.OneDesK.app;

import com.OneDesK.hilos.*;
import com.OneDesK.modelo.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        // ponytail: sin Admin/Catalogo en 0.2.2, las listas "globales" viven locales
        // en App. Lo que reemplace a Admin en la proxima etapa debe absorberlas.
        List<Producto> productos = new ArrayList<>();
        List<Usuario> usuarios = new ArrayList<>();
        List<EmpleadoIndoor> empleados = new ArrayList<>();
        List<Indoor> indoors = new ArrayList<>();
        List<RegistroProduccion> registrosProduccion = new ArrayList<>();

        seed(productos, usuarios, empleados, indoors);

        SimuladorCultivo simulador = new SimuladorCultivo(indoors);
        simulador.arrancar();

        Scanner scanner = new Scanner(System.in);
        MenuConsola menu = new MenuConsola(scanner, productos, usuarios, empleados,
                indoors, registrosProduccion,
                () -> {
                    simulador.detener();
                    System.out.println("Saliendo de OneDesk...");
                });
        menu.loop();
    }

    private static void seed(List<Producto> productos, List<Usuario> usuarios,
                             List<EmpleadoIndoor> empleados, List<Indoor> indoors) {
        productos.add(new Producto("Northern Lights", 10, 500));
        productos.add(new Producto("AK-47", 8, 450));

        Indoor i1 = new Indoor();
        i1.addPlanta(new Planta("Northern Lights", LocalDate.now(), LocalDate.now().minusDays(10),
                2, 4, 6));
        i1.addPlanta(new Planta("Amnesia", LocalDate.now(), LocalDate.now().minusDays(5),
                3, 5, 8));
        indoors.add(i1);

        EmpleadoIndoor carlos = new EmpleadoIndoor("Carlos", "Perez", "carlos@onedesk.com", "pass", 250000);
        carlos.addIndoor(i1);
        empleados.add(carlos);

        usuarios.add(new Usuario("Juan", "Lopez", "juan@mail.com", "pass"));
    }
}