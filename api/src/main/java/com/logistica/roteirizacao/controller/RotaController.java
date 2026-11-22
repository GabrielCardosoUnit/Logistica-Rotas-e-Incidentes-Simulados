package com.logistica.roteirizacao.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistica.roteirizacao.dto.RotaRequest;
import com.logistica.roteirizacao.dto.RotaResponse;
import com.logistica.roteirizacao.service.RotaService;

import jakarta.validation.Valid;

/** Endpoints de rotas: cálculo ({@code POST /rotas}) e consulta ({@code GET /rotas/{id}}). */
@RestController
@RequestMapping("/rotas")
public class RotaController {

    private final RotaService rotaService;

    /**
     * Cria o controller.
     *
     * @param rotaService serviço que calcula e consulta rotas
     */
    public RotaController(RotaService rotaService) {
        this.rotaService = rotaService;
    }

    /**
     * Calcula uma rota. Responde 201 também quando não há caminho (status SEM_CAMINHO no corpo).
     *
     * @param pedido origem, destino e critério
     * @return rota gravada, com o cabeçalho {@code Location} apontando para ela
     */
    @PostMapping
    public ResponseEntity<RotaResponse> calcular(@Valid @RequestBody RotaRequest pedido) {
        RotaResponse rota = rotaService.calcular(pedido);
        return ResponseEntity.created(URI.create("/rotas/" + rota.id())).body(rota);
    }

    /**
     * Consulta uma rota já calculada.
     *
     * @param id identificador da rota
     * @return a rota gravada
     */
    @GetMapping("/{id}")
    public RotaResponse buscar(@PathVariable("id") Long id) {
        return rotaService.buscar(id);
    }
}
