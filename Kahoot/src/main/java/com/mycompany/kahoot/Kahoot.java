package com.mycompany.kahoot;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.*;
import java.util.*;

// Clase que ejecuta cada jugador
public class Kahoot {

    // Apuntamos al servidor local
    static String URL = "http://localhost:8080";
    static HttpClient client = HttpClient.newHttpClient();
    static Gson gson = new Gson(); // Para tratar con los JSON

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        
        // Primero, pedimos el nombre al jugador 
        System.out.print("Nom: ");
        String nom = sc.nextLine();
        
        // Nos unimos al server y nos guardamos el ID único (UUID)
        String id = unir(nom);

        // Esto lo uso para saber por qué pregunta vamos y no imprimirla mil veces haciendo spam
        int lastQuestionIndex = -1;

        // Bucle infinito: vamos preguntando al servidor constantemente "¿qué está pasando?"
        while (true) {
            // Le pegamos al endpoint principal que nos chiva el estado de la partida
            Map estado = get("/api/pregunta");
            String estat = (String) estado.get("estat");

            if ("LOBBY".equals(estat)) {
                System.out.println("Esperant inici del joc...");
            } else if ("ESPERA".equals(estat)) {
                // No hacemos nada, simplemente el servidor está en la pausa entre preguntas
            } else if ("OK".equals(estat)) {
                // Sacamos el número de pregunta (Ojo: Gson parsea los números como Double)
                int currentIndex = ((Double) estado.get("index")).intValue();
                
                // Validamos que sea una pregunta nueva
                if (currentIndex != lastQuestionIndex) {
                    lastQuestionIndex = currentIndex;
                    
                    System.out.println("\nPregunta: " + estado.get("enunciat"));
                    
                    // Imprimimos la lista de opciones
                    List<String> opcions = (List<String>) estado.get("opcions");
                    if (opcions != null) {
                        for (int i = 0; i < opcions.size(); i++) {
                            System.out.println(i + ". " + opcions.get(i));
                        }
                    }
                    
                    // Nos quedamos bloqueados esperando que el usuario meta el número
                    System.out.print("Respuesta (0-3): ");
                    int respuesta = sc.nextInt();
                    
                    // Mandamos la respuesta al servidor para ver qué tal
                    post("/api/respondre", Map.of("id", id, "resposta", respuesta));
                }
            } else if ("FIN".equals(estat)) {
                // Fin de juego, pillamos la info del podio
                System.out.println("\nEl juego ha terminado!");
                List<Map> podium = getList("/api/podium");
                System.out.println("--- PODIO ---");
                for (int i = 0; i < podium.size(); i++) {
                    Map jugador = podium.get(i);
                    int puntos = ((Double) jugador.get("punts")).intValue();
                    System.out.println((i + 1) + ". " + jugador.get("nom") + " - " + puntos + " puntos");
                }
                break; // Rompemos el while para matar el programa
            }

            // Descansamos 1 segundo para no reventar el servidor a peticiones
            Thread.sleep(1000);
        }

    }

    // --- Métodos HTTP de ayuda ---

    // Petición para unirnos 
    static String unir(String nom) throws Exception {
        Map resp = post("/api/unir", Map.of("nom", nom));
        return (String) resp.get("id");
    }

    // Método genérico para tirar un POST pasándole un Map
    static Map post(String path, Map data) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data))) // Pasamos el Map a JSON
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.body() == null || res.body().isEmpty())
            return new HashMap<>();
        return gson.fromJson(res.body(), Map.class);
    }

    // Lo mismo pero con GET
    static Map get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(res.body(), Map.class);
    }

    // GET especial para cuando el endpoint nos devuelve un Array de objetos (List)
    static List<Map> getList(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL + path))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(res.body(), List.class);
    }
}
