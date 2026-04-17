package com.mycompany.kahoot;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class Kahoot {

    static String URL = "http://localhost:8080";
    static HttpClient client = HttpClient.newHttpClient();
    static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nom: ");
        String nom = sc.nextLine();
        String id = unir(nom);

        int lastQuestionIndex = -1;

        while (true) {
            Map estado = get("/api/estat");
            String estat = (String) estado.get("estat");

            if ("LOBBY".equals(estat)) {
                System.out.println("Esperant inici del joc...");
            } else if ("OK".equals(estat)) {
                int currentIndex = ((Double) estado.get("index")).intValue();
                
                if (currentIndex != lastQuestionIndex) {
                    lastQuestionIndex = currentIndex;
                    
                    System.out.println("\nPregunta: " + estado.get("pregunta"));
                    List<String> opcions = (List<String>) estado.get("opcions");
                    if (opcions != null) {
                        for (int i = 0; i < opcions.size(); i++) {
                            System.out.println(i + ". " + opcions.get(i));
                        }
                    }
                    
                    System.out.print("Respuesta (0-3): ");
                    int respuesta = sc.nextInt();
                    
                    post("/api/respondre", Map.of("id", id, "resposta", respuesta));
                }
            } else if ("FIN".equals(estat)) {
                System.out.println("\nEl juego ha terminado!");
                List<Map> podium = getList("/api/podium");
                System.out.println("--- PODIO ---");
                for (int i = 0; i < podium.size(); i++) {
                    Map jugador = podium.get(i);
                    int puntos = ((Double) jugador.get("punts")).intValue();
                    System.out.println((i + 1) + ". " + jugador.get("nom") + " - " + puntos + " puntos");
                }
                break;
            }

            Thread.sleep(1000);
        }

    }

    static String unir(String nom) throws Exception {
        Map resp = post("/api/unir", Map.of("nom", nom));
        return (String) resp.get("id");
    }

    static Map post(String path, Map data) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.body() == null || res.body().isEmpty())
            return new HashMap<>();
        return gson.fromJson(res.body(), Map.class);
    }

    static Map get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(res.body(), Map.class);
    }

    static List<Map> getList(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(res.body(), List.class);
    }
}
