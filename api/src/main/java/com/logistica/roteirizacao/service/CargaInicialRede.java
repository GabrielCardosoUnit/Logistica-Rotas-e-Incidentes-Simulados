package com.logistica.roteirizacao.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.logistica.roteirizacao.dto.ImportacaoResponse;
import com.logistica.roteirizacao.exception.ApiException;

/**
 * No perfil dev, sobe a API já com a rede de dados/tratados para a GUI ter
 * algo para mostrar. Se os arquivos não forem encontrados, só avisa no log:
 * a rede ainda pode ser importada por POST /redes/importacoes.
 */
@Component
@ConditionalOnProperty(name = "rede.carga-inicial.habilitada", havingValue = "true")
public class CargaInicialRede implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialRede.class);

    private final RedeService redeService;
    private final File arquivoPontos;
    private final File arquivoVias;

    public CargaInicialRede(RedeService redeService,
                            @Value("${rede.carga-inicial.pontos}") String caminhoPontos,
                            @Value("${rede.carga-inicial.vias}") String caminhoVias) {
        this.redeService = redeService;
        this.arquivoPontos = new File(caminhoPontos);
        this.arquivoVias = new File(caminhoVias);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!arquivoPontos.isFile() || !arquivoVias.isFile()) {
            log.warn("Carga inicial ignorada: arquivos não encontrados ({} / {}). Importe por POST /redes/importacoes.",
                    arquivoPontos.getAbsolutePath(), arquivoVias.getAbsolutePath());
            return;
        }
        try (InputStream pontos = new FileInputStream(arquivoPontos);
             InputStream vias = new FileInputStream(arquivoVias)) {
            ImportacaoResponse r = redeService.importar(pontos, vias);
            log.info("Rede inicial carregada: {} pontos, {} vias, {} linhas rejeitadas (versão {}).",
                    r.pontosImportados(), r.viasImportadas(), r.linhasRejeitadas().size(), r.versaoRede());
        } catch (IOException | ApiException e) {
            log.warn("Carga inicial falhou: {}", e.getMessage());
        }
    }
}
