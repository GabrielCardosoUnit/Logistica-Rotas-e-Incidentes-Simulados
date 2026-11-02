package com.logistica.roteirizacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.logistica.roteirizacao.dto.IncidenteRequest;
import com.logistica.roteirizacao.dto.IncidenteResponse;
import com.logistica.roteirizacao.service.IncidenteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    private final IncidenteService incidenteService;

    public IncidenteController(IncidenteService incidenteService) {
        this.incidenteService = incidenteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenteResponse registrar(@Valid @RequestBody IncidenteRequest pedido) {
        return incidenteService.registrar(pedido);
    }
}
