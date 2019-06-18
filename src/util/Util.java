/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author adrian
 */
public class Util {
    /**
     * Convierte codigos dificiles de enviar por letras distinguibles.
     * 
     * Además permite modificar el codigo sin que este afecte al cliente y servidor
     * siempre y cuando ambos usen la misma versión del protocolo.
     * 
     * Igualmente no se recomienda modificar los códigos de error una vez ha sido
     * seleccionado
     */
    public enum CODIGO {
        // 0 - 99. Juego
        desconocido             (-1),
        error                   (3),
        ponerMenu               (30),
        
        // 200 - 299. Mensajes de confirmación (Status OK)
        ok                      (200),
        
        // 400 - 449. Errores del cliente
        forbidden               (403),  // Intentar acceder sin tener privilegios necesarios
        notFound                (404),
        timeOut                 (408),
        sessionExpired          (440),
        notConnection           (450),
        
        
        // 450 - 499. Errores del servidor
        internalError           (450),
        
        // 500 - 599. UDP
        identificarse           (500),
        conectado               (501),
        loginFailed             (502),
        
        
        registrarse             (550),
        registrado              (551),
        
        desconectar             (580)   // Cierra la sesión eliminando el token.
        
        ;
        
        int codigo;
        CODIGO (int codigo) {
            this.codigo = codigo;
        }
        
        public int getCodigo() {
            return codigo;
        }
        
        public static CODIGO fromCode (int code) {
            CODIGO[] cod = CODIGO.values();
            for (CODIGO c : cod) {
                if (c.getCodigo()==code)
                    return c;
            }
            
            return CODIGO.desconocido;
        }
        
        public static CODIGO fromCode (String code) {
            CODIGO cod = CODIGO.desconocido;
            try {
                cod = Util.CODIGO.fromCode(Integer.parseInt(code));
            } catch (NumberFormatException e) {
                cod = Util.CODIGO.desconocido;
                System.err.println("Error de parseo del texto "+code+". Error: "+e.getMessage());
            }
            return cod;
        }
    };
    
    // TODO: Poner al menos una manera de codificar el texto de manera online.
    // Es más seguro que ir en texto plano. Luego tiene que volver a ser igual
    // en el decode.
    public enum ENCRIPTADOR {
        plain, hybridCriptography //<- Este es el que usaré
    };

    private static final String separator = ";";
    private static final String separatorArgs = ":";
    private static final ENCRIPTADOR encriptacion = ENCRIPTADOR.plain;
    
    /**
     * Convierte una cadena de texto en un paquete 
     * 
     * TODO: Convertir el paquete en un DTO
     */
    public static PaqueteServidor unpackToServer (String cadena) {
        PaqueteServidor pack = new PaqueteServidor();
        cadena = convertirObjetos(cadena, pack);
        System.out.println("-> " + cadena);
        String[] decoded = decode(cadena);
        
        if (decoded==null || decoded.length<3) {
            System.err.println("Error Util::unpack: El paquete no se ha formado correctamente.");
            //throw new Exception();
            return null;
        }
        
        // Extraemos el contenido del paquete
        String idPaquete = decoded[0];
        String nick = decoded[1];
        String token = decoded[2];
        String uri = decoded[3];
        Map<String,String> parametros = new HashMap<>();
        if (decoded.length>=4) {
            for (int i = 4; i < decoded.length; i++) {
                String[] type = decoded[i].split("["+separatorArgs+"]");
                if (type.length<2) {
                    // NO ES UN ARGUMENTO.
                    // TODO: devolver correctamente el mensaje de error
                    System.err.println("La variable " + decoded[i] + " no es un parametro");
                    continue;
                }
                
                // Es un objeto ??
                if (type[1].charAt(0)=='{' && type[1].charAt(type[1].length()-1)=='}') {
                    
                }
                
                parametros.put(destransformarKeyValue(type[0]), destransformarKeyValue(type[1]));
            }
        }
        
        // Lo almacenamos en un objeto de tipo Paquete
        
        pack.setIdPaquete(idPaquete);
        pack.setNick(nick);
        pack.setToken(token);
        pack.setUri(uri);
        pack.setArgumentos(parametros);
        
        // Lo devolvemos
        return pack;
    }
    
