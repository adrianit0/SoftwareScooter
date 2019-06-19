/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scooter;

import interfaz.Control;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author agarcia.gonzalez
 */
public class InicializarMuchasScooters {
    
    private List<Scooter> listaScooters;

    public InicializarMuchasScooters() {
        listaScooters = new ArrayList<>();
    }
    
    public static void main(String[] args) {
        (new InicializarMuchasScooters()).ejecutar();
    }
    
    private void ejecutar () {
        crearScooter(1, 123123, 36.512899f, -6.276407f, 0.82f);
        crearScooter(2, 135246, 36.510626f, -6.276703f, 1f);
        crearScooter(3, 111111, 36.508984f, -6.277094f, 0.23f);
        crearScooter(4, 222222, 36.511192f, -6.271775f, 0.13f);
        crearScooter(5, 333333, 36.506559f, -6.268625f, 0.56f);
        
        consolaScooter();
    }
    
    private void crearScooter (int serie, int codigo, float lat, float lon, float bateria) {
        Scooter scooter = new Scooter();
        
        scooter.setNoSerie("Scooter#S"+serie);
        scooter.setCodigo(codigo);
        scooter.setBloqueado(true); // Empezará bloqueada hasta que haga login
        scooter.setBateria(bateria);
        
        scooter.setLatitud(lat);
        scooter.setLongitud(lon);
        
        ScooterClientController controlador = new ScooterClientController (scooter, null);
        controlador.start();
        
        scooter.setControlador(controlador);
        
        listaScooters.add(scooter);
    }
    
    private void consolaScooter () {
        // Añadir consola
        Scanner sc = new Scanner (System.in);
        System.out.println("Consola para scooters, si quiere activar el control de una scooter escribe scooter <nombre scooter>. Ejemplo scooter scooter#S5");
        String texto="";
        while (!texto.equals("exit")) {
            texto = sc.nextLine();
            String[] splitted = texto.split(" ");
            switch (splitted[0]) {
                case "scooter":
                    if (splitted.length>1) {
                        Scooter scooter = buscarScooter (splitted[1]);
                        if (scooter==null) {
                            System.out.println("Scooter no encontrada");
                        } else {
                            Control control = new Control (scooter);
                            control.mostrar();
                        }
                    }
                    break;
                case "exit":
                    System.out.println("Adios!!");
                    break;
                default:
                    System.out.println("Comando no encontrado");
                    break;
            }
            
        }
    }
    
    private Scooter buscarScooter (String busqueda) {
        for (Scooter s : listaScooters) {
            if(busqueda.equals(s.getNoSerie()))
                return s;
        }
        return null;
    }
}