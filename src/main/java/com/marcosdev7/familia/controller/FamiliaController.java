package com.marcosdev7.familia.controller;

import com.marcosdev7.familia.dto.*;
import com.marcosdev7.familia.service.FamiliaService;
import com.marcosdev7.financas.domain.Mes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Família & Membros", description = "Gestão de membros da família, convites e visão consolidada")
@RequestMapping("/familia")
public class FamiliaController {

    private final FamiliaService familiaService;

    @Operation(summary = "Retorna o painel familiar do usuário logado",
            description = "Lista membros ativos, convites enviados/recebidos e status de compartilhamento de renda.")
    @GetMapping
    public ResponseEntity<PainelFamiliaResponseDTO> obterPainelFamilia() {
        return ResponseEntity.ok(familiaService.obterPainelFamilia());
    }

    @Operation(summary = "Envia um convite para um novo membro da família",
            description = "Gera um token com validade de 48h e envia por e-mail para o convidado.")
    @PostMapping("/convites")
    public ResponseEntity<MembroFamiliaItemDTO> enviarConvite(@RequestBody @Valid ConviteFamiliaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familiaService.enviarConvite(dto));
    }

    @Operation(summary = "Aceita um convite familiar via token",
            description = "Valida o token, o prazo de 48h e o e-mail do usuário logado, ativando o vínculo.")
    @PostMapping("/convites/aceitar")
    public ResponseEntity<PainelFamiliaResponseDTO> aceitarConvite(@RequestBody @Valid AceitarConviteDTO dto) {
        return ResponseEntity.ok(familiaService.aceitarConvite(dto));
    }

    @Operation(summary = "Ativa ou desativa o compartilhamento da própria renda na família",
            description = "Permite que cada pessoa escolha a qualquer momento se sua renda entra na soma familiar.")
    @PatchMapping("/compartilhar-renda")
    public ResponseEntity<PainelFamiliaResponseDTO> alterarCompartilhamentoRenda(@RequestBody @Valid CompartilharRendaDTO dto) {
        return ResponseEntity.ok(familiaService.alterarCompartilhamentoRenda(dto));
    }

    @Operation(summary = "Ativa ou desativa o compartilhamento das próprias despesas na visão familiar",
            description = "Quando desligado, as despesas do usuário deixam de aparecer no consolidado e nos totais da família.")
    @PatchMapping("/compartilhar-despesas")
    public ResponseEntity<PainelFamiliaResponseDTO> alterarCompartilhamentoDespesas(@RequestBody @Valid CompartilharDespesasDTO dto) {
        return ResponseEntity.ok(familiaService.alterarCompartilhamentoDespesas(dto));
    }

    @Operation(summary = "Remove um membro da família ou cancela convite",
            description = "Corta imediatamente o acesso do membro aos dados familiares.")
    @DeleteMapping("/membros/{vinculoId}")
    public ResponseEntity<Void> removerMembro(@PathVariable Long vinculoId) {
        familiaService.removerMembroOuConvite(vinculoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista as despesas consolidadas da família (Somente Leitura para membros)",
            description = "Retorna as despesas compartilhadas de membros ativos da família, identificando o responsável.")
    @GetMapping("/despesas")
    public ResponseEntity<List<DespesaFamiliaResponseDTO>> listarDespesasFamilia(
            @RequestParam(required = false) Mes mes) {
        return ResponseEntity.ok(familiaService.listarDespesasFamilia(mes));
    }

    @Operation(summary = "Gestor dá baixa (paga) em uma despesa compartilhada da família",
            description = "Permitido exclusivamente para o gestor (criador) da família em despesas compartilhadas por membros ativos.")
    @PatchMapping("/despesas/{despesaId}/pagar")
    public ResponseEntity<DespesaFamiliaResponseDTO> pagarDespesaFamilia(@PathVariable Long despesaId) {
        return ResponseEntity.ok(familiaService.pagarDespesaFamilia(despesaId));
    }

    @Operation(summary = "Gestor estorna pagamento de uma despesa compartilhada da família",
            description = "Permitido exclusivamente para o gestor (criador) da família em despesas compartilhadas por membros ativos.")
    @PatchMapping("/despesas/{despesaId}/estornar")
    public ResponseEntity<DespesaFamiliaResponseDTO> estornarDespesaFamilia(@PathVariable Long despesaId) {
        return ResponseEntity.ok(familiaService.estornarDespesaFamilia(despesaId));
    }

    @Operation(summary = "Calcula o resumo financeiro consolidado da família no mês",
            description = "Soma as rendas de quem ativou o compartilhamento e subtrai as despesas familiares pagas.")
    @GetMapping("/resumo/{mes}")
    public ResponseEntity<ResumoFamiliaResponseDTO> calcularResumoFamilia(@PathVariable Mes mes) {
        return ResponseEntity.ok(familiaService.calcularResumoFamilia(mes));
    }
}