    /**
     * Convierte un paquete en un String.
     * 
     * TODO: Convertir el paquete en un DTO
     */
    public static String packFromServer (PaqueteServidor paquete) {
        if (paquete.getArgumentos()==null) {
            paquete.setArgumentos(new HashMap<String,String>());
        }
        int total = paquete.getArgumentos().size();
        String[] parametros = new String[total+4];
        parametros[0] = paquete.getIdPaquete();
        parametros[1] = paquete.getNick();
        parametros[2] = paquete.getToken();
        parametros[3] = paquete.getUri();
        int actual = 4;
        
        
        for (Map.Entry<String, String> entry : paquete.getArgumentos().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            parametros[actual] = transformarKeyValue(key) + ":" + transformarKeyValue(value);
            
            actual++;
        }
        
        String encoded = encode (parametros);
        
        for (Map.Entry<String, String> entry : paquete.getObjetos().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            
            encoded = encoded.replace (key, value);
        }
        
        return encoded;
    }
    
    /**
     * Convierte una cadena de texto en un paquete 
     * 
     * TODO: Convertir el paquete en un DTO
     */
    public static PaqueteCliente unpackToCliente (String cadena) {
        PaqueteCliente pack = new PaqueteCliente();
        cadena = convertirObjetos(cadena, pack);
        String[] decoded = decode(cadena);
        
        if (decoded==null || decoded.length<2) {
            System.err.println("Error Util::unpack: El paquete no se ha formado correctamente.");
            //throw new Exception();
            
            // Devolver un paquete con error Paquete mal formado
            return null;
        }
        
        // Extraemos el contenido del paquete
        CODIGO codigo = CODIGO.fromCode(decoded[0]);
        String idPaquete = decoded[1];
        Map<String,String> parametros = new HashMap<>();
        if (decoded.length>2) {
            for (int i = 2; i < decoded.length; i++) {
                String[] type = decoded[i].split("["+separatorArgs+"]");
                if (type.length<2) {
                    // NO ES UN ARGUMENTO.
                    // TODO: devolver correctamente el mensaje de error
                    System.err.println("La variable " + decoded[i] + " no es un parametro");
                    continue;
                }
                parametros.put(destransformarKeyValue(type[0]), destransformarKeyValue(type[1]));
            }
        }
        
        // Lo almacenamos en un objeto de tipo Paquete
        pack.setCodigo(codigo);
        pack.setIdPaquete(idPaquete);
        pack.setArgumentos(parametros);
        
        // Lo devolvemos
        return pack;
    }
    
    /**
     * Convierte un paquete en un String.
     * 
     * TODO: Convertir el paquete en un DTO
     */
    public static String packFromClient (PaqueteCliente paquete) {
        int total = paquete.getArgumentos().size();
        String[] parametros = new String[total+1];
        parametros[0] = paquete.getIdPaquete();
        int actual = 1;
        for (Map.Entry<String, String> entry : paquete.getArgumentos().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            parametros[actual] = transformarKeyValue(key) + ":" + transformarKeyValue(value);
            
            actual++;
            
        }
        
        String encoded = encode (paquete.getCodigo(), parametros);
        System.out.println("Cantidad argumentos: " + paquete.getObjetos().size());
        
        for (Map.Entry<String, String> entry : paquete.getObjetos().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            System.out.println("KEY: " + key+ " VALUE:" + value);
            
            encoded = encoded.replace (key, value);
        }
        
        return encoded;
    }
    
