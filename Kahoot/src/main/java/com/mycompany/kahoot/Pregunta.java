package com.mycompany.kahoot;

import java.util.List;

// Guardamos la estructura básica de cada pregunta
public class Pregunta {
    
    private String enunciat; // Lo que leemos por pantalla
    private List<String> opcions; // Las 4 opciones posibles
    private int correcta; // El índice (0 a 3) de la respuesta que está bien

    public Pregunta(String enunciat, List<String> opcions, int correcta) {
        this.enunciat = enunciat;
        this.opcions = opcions;
        this.correcta = correcta;
    }

    public String getEnunciat() {
        return enunciat;
    }

    public List<String> getOpcions() {
        return opcions;
    }

    public int getCorrecta() {
        return correcta;
    }

}
