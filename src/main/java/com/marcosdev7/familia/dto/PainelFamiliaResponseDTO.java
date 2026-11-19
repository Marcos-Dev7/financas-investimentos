package com.marcosdev7.familia.dto;

import java.util.List;

public record PainelFamiliaResponseDTO(
        boolean possuiFamiliaAtiva,
        boolean souTitular,
        String nomeTitular,
        String emailTitular,
        Boolean minhaRendaCompartilhada,
        List<MembroFamiliaItemDTO> membros,
        List<MembroFamiliaItemDTO> convitesRecebidosPendentes
) {}
