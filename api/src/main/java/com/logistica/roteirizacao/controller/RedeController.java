package com.logistica.roteirizacao.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.logistica.roteirizacao.dto.ImportacaoResponse;
import com.logistica.roteirizacao.service.RedeService;

@RestController
@RequestMapping("/redes")
public class RedeController {

    private final RedeService redeService;

    public RedeController(RedeService redeService) {
        this.redeService = redeService;
    }

    @PostMapping(path = "/importacoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacaoResponse importar(@RequestPart("pontos") MultipartFile pontos,
                                       @RequestPart("vias") MultipartFile vias) throws IOException {
        try (InputStream pontosCsv = pontos.getInputStream();
             InputStream viasCsv = vias.getInputStream()) {
            return redeService.importar(pontosCsv, viasCsv);
        }
    }
}
