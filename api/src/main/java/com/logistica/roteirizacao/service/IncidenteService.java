package com.logistica.roteirizacao.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistica.roteirizacao.dto.IncidenteRequest;
import com.logistica.roteirizacao.dto.IncidenteResponse;
import com.logistica.roteirizacao.exception.ConflitoException;
import com.logistica.roteirizacao.exception.RecursoNaoEncontradoException;
import com.logistica.roteirizacao.model.Incidente;
import com.logistica.roteirizacao.model.Via;
import com.logistica.roteirizacao.repository.IncidenteRepository;
import com.logistica.roteirizacao.repository.ViaRepository;

@Service
public class IncidenteService {

    private final ViaRepository viaRepository;
    private final IncidenteRepository incidenteRepository;
    private final RedeService redeService;
    private final Clock relogio;

    public IncidenteService(ViaRepository viaRepository, IncidenteRepository incidenteRepository,
                            RedeService redeService, Clock relogio) {
        this.viaRepository = viaRepository;
        this.incidenteRepository = incidenteRepository;
        this.redeService = redeService;
        this.relogio = relogio;
    }

    /** Bloqueia a via (nos dois sentidos) e sobe a versão da rede. */
    @Transactional
    public IncidenteResponse registrar(IncidenteRequest pedido) {
        String viaId = pedido.viaId().strip();
        Via via = viaRepository.findById(viaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("viaId", "Via " + viaId + " não existe na rede."));

        if (!via.isHabilitada()) {
            throw new ConflitoException("viaId", "A via " + viaId + " já está desabilitada desde a importação.");
        }
        incidenteRepository.findFirstByViaIdAndAtivoTrue(viaId).ifPresent(existente -> {
            throw new ConflitoException("viaId",
                    "A via " + viaId + " já está bloqueada pelo incidente " + existente.getId() + ".");
        });

        Incidente incidente = incidenteRepository.save(new Incidente(via, pedido.descricao(), relogio.instant()));
        int versao = redeService.incrementarVersao();
        return IncidenteResponse.de(incidente, versao, relogio.getZone());
    }
}
