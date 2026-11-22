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

/** Endpoints da rede: importação de pontos e vias a partir de CSV ({@code POST /redes/importacoes}). */
@RestController
@RequestMapping("/redes")
public class RedeController {

    private final RedeService redeService;

    /**
     * Cria o controller.
     *
     * @param redeService serviço que importa e mantém a rede
     */
    public RedeController(RedeService redeService) {
        this.redeService = redeService;
    }

    /**
     * Substitui a rede atual pelos pontos e vias dos CSVs enviados.
     *
     * @param pontos arquivo pontos.csv (campo {@code pontos} do formulário)
     * @param vias   arquivo vias.csv (campo {@code vias} do formulário)
     * @return quantidades importadas, nova versão da rede e linhas rejeitadas
     * @throws IOException se não for possível ler os arquivos enviados
     */
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