    /**
     *  Convierte los objetos de una trama de datos
    */
    private static String convertirObjetos (String trama, IPaquete contenido) {
        int firstPos = -1;
        int lastPos = -1;
        int founds = 0;
        for (int i = 0; i < trama.length(); i++) {
            char c = trama.charAt(i);
            if (c=='{') {
                if (founds==0) {
                    firstPos=i;
                    
                }
                founds++;
                
            } else if (founds>0 && c=='}') {
                founds--;
                if (founds==0) {
                    lastPos=i;
                    
                    String replace = trama.substring(firstPos+1, lastPos);
                    String toReplace = contenido.addObjeto(
                            // Convierte el objeto antes de enviarlo
                            convertirObjetos(replace, contenido)
                    );
                    
                    trama = trama.replaceFirst(Pattern.quote(replace), toReplace);
                    
                    i=firstPos+1;
                    
                    firstPos=-1;
                    lastPos=-1;
                }
            }
        }
        
        return trama;
    }
    
    /**
     * Convierte el texto del servidor a una sola linea
     */
    private static String encode (CODIGO code) {
        return encriptar(encriptacion, code.getCodigo()+"");
    }
    
    /**
     * Convierte el texto del servidor a una sola linea
     */
    private static String encode (CODIGO code, String... contenido) {
        return encriptar(encriptacion, code.getCodigo()+separator+String.join(separator, contenido));
    }
    
    private static String encode (String... contenido) {
        return encriptar(encriptacion, String.join(separator, contenido));
    }
    
    /**
     * Desencripta el contenido de la cadena en un array de String
     */
    private static String[] decode (String cadena) {
        return desencriptar(encriptacion,cadena).replace("\0","").split(separator);
    }
    
    // Es probable que dentro del texto contenga información que pueda corromperse
    // debido a la estructura interna de la trama de datos, por eso lo vamos a sustituir
    // usando un sistema de entidades parecidas a la que utiliza HTML
    private static String transformarKeyValue (String texto) {
        if (texto==null) return null;
        return texto.replaceAll("[&]", "&a").replaceAll("\n", "&s").replaceAll("["+separatorArgs+"]", "&d")/*.replaceAll("[{]", "&i").replaceAll("[}]", "&f")*/.replaceAll("[|]", "&p").replaceAll("["+separator+"]", "&c");
    }
    
    // Vuelve a convertir de las entidades al que habia antes
    private static String destransformarKeyValue (String texto) {
        if (texto==null) return null;
        return texto.replaceAll("&c", separator).replaceAll("&p", "|")/*.replaceAll("&f", "}").replaceAll("&i", "{")*/.replaceAll("&d", separatorArgs).replaceAll("&s", "\n").replaceAll("[&]", "&a");
    }
    
    /**
     * Encripta el código para presentar una mejor seguridad en la transferencia
     * de datos del servidor-cliente.
     * 
     * De momento no encripta nada, pero el método ya está creado.
     * 
     * TODO: Utilizar encriptación asimétrica (clave publica-privada) para la encriptación.
     */
    private static String encriptar (ENCRIPTADOR cod, String texto) {
        return texto;
    }
    
    
    /**
     * Desencripta un texto. El método de encriptacion debe ser el mismo que lleva.
     * 
     */
    private static String desencriptar (ENCRIPTADOR cod, String texto) {
        return texto;
    }
    
    public static List convertMapToList (Class clase, Map<String,String> parametros){
        List lista = new ArrayList();
        
        int length = Integer.parseInt(parametros.get("length"));
        
        for (int i = 0; i < length; i++) {
            Object o = convertMapToObject(clase, parametros, "["+i+"]");
            if (o!=null) {
                lista.add(o);
            }
        }
        
        return lista;
    }
    
    public static Object convertMapToObject (Class clase, Map<String,String> parametros) { 
        return convertMapToObject(clase, parametros, "");
    }
    
