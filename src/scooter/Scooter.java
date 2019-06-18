/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scooter;

/**
 *
 * @author agarcia.gonzalez
 */
public class Scooter {
    private String noSerie;
    // Posición de la Scooter
    private Float latitud;
    private Float longitud;
    
    // Otra información
    private Float bateria;
    private Integer codigo;
    
    // La moto se encuentra bloqueado, es decir, no se puede usar.
    private Boolean bloqueado;

    public Scooter() {
    }

    public Scooter(String noSerie, Float latitud, Float longitud, Float bateria, Integer codigo, Boolean bloqueado) {
        this.noSerie = noSerie;
        this.latitud = latitud;
        this.longitud = longitud;
        this.bateria = bateria;
        this.codigo = codigo;
        this.bloqueado = bloqueado;
    }

    public String getNoSerie() {
        return noSerie;
    }

    public void setNoSerie(String noSerie) {
        this.noSerie = noSerie;
    }

    public Float getLatitud() {
        return latitud;
    }

    public void setLatitud(Float latitud) {
        this.latitud = latitud;
    }

    public Float getLongitud() {
        return longitud;
    }

    public void setLongitud(Float longitud) {
        this.longitud = longitud;
    }

    public Float getBateria() {
        return bateria;
    }

    public void setBateria(Float bateria) {
        this.bateria = bateria;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public Boolean getBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(Boolean bloqueado) {
        this.bloqueado = bloqueado;
    }
}
