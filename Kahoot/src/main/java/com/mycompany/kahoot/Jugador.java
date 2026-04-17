package com.mycompany.kahoot;

// Clase para montar el perfil de cada jugador que entra a la partida
public class Jugador {
    private String id; // El UUID único para no confundir si hay gente con el mismo nombre
    private String nom;
    private int punts; // Puntos que va acumulando

    public Jugador(String id, String nom) {
        this.id = id;
        this.nom = nom;
        this.punts = 0; // Arranca con 0 puntos
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getPunts() {
        return punts;
    }

    // Método para darle los puntos cuando acierta la pregunta
    public void sumarPunts(int p) {
        this.punts += p;
    }
}
