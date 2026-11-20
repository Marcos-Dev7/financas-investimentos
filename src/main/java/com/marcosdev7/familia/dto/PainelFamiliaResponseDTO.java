package com.marcosdev7.familia.dto;

import java.util.List;

public record PainelFamiliaResponseDTO(
        boolean possuiFamiliaAtiva,
        boolean souTitular,
        String nomeTitular,
        String emailTitular,
        Boolean minhaRendaCompartilhada,
        Boolean minhasDespesasCompartilhadas,
        List<MembroFamiliaItemDTO> membros,
        List<MembroFamiliaItemDTO> convitesRecebidosPendentes
) {
    public PainelFamiliaResponseDTO(
            boolean possuiFamiliaAtiva,
            boolean souTitular,
            String nomeTitular,
            String emailTitular,
            Boolean minhaRendaCompartilhada,
            List<MembroFamiliaItemDTO> membros,
            List<MembroFamiliaItemDTO> convitesRecebidosPendentes) {
        this(possuiFamiliaAtiva, souTitular, nomeTitular, emailTitular, minhaRendaCompartilhada, true, membros, convitesRecebidosPendentes);
    }
}
