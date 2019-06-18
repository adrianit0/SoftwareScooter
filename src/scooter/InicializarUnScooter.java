/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scooter;

import conexion.ConectorTCP;
import java.util.Map;
import util.CallbackRespuesta;
import util.Util;

/**
 *
 * @author agarcia.gonzalez
 */
public class InicializarUnScooter {
    public static void main(String[] args) {
        Scooter scooter = new Scooter();
        
        scooter.setNoSerie("Scooter#1");
        scooter.setBateria(100f);
        scooter.setCodigo(123456);
        scooter.setBloqueado(false);
        
        scooter.setLatitud(36.510960f);
        scooter.setLongitud(-6.278162f);
        
        ScooterClientController controlador = new ScooterClientController (scooter, null);
        controlador.start();
    }
}
