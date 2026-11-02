package com.logistica.roteirizacao.files;

import java.util.List;

import com.logistica.roteirizacao.model.Ponto;
import com.logistica.roteirizacao.model.Via;

/** Pontos e vias válidos lidos dos CSVs, mais as linhas rejeitadas. */
public record ResultadoImportacao(List<Ponto> pontos, List<Via> vias, List<LinhaRejeitada> rejeitadas) {

    public ResultadoImportacao {
        pontos = List.copyOf(pontos);
        vias = List.copyOf(vias);
        rejeitadas = List.copyOf(rejeitadas);
    }
}
