package com.mycompany.kahoot;

public class Jugador {
    private String id;
    private String nom;
    private int punts;

    public Jugador(String id, String nom) {
        this.id = id;
        this.nom = nom;
        this.punts = 0;
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

    public void sumarPunts(int p) {
        this.punts += p;
    }
}
