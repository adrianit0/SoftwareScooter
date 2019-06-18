/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scooter;

/**
 *
 * @author Adrián
 */
public interface ScooterEvento {
    public enum Evento {
        DESBLOQUEAR, BLOQUEAR, DAR_POSICION, DAR_INFO
    };
    
    void onEventExecute (Evento evento);
}
