/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package wrapper.facebook;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Sebastian Ardila
 */
public class App {
    
    private String token;
    
    private String[] Palabras_clave;
    
    private PrintWriter pw_salida;
//	private PrintWriter pw_salida_prueba;
    
    //Conexion con base de datos
    Connection conn = null;
    Statement stmt = null;
    
    private long total_solicitudes;
    
    public App() throws ParseException, java.text.ParseException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException{
        try {
            generarArchivoDeSalida();
            leerInfo();
            pw_salida.close();
            System.out.println("Proceso terminado " + total_solicitudes);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @return
     */
    public String generarFechaDeExtraccion(){
        Date fecha_extraccion = new Date();
        SimpleDateFormat df =new SimpleDateFormat("yyyy-MM-dd");
        return df.format(fecha_extraccion);
    }
    
    /**
     * @throws IOException
     */
    public void generarArchivoDeSalida() throws IOException{
        String fileNamePrueba = "C:/Users/User/Desktop/facebook-Prueba-" + generarFechaDeExtraccion() +".csv";
        String fileName = "C:/Users/User/Desktop/facebook" + "-" + generarFechaDeExtraccion() + ".csv";
        pw_salida = new PrintWriter(new File(fileName));
//		pw_salida_prueba = new PrintWriter(new File(fileNamePrueba));
    }
    
    /**
     * @return
     * @throws FileNotFoundException
     */
    public BufferedReader generarArchivoDeLecturaDeInformacion() throws FileNotFoundException{
        BufferedReader info = new BufferedReader( new FileReader(new File("./Data/facebook-info.csv")));
        return info;
    }
    
    /**
     * @throws IOException
     * @throws ParseException
     * @throws java.text.ParseException
     */
    public void leerInfo() throws IOException, ParseException, java.text.ParseException{
        BufferedReader info = generarArchivoDeLecturaDeInformacion();
        token = JOptionPane.showInputDialog("Introduzca su token de Facebook");
        System.out.println(info.readLine());
        Palabras_clave = info.readLine().split(",");
        escribirTitulosColumnas();
        crearTodasLasSolicitudes(info);
    }
    
    /**
     * @throws IOException
     */
    public void escribirTitulosColumnas() throws IOException{
        pw_salida.println("Actor;Fuente;Fecha de la publica;Publicacion\n");
    }
    
    /**
     * @param info_actor
     * @return
     * @throws MalformedURLException
     */
    public String extraerNombreDelActor(String info_actor) throws MalformedURLException{
        return info_actor.split(",")[0];
    }
    
    /**
     * @param info_actor
     * @return
     */
    public String extraerIdDeLaPagina(String info_actor){
        return info_actor.split(",")[1];
    }
    
    /**
     * @param id_page
     * @param fecha_until
     * @return
     * @throws MalformedURLException
     */
    public URL crearSolicitudAlAPI(String id_page, String fecha_until) throws MalformedURLException{
        URL url = new URL("https://graph.facebook.com/v2.11/" +  id_page +"/posts?format=json&" + "until=" + fecha_until +"&limit=100&access_token=" + token);
        //		System.out.println(url);
        return url;
    }
    
    /**
     * @param url
     * @return
     * @throws IOException
     */
    public String enviarSolicitud(URL url) throws IOException{
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String resultado = new String();
        String line;
        while ((line = reader.readLine()) != null) {
            resultado += line + "\n";
        }
        connection.disconnect();
        return resultado;
        
    }
    
    /**
     * @param info
     * @throws IOException
     * @throws ParseException
     * @throws java.text.ParseException
     */
    public void crearTodasLasSolicitudes(BufferedReader info) throws IOException, ParseException, java.text.ParseException{
        String info_actor = info.readLine();
        int i = 0;
        while(info_actor != null && !info_actor.equals("")){
            
            
            String actor = extraerNombreDelActor(info_actor);
            String page_id = extraerIdDeLaPagina(info_actor);
            int publicaciones_actor = 0;
            boolean terminado = false;
            int tamanho_anterior = 0;
            
            URL url = crearSolicitudAlAPI(page_id, crearFechaUntil());
            JSONArray arregloViejo = new JSONArray();
            while(!terminado){
                //				System.out.println(actor + " " + ++i);
                String resultado = enviarSolicitud(url);
                JSONParser parser = new JSONParser();
                JSONObject obj= (JSONObject)parser.parse(resultado);
                JSONArray arreglo = (JSONArray)obj.get("data");
                total_solicitudes+= arreglo.size();
                publicaciones_actor += arreglo.size();
                tamanho_anterior = arreglo.size();
                String fecha_until = "";
                if(noLlegoInformacion(arreglo) || tamanho_anterior < 100  ){
                    terminado = true;
                }
                else{
                    fecha_until = procesarDataDeLaSolicitud(arreglo, actor);
                    String fecha_until_vieja = fecha_until;
                    if(arregloViejo.equals(arreglo)) terminado = true;
                    else arregloViejo = arreglo;
                }
                url = crearSolicitudAlAPI(page_id, fecha_until);
            }
            System.out.println(actor + " " + publicaciones_actor);
            
            info_actor = info.readLine();
        }
        pw_salida.close();
        
    }
    
    /**
     * @param arreglo
     * @param actor
     * @return
     * @throws IOException
     * @throws java.text.ParseException
     */
    public String procesarDataDeLaSolicitud( JSONArray arreglo, String actor) throws IOException, java.text.ParseException{
        String fecha = "";
        for (int i = 0; i < arreglo.size(); i++) {
            JSONObject obj = (JSONObject)arreglo.get(i);
            
            fecha = (String) obj.get("created_time");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            Date date = formatter.parse(fecha);
            formatter = new SimpleDateFormat("dd-MM-yyyy");
            fecha = formatter.format(date);
            
            String contenido;
            boolean contiene = false;
            if((String) obj.get("message") != null)
            {
                contenido = (String) obj.get("message");
            }
            else {
                contenido = (String) obj.get("story");
            }
            if(contenido != null){
                CharSequence cs = "\n";
                CharSequence cs2 = ";";
//				if(contenido.contains(cs)){
//					contenido = contenido.replace(cs, ". ");
//				}
//				if(contenido.contains(cs2)){
//					contenido = contenido.replace(cs2, ", ");
//				CharSequence cs2 = ",";
if(contenido.contains(cs)){
    contenido = contenido.replace(cs, ". ");
}
//				if(contenido.contains(cs2)){
//					contenido = contenido.replace(cs2, "; ");
//				}
//				pw_salida_prueba.println(actor + "," + fecha +"," + contenido + "\n");


for(int j = 0; j < Palabras_clave.length && !contiene; j++){
    String palabra = Palabras_clave[j];
    String csString = palabra;
    if(org.apache.commons.lang3.StringUtils.containsIgnoreCase(contenido,csString)) contiene = true;
    
}
if(contiene) {
    cs = "\n";
    if(contenido.contains(cs)){
        contenido = contenido.replace(cs, ". ");
    }
    if(contenido.contains(cs2)){
        contenido = contenido.replace(cs2, ", ");
    }
    escribirEntrada(actor + ";Facebook;" + fecha + ";" + contenido + "\n" );
}
            }
        }
        return fecha;
    }
    
    /**
     * @param contenido
     * @param palabra
     * @return
     */
    private boolean fraseContienePalabra(String contenido, String palabra) {
        String[] fraseSeparada = contenido.split(" ");
        String[] palabraSeparada = palabra.split(" ");
        boolean cumple = false;
        for(int i = 0; i < fraseSeparada.length && !cumple; i++){
            if(fraseSeparada[i].equalsIgnoreCase(palabraSeparada[0])){
                if(palabraSeparada.length == 1) cumple= true;
                else{
                    boolean cumple2 = true;
                    int k = i;
                    for(int j = 1; j < palabraSeparada.length -1 && cumple2; j++){
                        if(palabraSeparada[j].equalsIgnoreCase(fraseSeparada[k])){
                            k++;
                        }
                        else cumple2= false;
                    }
                    if(cumple2) cumple = true;
                }
            }
        }
        return cumple;
    }
    
    /**
     * @param entrada
     * @throws IOException
     */
    public void escribirEntrada(String entrada) throws IOException{
        pw_salida.println(entrada);
    }
    
    /**
     * @return
     */
    public String crearFechaUntil(){
        Date fecha_until = new Date();
        SimpleDateFormat df =new SimpleDateFormat("yyyy-MM-dd");
        return df.format(fecha_until);
    }
    
    /**
     * @param arreglo
     * @return
     */
    public boolean noLlegoInformacion(JSONArray arreglo){
        return arreglo.isEmpty();
    }
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        try {
            
            App clasePrincipal = new App();
            
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
}
