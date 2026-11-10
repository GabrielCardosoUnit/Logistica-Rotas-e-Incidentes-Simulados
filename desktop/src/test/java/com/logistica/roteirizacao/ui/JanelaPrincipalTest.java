package com.logistica.roteirizacao.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.logistica.roteirizacao.client.ClienteApi;
import com.logistica.roteirizacao.client.ClienteApiException;
import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.IncidenteResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import com.logistica.roteirizacao.files.Configuracao;
import com.logistica.roteirizacao.files.ConfiguracaoBinaria;
import com.logistica.roteirizacao.files.ExportadorRotaCsv;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fluxo da tela com um cliente falso (sem API): importar, calcular, exportar CSV, bloquear via
 * e recalcular, e a recuperação das preferências do config.bin. Usa a rede de dados/tratados,
 * que a tela pré-carrega.
 */
class JanelaPrincipalTest {

    @TempDir
    Path pasta;

    private ClienteFalso cliente;
    private JanelaPrincipal janela;

    @BeforeEach
    void exigirAmbienteGrafico() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "teste de tela precisa de ambiente gráfico");
        cliente = new ClienteFalso();
    }

    @AfterEach
    void fecharJanela() throws Exception {
        if (janela != null) {
            SwingUtilities.invokeAndWait(janela::dispose);
        }
    }

    @Test
    void bloquearViaDaRotaRecalculaSemEla() throws Exception {
        File config = pasta.resolve("config.bin").toFile();
        abrirEImportar(Configuracao.padrao(), config);

        naEdt(() -> {
            janela.painelRota().selecionar("P05", "P01");
            janela.painelRota().getBotaoCalcular().doClick();
        });
        aguardar(() -> janela.ultimaRota() != null);
        assertEquals(List.of("P05", "P15", "P01"), lerNaEdt(() -> janela.ultimaRota().pontos()));

        naEdt(() -> {
            assertTrue(janela.painelIncidente().selecionarVia("V24"));
            janela.painelIncidente().getBotaoBloquear().doClick();
        });
        aguardar(() -> cliente.calculos.size() == 2 && janela.ultimaRota().pontos().size() == 4);

        assertEquals(List.of("V24"), cliente.incidentes);
        assertEquals("P05→P01:MENOR_DISTANCIA", cliente.calculos.get(1), "recalcula a mesma rota");
        assertEquals(List.of("P05", "P15", "P03", "P01"), lerNaEdt(() -> janela.ultimaRota().pontos()));
        String barra = lerNaEdt(janela::textoDaBarra);
        assertTrue(barra.contains("Via V24 bloqueada") && barra.contains("Rota recalculada: 11,7 km"), barra);
        assertTrue(lerNaEdt(() -> janela.painelMapa().estaBloqueada(new ViaRede("V24", "P15", "P01", true))));
        assertFalse(lerNaEdt(() -> janela.painelIncidente().selecionarVia("V24")),
                "via bloqueada sai da lista para não gerar 409");
    }

    @Test
    void exportaARotaCalculadaEmCsv() throws Exception {
        abrirEImportar(Configuracao.padrao(), null);
        naEdt(() -> {
            janela.painelRota().selecionar("P05", "P01");
            janela.painelRota().getBotaoCalcular().doClick();
        });
        aguardar(() -> janela.ultimaRota() != null);
        assertTrue(lerNaEdt(() -> janela.painelResultado().getBotaoExportar().isEnabled()));

        File csv = pasta.resolve("rota.csv").toFile();
        naEdt(() -> janela.exportarPara(csv));

        assertEquals(lerNaEdt(janela::ultimaRota), ExportadorRotaCsv.ler(csv));
        assertTrue(lerNaEdt(janela::textoDaBarra).startsWith("OK: Rota exportada"));
    }

    @Test
    void calcularGravaAsPreferenciasNoConfigBin() throws Exception {
        File config = pasta.resolve("config.bin").toFile();
        abrirEImportar(Configuracao.padrao(), config);

        naEdt(() -> {
            janela.painelRota().setCriterio(Criterio.MENOR_TEMPO);
            janela.painelRota().selecionar("P12", "P07");
            janela.painelRota().getBotaoCalcular().doClick();
        });
        aguardar(() -> janela.ultimaRota() != null);

        Configuracao salva = ConfiguracaoBinaria.ler(config);
        assertEquals(Criterio.MENOR_TEMPO, salva.getCriterioPadrao());
        assertEquals("P12", salva.getUltimaOrigemId());
        assertEquals("P07", salva.getUltimoDestinoId());
    }

    @Test
    void preferenciasRecuperadasPreenchemCriterioOrigemEDestino() throws Exception {
        abrirEImportar(new Configuracao(Criterio.MENOR_TEMPO, "P05", "P01", "http://localhost:8080"), null);

        assertEquals("P05", lerNaEdt(() -> janela.painelRota().getOrigem().getId()));
        assertEquals("P01", lerNaEdt(() -> janela.painelRota().getDestino().getId()));
        assertEquals(Criterio.MENOR_TEMPO, lerNaEdt(() -> janela.painelRota().getCriterio()));
        assertTrue(lerNaEdt(janela::textoDaBarra).contains("Última rota recuperada"));
    }

    @Test
    void bloquearSemEscolherViaMostraErroSemChamarAApi() throws Exception {
        abrirEImportar(Configuracao.padrao(), null);

        naEdt(() -> janela.painelIncidente().getBotaoBloquear().doClick());

        assertTrue(cliente.incidentes.isEmpty());
        assertTrue(lerNaEdt(janela::textoDaBarra).startsWith("Erro:"));
    }

    // ---- apoio ----------------------------------------------------------------------------------

    private void abrirEImportar(Configuracao configuracao, File arquivoConfig) throws Exception {
        naEdt(() -> {
            janela = new JanelaPrincipal(cliente, configuracao, arquivoConfig);
            janela.painelImportacao().getBotaoImportar().doClick();
        });
        aguardar(() -> janela.textoDaBarra().startsWith("OK: Rede importada"));
    }

    private static void naEdt(Runnable acao) throws Exception {
        SwingUtilities.invokeAndWait(acao);
    }

    private static <T> T lerNaEdt(Supplier<T> leitura) throws Exception {
        AtomicReference<T> valor = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> valor.set(leitura.get()));
        return valor.get();
    }

    /** Espera a tarefa em segundo plano terminar e a tela ser atualizada na EDT. */
    private static void aguardar(Supplier<Boolean> condicao) throws Exception {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            if (Boolean.TRUE.equals(lerNaEdt(condicao))) {
                return;
            }
            Thread.sleep(20);
        }
        fail("a tela não chegou ao estado esperado em 5 s");
    }

    /** Responde como a API faria para a rede de Aracaju, sem rede nem servidor. */
    private static final class ClienteFalso implements ClienteApi {
        // escritas no SwingWorker e lidas pelo teste: listas seguras entre threads
        private final List<String> calculos = new CopyOnWriteArrayList<>();
        private final List<String> incidentes = new CopyOnWriteArrayList<>();

        @Override
        public ImportacaoResultado importarRede(File pontos, File vias) {
            return new ImportacaoResultado(1, 20, 30, List.of());
        }

        @Override
        public RotaResultado calcularRota(String origemId, String destinoId, Criterio criterio) {
            calculos.add(origemId + "→" + destinoId + ":" + criterio.name());
            long id = calculos.size();
            OffsetDateTime agora = OffsetDateTime.parse("2026-11-10T10:15:00-03:00");
            if (incidentes.contains("V24")) {
                return new RotaResultado(id, origemId, destinoId, criterio, "CALCULADA",
                        List.of("P05", "P15", "P03", "P01"), List.of("V14", "V23", "V25"),
                        11.7, 23.2, 2, null, agora);
            }
            return new RotaResultado(id, origemId, destinoId, criterio, "CALCULADA",
                    List.of(origemId, "P15", destinoId), List.of("V14", "V24"), 8.5, 14.5, 1, null, agora);
        }

        @Override
        public RotaResultado buscarRota(long id) throws ClienteApiException {
            throw new ClienteApiException("não usado neste teste");
        }

        @Override
        public IncidenteResultado registrarIncidente(String viaId, String descricao) {
            incidentes.add(viaId);
            return new IncidenteResultado(incidentes.size(), viaId, descricao, true,
                    OffsetDateTime.parse("2026-11-10T10:16:00-03:00"), 2);
        }
    }
}
