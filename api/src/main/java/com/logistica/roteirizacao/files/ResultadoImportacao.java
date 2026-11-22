package com.logistica.roteirizacao.files;

import java.util.List;

import com.logistica.roteirizacao.model.Ponto;
import com.logistica.roteirizacao.model.Via;

/**
 * Pontos e vias válidos lidos dos CSVs, mais as linhas rejeitadas.
 *
 * @param pontos     pontos aceitos
 * @param vias       vias aceitas
 * @param rejeitadas linhas descartadas, com o motivo
 */
public record ResultadoImportacao(List<Ponto> pontos, List<Via> vias, List<LinhaRejeitada> rejeitadas) {

    /**
     * Guarda cópias imutáveis das listas.
     *
     * @param pontos     pontos aceitos
     * @param vias       vias aceitas
     * @param rejeitadas linhas descartadas
     */
    public ResultadoImportacao {
        pontos = List.copyOf(pontos);
        vias = List.copyOf(vias);
        rejeitadas = List.copyOf(rejeitadas);
    }
}
