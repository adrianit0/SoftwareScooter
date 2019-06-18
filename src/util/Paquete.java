/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.Map;
import util.Util.CODIGO;

/**
 *
 * @author agarcia.gonzalez
 */
public class Paquete {
    private CODIGO codigo;
    private String nick;
    private String token;
    private String uri;
    
    private Map<String,String> argumentos;

    public Paquete() { }

    public Paquete(CODIGO codigo, String nick, String token, String uri, Map<String, String> argumentos) {
        this.codigo = codigo;
        this.nick = nick;
        this.token = token;
        this.uri = uri;
        this.argumentos = argumentos;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public CODIGO getCodigo() {
        return codigo;
    }

    public void setCodigo(CODIGO codigo) {
        this.codigo = codigo;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, String> getArgumentos() {
        return argumentos;
    }

    public void setArgumentos(Map<String, String> argumentos) {
        this.argumentos = argumentos;
    }
    
    
    
}
