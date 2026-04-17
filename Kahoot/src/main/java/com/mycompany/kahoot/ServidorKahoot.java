package com.mycompany.kahoot;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class ServidorKahoot {

    // Usamos ConcurrentHashMap porque al ser multihilo evitaremos problemas de concurrencia si se unen muchos a la vez
    static ConcurrentHashMap<String, Jugador> jugadors = new ConcurrentHashMap<>();
    static List<Pregunta> preguntes = new ArrayList<>(); // La lista donde volcamos las preguntas
    
    // Variables de estado super importantes para controlar en qué momento de la partida estamos
    static int indexPregunta = 0;
    static boolean jocIniciat = false;
    static boolean enPregunta = false;
    static boolean jocFinalitzat = false;
    static Gson gson = new Gson(); // Nuestro parseador JSON de confianza

    public static void main(String[] args) throws Exception {
        carregarPreguntes(); // Precargamos las opciones en memoria
        
        // Levantamos un server web en el 8080 nativo de Java
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newFixedThreadPool(10)); // Le metemos 10 hilos para que aguante a la gente
        
        // Mapeamos todas las URLs de la API con sus métodos manejadores
        server.createContext("/api/unir", new HandlerUnir());
        server.createContext("/api/start", new HandlerStart());
        server.createContext("/api/pregunta", new HandlerPregunta());
        server.createContext("/api/respondre", new HandlerRespondre());
        server.createContext("/api/podium", new HandlerPodium());
        
        server.start();
        System.out.println("Servidor iniciat a port 8080");
    }

    static void carregarPreguntes() {
        preguntes.add(new Pregunta("Capital de França?",
                List.of("Madrid", "París", "Roma", "Berlin"), 1));
        preguntes.add(new Pregunta("2+2?",
                List.of("3", "4", "5", "6"), 1));
    }

    // El cerebro del juego: Un hilo independiente que va pasando las preguntas
    static void iniciarControladorTemps() {
        new Thread(() -> { // Arrancamos un hilo secundario para no bloquear el HTTP Server
            try {
                // Mientras queden preguntas en la lista sigue funcionando
                while (indexPregunta < preguntes.size()) {
                    enPregunta = true;
                    System.out.println("Pregunta " + indexPregunta);
                    Thread.sleep(20000); // Tienen 20 segundos de reloj para contestar cada pregunta
                    enPregunta = false;
                    Thread.sleep(5000); // Pausa de 5 seg antes de la siguiente
                    indexPregunta++; // Pasamos a la siguiente pregunta
                }
                jocFinalitzat = true; // Si salta aquí, el juego ha terminado 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // 👤 UNIR
    // Endpoint para que los clientes entren en la partida antes de empezar
    static class HandlerUnir implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            // Leemos el stream y lo transformamos a Mapa gracias a Gson
            String body = new String(ex.getRequestBody().readAllBytes());
            Map data = gson.fromJson(body, Map.class);
            
            // Les generamos un UUID aleatorio y con su nombre los guardamos en la HashMap
            String nom = (String) data.get("nom");
            String id = UUID.randomUUID().toString();
            jugadors.put(id, new Jugador(id, nom));
            
            // Le devolvemos el ID para que lo use luego al contestar
            Map<String, String> resp = Map.of("id", id);
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write(gson.toJson(resp).getBytes());
            ex.close(); // Importante cerrar al acabar el HttpExchange
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // ▶ START
    // Endpoint para que la partida pueda empezar
    static class HandlerStart implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            // Un if de control de error para no poder llamar al /start 20 veces
            if (!jocIniciat) {
                jocIniciat = true;
                iniciarControladorTemps(); // Arranca el cronómetro
                System.out.println("JOC INICIAT!");
            }
            ex.sendResponseHeaders(200, 0);
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // ❓ PREGUNTA
    // El endpoint que las aplicaciones cliente espamean cada 1 segundo para saber el estado de la partida
    static class HandlerPregunta implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, Object> resp = new HashMap<>(); // Construimos lo que les vamos a mandar
            
            if (!jocIniciat) {
                resp.put("estat", "LOBBY");
            } else if (jocFinalitzat) {
                resp.put("estat", "FIN");
            } else if (!enPregunta) {
                resp.put("estat", "ESPERA");
            } else {
                // Si la partida está en marcha, escoge la pregunta actual y se envía
                Pregunta p = preguntes.get(indexPregunta);
                resp.put("estat", "OK");
                resp.put("enunciat", p.getEnunciat());
                resp.put("opcions", p.getOpcions());
                resp.put("index", indexPregunta);
            }
            
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write(gson.toJson(resp).getBytes());
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // ✅ RESPONDRE
    // Endpoint para la respuesta desde el cliente
    static class HandlerRespondre implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            // Lee el usuario que es 
            String body = new String(ex.getRequestBody().readAllBytes());
            Map data = gson.fromJson(body, Map.class);
            String id = (String) data.get("id");
            
            // Como Gson parsea a Double lo cambiamos a int para que no de error
            int resposta = ((Double) data.get("resposta")).intValue();
            
            // Comprueba que es momento de una pregunta y revisa si es la respuesta correcta
            if (enPregunta) {
                Pregunta p = preguntes.get(indexPregunta);
                if (resposta == p.getCorrecta()) {
                    jugadors.get(id).sumarPunts(100); // 100 puntos por acierto
                }
            }
            ex.sendResponseHeaders(200, 0);
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // 🏆 PODIUM
    // Cuando el juego termina, se envía el ranking final
    static class HandlerPodium implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            // Cogemos los valores de ConcurrentHashMap y los pasamos a una lista
            List<Jugador> ranking = new ArrayList<>(jugadors.values());
            
            // Ordenamos usando una Lambda: El de mayor puntuación primero
            ranking.sort((a, b) -> b.getPunts() - a.getPunts());
            
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write(gson.toJson(ranking).getBytes());
            ex.close();
        }
    }
}
