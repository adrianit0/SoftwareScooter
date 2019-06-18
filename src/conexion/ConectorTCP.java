/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.CallbackRespuesta;
import util.PaqueteCliente;
import util.PaqueteServidor;
import util.Util;
import util.Util.CODIGO;

/**
 *
 * @author agarcia.gonzalez
 */
public class ConectorTCP {
    
    private int paqueteId;
    private String nick;
    private String token;
    
    private RealizarConexion conexion;
    
    private String inMessage;
    private String outMessage;
    
    // Singleton
    private static ConectorTCP instance;
    
    private long timeout = 1000;
    private String hostServerName="localhost";
    private int port = 4444;
    
    private final String RUTA_CONFIG = "serverconfig.txt";
    
    private ConectorTCP() throws FileNotFoundException, IOException {
        File f = new File(RUTA_CONFIG);
        BufferedReader buffer=new BufferedReader(new FileReader(f));
        while(buffer.ready()) {
            String texto = buffer.readLine();
            String[] splitted = texto.split("=");
            if (splitted.length==2) {
                switch(splitted[0]) {
                    case "timeout":
                        timeout=Integer.parseInt(splitted[1]);
                        break;
                    case "hostServerName":
                    case "server":
                        hostServerName=splitted[1];
                        break;
                    case "port":
                        port=Integer.parseInt(splitted[1]);
                        break;
                }
            }
        }
        
        paqueteId=10;
        conexion = new RealizarConexion ();
        conexion.start();
        
        // TEST
        inMessage = "No se puede construir la trama de datos de entrada";
        outMessage = "No se puede construir la trama de datos de salida";
    }
    
    public synchronized static ConectorTCP getInstance () {
        if (instance==null) {
            iniciarServidor ();
        }
        
        return instance;
    }
    
    public synchronized static boolean iniciarServidor () {
        try {
            instance=new ConectorTCP();
        } catch (Exception e) {
            System.err.println("Error al inicializar el servidor. "+ e.getMessage());
            return false;
        }
        
        return true;
    }
    
    public synchronized void realizarConexion (String uri, CallbackRespuesta response) {
        Map<String,String> parametros = new HashMap<>();
        realizarConexion(nick,token,uri,getPaqueteID(),parametros,response, false);
    }
    
    public synchronized void realizarConexion (String uri, Map<String,String> parametros, CallbackRespuesta response) {
        realizarConexion(nick,token,uri,getPaqueteID(),parametros,response, false);
    }
    
    /**
     * Un paquete persistente no se envia al servidor y se queda esperando a que el servidor responda con
     * el mismo identificador. Es una manera de tener eventos que se activan al llegar información especial del servidor.
     */
    public synchronized void realizarConexionPersistente (String identificador, CallbackRespuesta response) {
        realizarConexion(nick,token,"paquetePersistente",identificador,null,response, true);
        System.out.println("Añadida conexión persistente " + identificador + " con el servidor");
    }

    // Para tests
    public synchronized void realizarConexion (String nick, String token, String uri, String paqueteid, Map<String,String> parametros, CallbackRespuesta response, boolean persistente) {
        // Ponemos los valores para realizar la conexión
        PaqueteServidor paquete = new PaqueteServidor();
        paquete.setIdPaquete(paqueteid);
        paquete.setNick(nick);
        paquete.setToken(token);
        paquete.setArgumentos(parametros);
        paquete.setUri(uri);
        paquete.setCallback(response);
        paquete.setDestroyable(!persistente);
        
        conexion.addInQueue(paquete);
    }
    
