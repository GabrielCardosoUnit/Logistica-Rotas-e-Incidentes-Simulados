package com.logistica.roteirizacao.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistica.roteirizacao.dto.ImportacaoResponse;
import com.logistica.roteirizacao.dto.LinhaRejeitadaResponse;
import com.logistica.roteirizacao.exception.ValidacaoException;
import com.logistica.roteirizacao.files.ImportadorRedeCsv;
import com.logistica.roteirizacao.files.ResultadoImportacao;
import com.logistica.roteirizacao.model.EstadoRede;
import com.logistica.roteirizacao.model.Grafo;
import com.logistica.roteirizacao.model.Incidente;
import com.logistica.roteirizacao.model.Ponto;
import com.logistica.roteirizacao.model.Via;
import com.logistica.roteirizacao.repository.EstadoRedeRepository;
import com.logistica.roteirizacao.repository.IncidenteRepository;
import com.logistica.roteirizacao.repository.PontoRepository;
import com.logistica.roteirizacao.repository.ViaRepository;

/** Importação da rede, versão da rede e montagem do grafo a partir do banco. */
@Service
public class RedeService {

    private final PontoRepository pontoRepository;
    private final ViaRepository viaRepository;
    private final IncidenteRepository incidenteRepository;
    private final EstadoRedeRepository estadoRedeRepository;
    private final ImportadorRedeCsv importador = new ImportadorRedeCsv();

    public RedeService(PontoRepository pontoRepository, ViaRepository viaRepository,
                       IncidenteRepository incidenteRepository, EstadoRedeRepository estadoRedeRepository) {
        this.pontoRepository = pontoRepository;
        this.viaRepository = viaRepository;
        this.incidenteRepository = incidenteRepository;
        this.estadoRedeRepository = estadoRedeRepository;
    }

    /**
     * Substitui a rede atual pela dos CSVs. Incidentes da rede anterior são
     * apagados junto (as vias podem nem existir mais). Rotas já calculadas
     * continuam no histórico, pois guardam só ids e a versão da rede.
     */
    @Transactional
    public ImportacaoResponse importar(InputStream pontosCsv, InputStream viasCsv) {
        ResultadoImportacao resultado = importador.ler(pontosCsv, viasCsv);
        exigirAlgumaLinhaValida(resultado.pontos().isEmpty(), "pontos", ImportadorRedeCsv.ARQUIVO_PONTOS, resultado);
        exigirAlgumaLinhaValida(resultado.vias().isEmpty(), "vias", ImportadorRedeCsv.ARQUIVO_VIAS, resultado);

        incidenteRepository.deleteAllInBatch();
        viaRepository.deleteAllInBatch();
        pontoRepository.deleteAllInBatch();
        pontoRepository.saveAll(resultado.pontos());
        viaRepository.saveAll(resultado.vias());
        int versao = incrementarVersao();

        List<LinhaRejeitadaResponse> rejeitadas = resultado.rejeitadas().stream()
                .map(LinhaRejeitadaResponse::de)
                .toList();
        return new ImportacaoResponse(versao, resultado.pontos().size(), resultado.vias().size(), rejeitadas);
    }

    private static void exigirAlgumaLinhaValida(boolean vazio, String campo, String arquivo,
                                                ResultadoImportacao resultado) {
        if (!vazio) {
            return;
        }
        String detalhe = resultado.rejeitadas().stream()
                .filter(l -> l.arquivo().equals(arquivo))
                .findFirst()
                .map(l -> " Primeiro erro na linha " + l.linha() + ": " + l.motivo())
                .orElse("");
        throw new ValidacaoException(campo, "Nenhuma linha válida em " + arquivo + "." + detalhe);
    }

    @Transactional(readOnly = true)
    public int versaoAtual() {
        return estadoRedeRepository.findById(EstadoRede.ID_UNICO)
                .map(EstadoRede::getVersao)
                .orElse(0);
    }

    /** Chamado dentro da transação de quem altera a rede (importação ou incidente). */
    @Transactional
    public int incrementarVersao() {
        EstadoRede estado = estadoRedeRepository.findById(EstadoRede.ID_UNICO)
                .orElseGet(EstadoRede::inicial);
        int versao = estado.incrementarVersao();
        estadoRedeRepository.save(estado);
        return versao;
    }

    /** Grafo com a rede atual; vias desabilitadas ou com incidente ativo entram bloqueadas. */
    @Transactional(readOnly = true)
    public Grafo montarGrafo() {
        Grafo grafo = new Grafo();
        for (Ponto ponto : pontoRepository.findAll(Sort.by("id"))) {
            grafo.adicionarPonto(ponto);
        }
        for (Via via : viaRepository.findAll(Sort.by("id"))) {
            grafo.adicionarVia(via);
        }
        for (Incidente incidente : incidenteRepository.findByAtivoTrue()) {
            grafo.bloquearVia(incidente.getVia().getId());
        }
        return grafo;
    }
}
