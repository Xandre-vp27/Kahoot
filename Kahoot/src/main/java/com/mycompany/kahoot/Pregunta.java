package com.mycompany.kahoot;

import java.util.List;

public class Pregunta {
    
    private String enunciat;
    private List<String> opcions;
    private int correcta;

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
