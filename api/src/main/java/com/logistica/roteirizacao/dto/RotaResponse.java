package com.logistica.roteirizacao.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.logistica.roteirizacao.model.RotaCalculada;
import com.logistica.roteirizacao.model.StatusRota;
import com.logistica.roteirizacao.model.TipoCriterio;

/**
 * Rota calculada (ou tentativa sem caminho) devolvida pela API.
 *
 * @param id               identificador da rota
 * @param origemId         ponto de partida
 * @param destinoId        ponto de chegada
 * @param criterio         critério usado no cálculo
 * @param status           {@link StatusRota#CALCULADA} ou {@link StatusRota#SEM_CAMINHO}
 * @param pontos           pontos percorridos, em ordem
 * @param vias             vias percorridas, em ordem
 * @param distanciaTotalKm distância total, ou {@code null} sem caminho
 * @param tempoTotalMin    tempo total, ou {@code null} sem caminho
 * @param versaoRede       versão da rede usada no cálculo
 * @param mensagem         explicação para o usuário, quando houver
 * @param calculadaEm      momento do cálculo
 */
public record RotaResponse(
        Long id,
        String origemId,
        String destinoId,
        TipoCriterio criterio,
        StatusRota status,
        List<String> pontos,
        List<String> vias,
        Double distanciaTotalKm,
        Double tempoTotalMin,
        int versaoRede,
        String mensagem,
        OffsetDateTime calculadaEm) {

    /**
     * Converte a entidade para a resposta da API.
     *
     * @param rota rota gravada
     * @param fuso fuso usado para exibir {@code calculadaEm}
     * @return a resposta
     */
    public static RotaResponse de(RotaCalculada rota, ZoneId fuso) {
        return new RotaResponse(
                rota.getId(),
                rota.getOrigemId(),
                rota.getDestinoId(),
                rota.getCriterio(),
                rota.getStatus(),
                rota.getPontos(),
                rota.getVias(),
                rota.getDistanciaTotalKm(),
                rota.getTempoTotalMin(),
                rota.getVersaoRede(),
                rota.getMensagem(),
                OffsetDateTime.ofInstant(rota.getCalculadaEm(), fuso));
    }
}