    private String getPaqueteID () {
        if (paqueteId==100)
            paqueteId=10;
        return Integer.toString(paqueteId++);
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

    public String getInMessage() {
        return inMessage;
    }

    public String getOutMessage() {
        return outMessage;
    }
    
    
    /**
     ==========================================================
     
     *  CLASES PARA REALIZAR LA CONEXION ASINCRONAMENTE
     
     * ==========================================================
     */
    
    
    class RealizarConexion extends Thread {
        
        private List<PaqueteServidor> enCola;
        private Map<String,PaqueteServidor> pendientes;
        
        private Socket echoSocket;
        private PrintWriter out;
        private BufferedReader in;
        
        private boolean listening;
        private boolean conectado;
        
        public RealizarConexion () {
            enCola = new ArrayList<>();
            pendientes = new HashMap<>();
            listening = true;
            conectado = false;
            
            iniciar();
        }
        
        private synchronized boolean iniciar() {
            try {
                echoSocket = new Socket(hostServerName, port);
                out = new PrintWriter(echoSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
                conectado=true;
            } catch (IOException ex) {
                System.err.println("Error de comunicación: " + ex.getMessage());
                conectado=false;
            }
            return conectado;
        }
        
        public synchronized void addInQueue(PaqueteServidor paquete) {
            enCola.add(paquete);
        }
        
        /**
         Devuelve, a partir de lo que le ha devuelto el servidor, el paquete correcto para ejecutar, y lo elimina de la lista
         */
        public synchronized PaqueteServidor getPaquete (PaqueteCliente paquete) {
            if (!pendientes.containsKey(paquete.getIdPaquete()))
                return null;
            
            // Es posible que haya conexiones persistentes
            PaqueteServidor paqueteServidor = pendientes.get(paquete.getIdPaquete());
            if (!paqueteServidor.isDestroyable())
                return paqueteServidor;
            
            return pendientes.remove(paquete.getIdPaquete());
        }
        
        private synchronized boolean hayPaqueteEnCola () {
            return enCola.size()>0;
        }
        
        @Override
        public void run () {
            // Conectar juego, con un try-with-resource
            // al ser TRY-WITH-RESOURCE estos se cierran solo al terminar la llave
            while (listening) {
                try  {
                    
                    // Preguntamos si hay paquetes en cola para enviar
                    if (out!=null && hayPaqueteEnCola()) {
                        PaqueteServidor paquete = enCola.remove(0);
                        
                        // Solo lo enviamos si el paquete no es persistente
                        if (paquete.isDestroyable()) {
                            String request = Util.packFromServer(paquete);
                        
                            // Le envio la info al servidor
                           out.println(request);
                        }

                       pendientes.put(paquete.getIdPaquete(), paquete);
                    }
                    
                    // Preguntamos si hay paquetes de vuelta
                    if (in!=null && in.ready()) {
                        String respuesta = in.readLine();
                        
                        System.out.println("Respuesta -> " + respuesta);

                        PaqueteCliente paqueteCliente = Util.unpackToCliente(respuesta);

                        if (paqueteCliente!=null) {
                            CODIGO codigo = paqueteCliente.getCodigo();
                        
                            PaqueteServidor paquete = getPaquete(paqueteCliente);

                            // Ejecutamos una parte u otra del callback según si devuelve o no errores
                            if (paquete != null) {
                                if (codigo.getCodigo()>=200 && codigo.getCodigo()<=299) {
                                    paquete.getCallback().success(paqueteCliente.getArgumentos());
                                } else {
                                    paquete.getCallback().error(paqueteCliente.getArgumentos(), paqueteCliente.getCodigo());
                                }
                            }
                        } else {
                            System.err.println("ConectorTCP::run ERROR: trama de entrada \""+ respuesta+"\" formado incorrectamente");
                        }
                    }
                    
                    // TODO: Mirar si hay paquetes con timeOut
                    
                    
                    // Lo dormimos 0.05s (20 veces por segundos) para esperar antes de la próxima pregunta
                    Thread.sleep(50);
                    
                } catch (UnknownHostException e) {
                    System.err.println("No se conoce el host: " + hostServerName);
                } catch (IOException e) {
                    System.err.println("No hay conexión para " + hostServerName);
                } catch (InterruptedException ex) {
                    System.err.println("ConectorTCP::RealizarConexion Error: Timeout. " + ex.toString());
                }
            }
            
            System.out.println("Desconectado Scooter del servidor");
        }
        
        public void close() {
            listening=false;
        }
    }
}