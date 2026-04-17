package com.marcosdev7.financas.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Mes {
    JANEIRO("janeiro"),
    FEVEREIRO("fevereiro"),
    MARCO("marco"),
    ABRIL("abril"),
    MAIO("maio"),
    JUNHO("junho"),
    JULHO("julho"),
    AGOSTO("agosto"),
    SETEMBRO("setembro"),
    OUTUBRO("outubro"),
    NOVEMBRO("novembro"),
    DEZEMBRO("dezembro");

    private final String mesCase;

    Mes(String mesCase) {
        this.mesCase = mesCase;
    }

    @JsonCreator
    public static Mes fromString(String value) {
        return Mes.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getMesCase() {
        return mesCase;
    }

    public Mes proximo(){
        Mes[] meses = Mes.values();
        int proximoIndice = (this.ordinal() + 1) % meses.length;
        return meses[proximoIndice];
    }

    public Mes anterior(){
        Mes[] meses = Mes.values();
        int indiceAnterior = (this.ordinal() - 1 + meses.length) % meses.length;
        return meses[indiceAnterior];
    }
}