    // FIXME: TODO: Hacer esto
    public static Object convertMapToObject (Class clase, Map<String,String> parametros, String extra) {
        Field[] campos = clase.getDeclaredFields();
        Object obj=null;
        try {
            obj = clase.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            System.err.println("Util:: convertMapToObject Error: no se puede instanciar el objeto. "+ex.getMessage());
        }
        
        if (obj==null)
            return null;
             
        for (Field f : campos) {
            String genericType = f.getGenericType().toString().split(" ")[0];
            
            if (!genericType.equals("interface")) {
                try {
                    boolean ignore = false;
                    Annotation[] anotaciones = f.getAnnotations();
                    for (Annotation a : anotaciones) {
                        if (a.annotationType().getSimpleName().equals("Ignore")){
                            ignore=true;
                            break;
                        }
                    }
                    if (ignore) continue;
                    
                    String methodName = f.getName();
                    methodName = "set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                    
                    String getMethodName = "get" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1);
                    Method getMethod = clase.getMethod(getMethodName);
                    Class returnType = getMethod.getReturnType();
                    
                    
                    Method metodo = clase.getMethod (methodName, returnType);
                    
                    Object objectConverted = null;
                    String parametro = parametros.get(f.getName()+extra);
                    if (parametro!=null) {
                        switch (returnType.getSimpleName()) {
                            case "String":
                                objectConverted = parametro;
                                break;
                            case "Integer":
                            case "int":
                                objectConverted = Integer.parseInt(parametro);
                                break;
                            case "Double":
                            case "double":
                                objectConverted = Double.parseDouble(parametro);
                                break;
                            case "Float":
                            case "float":
                                objectConverted = Float.parseFloat(parametro);
                                break;
                            case "Boolean":
                            case "boolean":
                                objectConverted = Boolean.parseBoolean(parametro);
                            default:
                                System.err.println("Util::convertMapToObject error: "+returnType.getCanonicalName() + " no es compatible con el parseo");
                                break;
                        }
                    }
                    
                    metodo.invoke(obj, objectConverted);
                    
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    System.err.println("Error en "+ f.getName() + ": "+ex.getMessage() + " ("+ex.getClass().getName()+")");
                    ex.printStackTrace();
                }
            }
        }
        
        return obj;
    }
    
    /**
     * Convierte los valores de un objeto en un Map<String,String>.
     * 
     * Ignora cualquier campo que tenga la anotación @Ignore
     */
    public static Map<String,String> convertObjectToMap (Object obj) {
        return convertObjectToMap(obj,"");
    }
    
    public static Map<String,String> convertObjectToMap (Object obj, String extra) {
        Class clase = obj.getClass();
        Field[] campos = clase.getDeclaredFields();
        
        Map<String,String> parametros = new HashMap<>();
        
        for (Field f : campos) {
            String genericType = f.getGenericType().toString().split(" ")[0];
            
            if (!genericType.equals("interface")) {
                try {
                    boolean ignore = false;
                    Annotation[] anotaciones = f.getAnnotations();
                    for (Annotation a : anotaciones) {
                        if (a.annotationType().getSimpleName().equals("Ignore")){
                            ignore=true;
                            break;
                        }
                    }
                    if (ignore) continue;
                    
                    String methodName = f.getName();
                    methodName = "get" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                    
                    Method metodo = clase.getMethod (methodName);
                    Object o = metodo.invoke(obj);
                    
                    System.out.println(o);
                    parametros.put(f.getName()+extra, o==null||o.toString().isEmpty()?null:o.toString());
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    System.err.println("Error en "+ f.getName() + ": "+ex.getMessage() + " ("+ex.getClass().getName()+")");
                }
            }
        }
        
        return parametros;
    }
    
    public static Map<String,String> convertListToMap (List lista) {
        return convertListToMap(lista, "[", "]");
    }
    
    public static Map<String,String> convertListToMap (List lista, String extraPre, String extraPost) {
        Map<String,String> parametros = new HashMap<>();
        parametros.put("length", lista.size()+"");
        
        int i = 0;
        for (Object o : lista) {
            parametros.putAll(convertObjectToMap(o, extraPre+i+extraPost));
            
            i++;
        }
        
        return parametros;
    }
    
    // Mejoraría cambiar el cifrado
    public static String crearTokenUsuario () {
        return Integer.toString(Math.abs(Double.toString(Math.random()*Math.random()).hashCode()));
    }
    
    /**
     * Convierte un texto plano en el cifrado MD5
     * */
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32)
                hashtext = "0" + hashtext;
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new CifrarMD5Exception(e);
        }
    }
}