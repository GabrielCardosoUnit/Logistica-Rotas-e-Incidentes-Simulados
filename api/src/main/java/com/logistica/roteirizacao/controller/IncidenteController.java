package com.logistica.roteirizacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.logistica.roteirizacao.dto.IncidenteRequest;
import com.logistica.roteirizacao.dto.IncidenteResponse;
import com.logistica.roteirizacao.service.IncidenteService;

import jakarta.validation.Valid;

/**
 * Endpoints de incidentes: bloqueio ({@code POST /incidentes}) e
 * encerramento ({@code POST /incidentes/{id}/encerramento}) de vias.
 */
@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    private final IncidenteService incidenteService;

    /**
     * Cria o controller.
     *
     * @param incidenteService serviço que aplica as regras de incidente
     */
    public IncidenteController(IncidenteService incidenteService) {
        this.incidenteService = incidenteService;
    }

    /**
     * Registra um incidente e bloqueia a via informada.
     *
     * @param pedido via e descrição do incidente
     * @return incidente registrado, com a nova versão da rede
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenteResponse registrar(@Valid @RequestBody IncidenteRequest pedido) {
        return incidenteService.registrar(pedido);
    }

    /**
     * Encerra o incidente: desbloqueia a via. O incidente fica no histórico com ativo=false.
     *
     * @param id identificador do incidente
     * @return incidente encerrado, com a nova versão da rede
     */
    @PostMapping("/{id}/encerramento")
    public IncidenteResponse encerrar(@PathVariable("id") Long id) {
        return incidenteService.encerrar(id);
    }
}
