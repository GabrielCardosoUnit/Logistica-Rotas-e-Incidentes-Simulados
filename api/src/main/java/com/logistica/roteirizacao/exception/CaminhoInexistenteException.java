package com.logistica.roteirizacao.exception;

import java.util.List;

/**
 * Destino não alcançável. Não é um erro HTTP: o RotaService trata a exceção
 * e grava a tentativa com status SEM_CAMINHO (decisão do openapi.yaml).
 */
public class CaminhoInexistenteException extends RuntimeException {

    private final String origemId;
    private final String destinoId;
    private final List<String> viasBloqueadas;

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

    public String getOrigemId() {
        return origemId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public List<String> getViasBloqueadas() {
        return viasBloqueadas;
    }
}
