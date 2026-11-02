package com.logistica.roteirizacao.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.logistica.roteirizacao.exception.CaminhoInexistenteException;

/** Rede de validação de 5 nós do README, com o resultado calculado à mão. */
class CalculadorRotaTest {

    private final CalculadorRota calculador = new CalculadorRota();
    private Grafo grafo;

    @BeforeEach
    void montarRedeDeValidacao() {
        grafo = new Grafo();
        Map<String, Ponto> p = new HashMap<>();
        for (String id : new String[] {"A", "B", "C", "D", "E"}) {
            p.put(id, new Ponto(id, "Ponto " + id, 0, 0));
            grafo.adicionarPonto(p.get(id));
        }
        grafo.adicionarVia(new Via("V1", p.get("A"), p.get("B"), 4, 14, true));
        grafo.adicionarVia(new Via("V2", p.get("A"), p.get("C"), 2, 15, true));
        grafo.adicionarVia(new Via("V3", p.get("B"), p.get("D"), 5, 5, true));
        grafo.adicionarVia(new Via("V4", p.get("C"), p.get("D"), 8, 6, true));
        grafo.adicionarVia(new Via("V5", p.get("C"), p.get("E"), 11, 4, true));
        grafo.adicionarVia(new Via("V6", p.get("D"), p.get("E"), 2, 3, true));
    }

    @Test
    void menorDistanciaDeAateE() {
        Rota rota = calculador.calcular(grafo, "A", "E", new MenorDistancia());

        assertThat(rota.getPontos()).containsExactly("A", "B", "D", "E");
        assertThat(rota.getVias()).containsExactly("V1", "V3", "V6");
        assertThat(rota.getDistanciaTotalKm()).isEqualTo(11.0);
        assertThat(rota.getTempoTotalMin()).isEqualTo(22.0);
    }

    @Test
    void menorTempoDeAateE() {
        Rota rota = calculador.calcular(grafo, "A", "E", new MenorTempo());

        assertThat(rota.getPontos()).containsExactly("A", "C", "E");
        assertThat(rota.getTempoTotalMin()).isEqualTo(19.0);
    }

    @Test
    void criterioPadraoEhMenorDistancia() {
        assertThat(calculador.calcular(grafo, "A", "E").getPontos()).containsExactly("A", "B", "D", "E");
    }

    @Test
    void comV3BloqueadaARotaMudaParaACDE() {
        grafo.bloquearVia("V3");

        Rota rota = calculador.calcular(grafo, "A", "E", new MenorDistancia());

        assertThat(rota.getPontos()).containsExactly("A", "C", "D", "E");
        assertThat(rota.getDistanciaTotalKm()).isEqualTo(12.0);
    }

    @Test
    void semCaminhoQuandoV5eV6EstaoBloqueadas() {
        grafo.bloquearVia("V5");
        grafo.bloquearVia("V6");

        assertThatThrownBy(() -> calculador.calcular(grafo, "A", "E", new MenorDistancia()))
                .isInstanceOf(CaminhoInexistenteException.class)
                .hasMessage("Não há caminho disponível de A até E. Vias bloqueadas: V5, V6.");
    }

    @Test
    void origemIgualAoDestinoTemUmPontoETotaisZero() {
        Rota rota = calculador.calcular(grafo, "C", "C", new MenorTempo());

        assertThat(rota.getPontos()).containsExactly("C");
        assertThat(rota.getVias()).isEmpty();
        assertThat(rota.getDistanciaTotalKm()).isZero();
    }

    @Test
    void viaDeMaoDuplaPermiteOSentidoInverso() {
        Rota rota = calculador.calcular(grafo, "E", "A", new MenorDistancia());

        assertThat(rota.getPontos()).containsExactly("E", "D", "B", "A");
    }

    @Test
    void viaDesabilitadaNaImportacaoNaoEntraNoCalculo() {
        Grafo outro = new Grafo();
        Ponto a = new Ponto("A", "A", 0, 0);
        Ponto b = new Ponto("B", "B", 0, 0);
        outro.adicionarPonto(a);
        outro.adicionarPonto(b);
        outro.adicionarVia(new Via("V1", a, b, 1, 1, false));

        assertThatThrownBy(() -> calculador.calcular(outro, "A", "B"))
                .isInstanceOf(CaminhoInexistenteException.class)
                .hasMessageContaining("Vias bloqueadas: V1.");
    }

    @Test
    void viaComPesoNaoPositivoEhRecusada() {
        Ponto a = new Ponto("A", "A", 0, 0);
        Ponto b = new Ponto("B", "B", 0, 0);

        assertThatThrownBy(() -> new Via("V9", a, b, 0, 5, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distanciaKm deve ser maior que zero");
        assertThatThrownBy(() -> new Via("V9", a, b, 3, -1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tempoMin deve ser maior que zero");
    }
}
