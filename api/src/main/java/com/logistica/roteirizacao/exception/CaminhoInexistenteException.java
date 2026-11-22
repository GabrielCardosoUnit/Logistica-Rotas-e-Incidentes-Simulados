package com.logistica.roteirizacao.exception;

import java.util.List;

/**
 * Destino não alcançável. Não é um erro HTTP: o RotaService trata a exceção
 * e grava a tentativa com status SEM_CAMINHO (decisão do openapi.yaml).
 */
public class CaminhoInexistenteException extends RuntimeException {

    /** Ponto de partida pedido. */
    private final String origemId;
    /** Ponto de chegada pedido. */
    private final String destinoId;
    /** Vias bloqueadas no momento do cálculo. */
    private final List<String> viasBloqueadas;

    /**
     * Cria a exceção, já com a mensagem para o usuário.
     *
     * @param origemId       ponto de partida
     * @param destinoId      ponto de chegada
     * @param viasBloqueadas vias bloqueadas no momento do cálculo
     */
    public CaminhoInexistenteException(String origemId, String destinoId, List<String> viasBloqueadas) {
        super(montarMensagem(origemId, destinoId, viasBloqueadas));
        this.origemId = origemId;
        this.destinoId = destinoId;
        this.viasBloqueadas = List.copyOf(viasBloqueadas);
    }

    private static String montarMensagem(String origemId, String destinoId, List<String> viasBloqueadas) {
        String mensagem = "Não há caminho disponível de " + origemId + " até " + destinoId + ".";
        if (!viasBloqueadas.isEmpty()) {
            mensagem += " Vias bloqueadas: " + String.join(", ", viasBloqueadas) + ".";
        }
        return mensagem;
    }

    /**
     * Ponto de partida.
     *
     * @return id da origem
     */
    public String getOrigemId() {
        return origemId;
    }

    /**
     * Ponto de chegada.
     *
     * @return id do destino
     */
    public String getDestinoId() {
        return destinoId;
    }

    /**
     * Vias bloqueadas no momento do cálculo.
     *
     * @return ids das vias, em ordem crescente (lista imutável)
     */
    public List<String> getViasBloqueadas() {
        return viasBloqueadas;
    }
}
