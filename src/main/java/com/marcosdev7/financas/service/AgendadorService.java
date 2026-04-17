package com.marcosdev7.financas.service;

import com.marcosdev7.financas.domain.Despesa;
import com.marcosdev7.financas.domain.Mes;
import com.marcosdev7.financas.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AgendadorService {

    private final DespesaRepository despesaRepository;

    @Scheduled(cron = "0 0 0 1 * *")
    public void projetarDespesasMesSeguinte() {

        Mes mesAtual = Mes.valueOf(LocalDate.now().getMonth().name());
        Mes mesAnterior = mesAtual.anterior();
        var despesasFixas = despesaRepository.findByIsFixaTrueAndMes(mesAnterior);
        despesasFixas.stream()
                .forEach(d ->{
                    Mes proximoMes = d.getMes().proximo();
                    LocalDate proximaData = d.getDataVencimento().plusMonths(1);

                    Despesa novaDespesa = new Despesa(
                            d.getDescricao(),
                            proximoMes,
                            d.getValor(),
                            d.getIsFixa(),
                            proximaData,
                            d.getCategoria()
                    );

                    despesaRepository.save(novaDespesa);
                });
    }
}
