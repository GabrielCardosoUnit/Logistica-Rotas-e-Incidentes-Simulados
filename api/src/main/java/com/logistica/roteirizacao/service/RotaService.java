package com.logistica.roteirizacao.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistica.roteirizacao.dto.RotaRequest;
import com.logistica.roteirizacao.dto.RotaResponse;
import com.logistica.roteirizacao.exception.CaminhoInexistenteException;
import com.logistica.roteirizacao.exception.RecursoNaoEncontradoException;
import com.logistica.roteirizacao.model.CalculadorRota;
import com.logistica.roteirizacao.model.Grafo;
import com.logistica.roteirizacao.model.Rota;
import com.logistica.roteirizacao.model.RotaCalculada;
import com.logistica.roteirizacao.repository.PontoRepository;
import com.logistica.roteirizacao.repository.RotaCalculadaRepository;

@Service
public class RotaService {

    private final RedeService redeService;
    private final PontoRepository pontoRepository;
    private final RotaCalculadaRepository rotaRepository;
    private final Clock relogio;
    private final CalculadorRota calculador = new CalculadorRota();

    public RotaService(RedeService redeService, PontoRepository pontoRepository,
                       RotaCalculadaRepository rotaRepository, Clock relogio) {
        this.redeService = redeService;
        this.pontoRepository = pontoRepository;
        this.rotaRepository = rotaRepository;
        this.relogio = relogio;
    }

    /**
     * Calcula e grava a rota. Sem caminho não é erro: a tentativa é gravada
     * com status SEM_CAMINHO e a mensagem explica o motivo.
     */
    @Transactional
    public RotaResponse calcular(RotaRequest pedido) {
        String origemId = pedido.origemId().strip();
        String destinoId = pedido.destinoId().strip();
        exigirPonto(origemId, "origemId");
        exigirPonto(destinoId, "destinoId");

        Grafo grafo = redeService.montarGrafo();
        int versao = redeService.versaoAtual();

        RotaCalculada registro;
        try {
            Rota rota = calculador.calcular(grafo, origemId, destinoId, pedido.criterio().estrategia());
            registro = RotaCalculada.calculada(pedido.criterio(), rota, versao, relogio.instant());
        } catch (CaminhoInexistenteException semCaminho) {
            registro = RotaCalculada.semCaminho(origemId, destinoId, pedido.criterio(),
                    semCaminho.getMessage(), versao, relogio.instant());
        }
        return RotaResponse.de(rotaRepository.save(registro), relogio.getZone());
    }

    @Transactional(readOnly = true)
    public RotaResponse buscar(Long id) {
        return rotaRepository.findById(id)
                .map(rota -> RotaResponse.de(rota, relogio.getZone()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("id", "Rota " + id + " não encontrada."));
    }

    private void exigirPonto(String pontoId, String campo) {
        if (!pontoRepository.existsById(pontoId)) {
            throw new RecursoNaoEncontradoException(campo,
                    "Ponto " + pontoId + " não existe na rede. Importe a rede ou escolha outro ponto.");
        }
    }
}
