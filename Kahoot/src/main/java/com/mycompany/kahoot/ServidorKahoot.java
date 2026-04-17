package com.mycompany.kahoot;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class ServidorKahoot {

    static ConcurrentHashMap<String, Jugador> jugadors = new ConcurrentHashMap<>();
    static List<Pregunta> preguntes = new ArrayList<>();
    static int indexPregunta = 0;
    static boolean jocIniciat = false;
    static boolean enPregunta = false;
    static boolean jocFinalitzat = false;
    static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        carregarPreguntes();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));
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

    static void iniciarControladorTemps() {
        new Thread(() -> {
            try {
                while (indexPregunta < preguntes.size()) {
                    enPregunta = true;
                    System.out.println("Pregunta " + indexPregunta);
                    Thread.sleep(20000);
                    enPregunta = false;
                    Thread.sleep(5000);
                    indexPregunta++;
                }
                jocFinalitzat = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // 👤 UNIR
    static class HandlerUnir implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map data = gson.fromJson(body, Map.class);
            String nom = (String) data.get("nom");
            String id = UUID.randomUUID().toString();
            jugadors.put(id, new Jugador(id, nom));
            Map<String, String> resp = Map.of("id", id);
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write(gson.toJson(resp).getBytes());
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // ▶ START
    static class HandlerStart implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!jocIniciat) {
                jocIniciat = true;
                iniciarControladorTemps();
                System.out.println("JOC INICIAT!");
            }
            ex.sendResponseHeaders(200, 0);
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // ❓ PREGUNTA
    static class HandlerPregunta implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, Object> resp = new HashMap<>();
            if (!jocIniciat) {
                resp.put("estat", "LOBBY");
            } else if (jocFinalitzat) {
                resp.put("estat", "FIN");
            } else if (!enPregunta) {
                resp.put("estat", "ESPERA");
            } else {
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
    static class HandlerRespondre implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map data = gson.fromJson(body, Map.class);
            String id = (String) data.get("id");
            int resposta = ((Double) data.get("resposta")).intValue();
            if (enPregunta) {
                Pregunta p = preguntes.get(indexPregunta);
                if (resposta == p.getCorrecta()) {
                    jugadors.get(id).sumarPunts(100);
                }
            }
            ex.sendResponseHeaders(200, 0);
            ex.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // 🏆 PODIUM
    static class HandlerPodium implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            List<Jugador> ranking = new ArrayList<>(jugadors.values());
            ranking.sort((a, b) -> b.getPunts() - a.getPunts());
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().write(gson.toJson(ranking).getBytes());
            ex.close();
        }
    }
}
