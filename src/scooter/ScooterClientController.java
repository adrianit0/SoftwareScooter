/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scooter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import conexion.ConectorTCP;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import util.CallbackRespuesta;
import util.Util;

/**
 *
 * @author agarcia.gonzalez
 */
public class ScooterClientController extends Thread {

    final private Scooter scooter;
    
    private ScooterEvento evento;
    
    private boolean bloqueado;
    
    private int intentos = 0;
    
    private final int maxIntentos = 10;
    private final long tiempoEntreIntento = 5000;
    
    private final int width = 350;
    private final int height = 350;
    
    private boolean conectado;
    
    private int tick;
    private float lastLatitude;
    private float lastLongitude;
    
    private String nick;
    private String token;
    
    public ScooterClientController (Scooter scooter, ScooterEvento evento) {
        this.scooter = scooter;
        this.evento = evento;
        bloqueado = true;
        conectado=false;
        
        lastLatitude = scooter.getLatitud();
        lastLongitude = scooter.getLongitud();
    }
    
    @Override
    public void run() {
        generarImagenQr();
        crearPaquetesPersistentes();
        System.out.println(" Carga de la scooter finalizada");
        
        conectar();
    }

    public Scooter getScooter() {
        return scooter;
    }
    
    public void asignarEvento (ScooterEvento evento) {
        this.evento = evento;
        
        if (evento!=null)
            evento.onEventExecute(bloqueado ? ScooterEvento.Evento.BLOQUEAR : ScooterEvento.Evento.DESBLOQUEAR);
    }
     
    public void eliminarEvento () {
        this.evento = null;
    }
    
    private void conectar () {
        Map<String,String> parametros = Util.convertObjectToMap(scooter);
        
        ConectorTCP.getInstance().realizarConexion("loginScooter", parametros, new CallbackRespuesta() {
            @Override
            public void success(Map<String, String> contenido) {
                //System.out.println("Conexión exitosa!");
                conectado=true;
                
                String nickAux = contenido.get("nick");
                String tokenAux = contenido.get("token");
                ConectorTCP.getInstance().setNick(nickAux);
                ConectorTCP.getInstance().setToken(tokenAux);
                
                nick =nickAux;
                token=tokenAux;
                
                //System.out.println("Nick "+ nick + " Token " + token);
            }

            @Override
            public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                intentos++;
                
                
                String codigo = contenido.get("codeName");
                if (codigo!=null && codigo.equals("notFound")) {
                    System.err.println("La scooter ha sido dada de baja");
                    return;
                }
                
                System.err.println("Error: " + codigoError.toString());
                
                if (intentos<=maxIntentos) {
                    try {
                        Thread.sleep(tiempoEntreIntento);
                        conectar();
                    } catch (InterruptedException ex) {
                        System.err.println("ScooterController::conectar Error del callback " + ex.getMessage());
                    }
                } else {
                    System.err.println("ScooterController::conectar Error de intentos máximos, se ha sobrepassdo el límite máximo de intentos ");
                }
            }
        });
        
        ejecutando();
    }
    
    private void generarImagenQr () {
        String root = "Qr";
        String ruta = root+"/Scooter" + scooter.getNoSerie() + ".png";
        String texto = "SC:"+scooter.getCodigo();
        
        try {
            // Miramos si existe la carpeta para añadir los QR para crearlo en caso de que no exista
            File carpeta = new File("./"+root);
            if (!carpeta.exists()) {
                carpeta.mkdir();
            }
            
            // Miramos si ya existe el archivo Qr, para no volver a generar más
            File archivo= new File ("./" + ruta);
            if (archivo.exists()) {
                //System.out.println("El archivo " +archivo.getAbsolutePath()+" actualmente existe, no se volverá a crear." );
                return;
            }
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, width, height);

            Path path = FileSystems.getDefault().getPath(ruta);
            //System.out.println(path.toFile().getAbsoluteFile());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            
            System.out.println("Imagen generado correctamente");
        } catch (WriterException | IOException e) {
            System.err.println("ScooterController::generarImagenQr error: " + e.getMessage());
        }
    }
    
    private void crearPaquetesPersistentes () {
        ConectorTCP.getInstance().realizarConexionPersistente("desbloquear"+scooter.getCodigo().toString(), new CallbackRespuesta() {
            @Override
            public void success(Map<String, String> contenido) {
                if (evento!=null)
                    evento.onEventExecute(ScooterEvento.Evento.DESBLOQUEAR);
                
                bloqueado=false;
                
                System.out.println("SCOOTER DESBLOQUEADA");
            }
            @Override
            public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                System.err.println("Error desbloquear: " + contenido.get("error"));
            }
        });
        
        ConectorTCP.getInstance().realizarConexionPersistente("bloquear"+scooter.getCodigo().toString(), new CallbackRespuesta() {
            @Override
            public void success(Map<String, String> contenido) {
                if (evento!=null)
                    evento.onEventExecute(ScooterEvento.Evento.BLOQUEAR);
                
                bloqueado=true;
                
                System.out.println("SCOOTER BLOQUEADA");
            }
            @Override
            public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                System.err.println("Error bloquear: " + contenido.get("error"));
            }
        });
        
        ConectorTCP.getInstance().realizarConexionPersistente("posicion"+scooter.getCodigo().toString(), new CallbackRespuesta() {
            @Override
            public void success(Map<String, String> contenido) {
                if (evento!=null)
                    evento.onEventExecute(ScooterEvento.Evento.DAR_POSICION);
                
                // Enviar la información de posición al servidor
            }
            @Override
            public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                System.err.println("Error getPosition: " + contenido.get("error"));
            }
        });
        
        ConectorTCP.getInstance().realizarConexionPersistente("info"+scooter.getCodigo().toString(), new CallbackRespuesta() {
            @Override
            public void success(Map<String, String> contenido) {
                if (evento!=null)
                    evento.onEventExecute(ScooterEvento.Evento.DAR_INFO);
                
                // Enviar la información del estado de la scooter al servidor
            }
            @Override
            public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                System.err.println("Error desbloquear: " + contenido.get("error"));
            }
        });
    }
    
    private void ejecutando () {
        // Crear una rutina de acciones a realizar cada cierto tiempo
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException ex) {
            System.err.println("ScooterController(Client)::ejecutando error");
        }
        
        if (conectado) {
            
            if (!bloqueado) {
                actualizarPosicion();
            } else {
                if (tick%10==0) {
                    actualizarPosicion();
                }
            }
            
            tick++;
        }
        
        ejecutando();
    }
    
    private void actualizarPosicion () {
        if (lastLatitude!=scooter.getLatitud () || lastLongitude!=scooter.getLongitud()) {
            lastLatitude = scooter.getLatitud();
            lastLongitude = scooter.getLongitud();
            
            
            Map<String,String> parametros = new HashMap<>();
            parametros.put("latitud", lastLatitude+"");
            parametros.put("longitud", lastLongitude+"");
            ConectorTCP.getInstance().realizarConexion(nick, token, "actualizarPosicion", parametros, new CallbackRespuesta() {
                @Override
                public void success(Map<String, String> contenido) { }

                @Override
                public void error(Map<String, String> contenido, Util.CODIGO codigoError) {
                    System.err.println("No se ha podido actualizar la posición de la scooter " + contenido.get("error"));
                }
                
            });
        }
        
        
    }
}