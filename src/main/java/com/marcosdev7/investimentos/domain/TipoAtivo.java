package com.marcosdev7.investimentos.domain;

public enum TipoAtivo {
    TESOURO_DIRETO(CategoriaAtivo.RENDA_FIXA),
    CDB(CategoriaAtivo.RENDA_FIXA),
    LCI(CategoriaAtivo.RENDA_FIXA),
    DEBENTURES(CategoriaAtivo.RENDA_FIXA),
    ACOES(CategoriaAtivo.RENDA_VARIAVEL),
    FIIS(CategoriaAtivo.RENDA_VARIAVEL),
    CRIPTOATIVOS(CategoriaAtivo.RENDA_VARIAVEL)
    ;


    private final CategoriaAtivo categoriaAtivo;
    TipoAtivo(CategoriaAtivo categoriaAtivo) {
        this.categoriaAtivo = categoriaAtivo;
    }
}
