package com.logistica.roteirizacao.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.IncidenteResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Testa o cliente contra um servidor HTTP falso (do próprio JDK), sem depender
 * da API real. As respostas imitam o openapi.yaml.
 */
class ClienteApiHttpTest {

    private HttpServer servidor;
    private ClienteApiHttp cliente;

    /** Último pedido recebido pelo servidor falso. */
    private final AtomicReference<String> metodoRecebido = new AtomicReference<>();
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> tipoConteudoRecebido = new AtomicReference<>();

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
        cliente = new ClienteApiHttp("http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofSeconds(2));
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    /** Faz o servidor falso responder {@code caminho} com o status e o JSON dados. */
    private void responder(String caminho, int status, String json) {
        servidor.createContext(caminho, troca -> {
            guardarPedido(troca);
            byte[] corpo = json.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
    }

    private void guardarPedido(HttpExchange troca) throws IOException {
        metodoRecebido.set(troca.getRequestMethod());
        tipoConteudoRecebido.set(troca.getRequestHeaders().getFirst("Content-Type"));
        corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void calcularRotaEnviaOPedidoEConverteARespostaDoContrato() throws Exception {
        responder("/rotas", 201, """
                {"id":1,"origemId":"P05","destinoId":"P01","criterio":"MENOR_DISTANCIA",
                 "status":"CALCULADA","pontos":["P05","P15","P01"],"vias":["V14","V24"],
                 "distanciaTotalKm":8.5,"tempoTotalMin":14.5,"versaoRede":1,"mensagem":null,
                 "calculadaEm":"2026-10-26T10:15:00-03:00"}
                """);

        RotaResultado rota = cliente.calcularRota("P05", "P01", Criterio.MENOR_DISTANCIA);

        assertEquals("POST", metodoRecebido.get());
        assertTrue(tipoConteudoRecebido.get().startsWith("application/json"));
        assertEquals("{\"origemId\":\"P05\",\"destinoId\":\"P01\",\"criterio\":\"MENOR_DISTANCIA\"}", corpoRecebido.get());
        assertEquals(List.of("P05", "P15", "P01"), rota.pontos());
        assertEquals(8.5, rota.distanciaTotalKm());
        assertEquals(14.5, rota.tempoTotalMin());
        assertEquals(Criterio.MENOR_DISTANCIA, rota.criterio());
        assertFalse(rota.semCaminho());
    }

    @Test
    void criterioPadraoEhMenorDistancia() throws Exception {
        responder("/rotas", 201, """
                {"id":2,"origemId":"P05","destinoId":"P01","criterio":"MENOR_DISTANCIA","status":"CALCULADA",
                 "pontos":["P05","P01"],"vias":["V1"],"distanciaTotalKm":1.0,"tempoTotalMin":2.0,"versaoRede":1,
                 "calculadaEm":"2026-10-26T10:15:00-03:00"}
                """);

        cliente.calcularRota("P05", "P01");

        assertTrue(corpoRecebido.get().contains("\"criterio\":\"MENOR_DISTANCIA\""));
    }

    @Test
    void semCaminhoNaoEhErroEChegaComTotaisNulos() throws Exception {
        responder("/rotas", 201, """
                {"id":3,"origemId":"P05","destinoId":"P07","criterio":"MENOR_DISTANCIA","status":"SEM_CAMINHO",
                 "pontos":[],"vias":[],"distanciaTotalKm":null,"tempoTotalMin":null,"versaoRede":4,
                 "mensagem":"Não há caminho disponível de P05 até P07. Vias bloqueadas: V29, V30.",
                 "calculadaEm":"2026-10-26T10:20:00-03:00"}
                """);

        RotaResultado rota = cliente.calcularRota("P05", "P07", Criterio.MENOR_DISTANCIA);

        assertTrue(rota.semCaminho());
        assertNull(rota.distanciaTotalKm());
        assertTrue(rota.pontos().isEmpty());
        assertTrue(rota.mensagem().contains("V29, V30"));
    }

    @Test
    void buscarRotaUsaGetNoCaminhoDoId() throws Exception {
        responder("/rotas/42", 200, """
                {"id":42,"origemId":"P05","destinoId":"P01","criterio":"MENOR_TEMPO","status":"CALCULADA",
                 "pontos":["P05","P01"],"vias":["V1"],"distanciaTotalKm":1.0,"tempoTotalMin":2.0,"versaoRede":1,
                 "calculadaEm":"2026-10-26T10:15:00-03:00"}
                """);

        RotaResultado rota = cliente.buscarRota(42);

        assertEquals("GET", metodoRecebido.get());
        assertEquals(42, rota.id());
        assertEquals(Criterio.MENOR_TEMPO, rota.criterio());
    }

    @Test
    void erroNoFormatoPadraoViraExcecaoComAMensagemEOCampoDaApi() {
        responder("/rotas", 404, """
                {"status":404,"erro":"NAO_ENCONTRADO","campo":"origemId",
                 "mensagem":"Ponto P99 não existe na rede. Importe a rede ou escolha outro ponto."}
                """);

        ClienteApiException erro = assertThrows(ClienteApiException.class,
                () -> cliente.calcularRota("P99", "P01", Criterio.MENOR_DISTANCIA));

        assertEquals("Ponto P99 não existe na rede. Importe a rede ou escolha outro ponto.", erro.getMessage());
        assertEquals("origemId", erro.getCampo().orElseThrow());
        assertEquals(404, erro.getStatusHttp().orElseThrow());
    }

    @Test
    void erroForaDoFormatoPadraoGanhaMensagemPeloStatus() {
        servidor.createContext("/rotas", troca -> {
            guardarPedido(troca);
            troca.sendResponseHeaders(503, -1);
            troca.close();
        });

        ClienteApiException erro = assertThrows(ClienteApiException.class,
                () -> cliente.calcularRota("P05", "P01", Criterio.MENOR_DISTANCIA));

        assertEquals(ClienteApiHttp.mensagemPorStatus(503), erro.getMessage());
        assertTrue(erro.getErroApi().isEmpty());
    }

    @Test
    void apiDesligadaGeraMensagemDeConexao() throws Exception {
        int portaLivre;
        try (ServerSocket socket = new ServerSocket(0)) {
            portaLivre = socket.getLocalPort();
        }
        ClienteApi semApi = new ClienteApiHttp("http://127.0.0.1:" + portaLivre, Duration.ofSeconds(2));

        ClienteApiException erro = assertThrows(ClienteApiException.class,
                () -> semApi.calcularRota("P05", "P01", Criterio.MENOR_DISTANCIA));

        assertTrue(erro.getMessage().startsWith("Não foi possível conectar à API"), erro.getMessage());
    }

    @Test
    void apiLentaGeraMensagemDeTempoEsgotado() {
        servidor.createContext("/rotas", troca -> {
            try {
                Thread.sleep(1_500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            troca.close();
        });
        ClienteApi apressado = new ClienteApiHttp("http://127.0.0.1:" + servidor.getAddress().getPort(),
                Duration.ofMillis(300));

        ClienteApiException erro = assertThrows(ClienteApiException.class,
                () -> apressado.calcularRota("P05", "P01", Criterio.MENOR_DISTANCIA));

        assertTrue(erro.getMessage().startsWith("A API demorou"), erro.getMessage());
    }

    @Test
    void importarRedeEnviaOsDoisCsvComoMultipart(@TempDir Path pasta) throws Exception {
        File pontos = Files.writeString(pasta.resolve("pontos.csv"),
                "id,nome,latitude,longitude\nP01,UNIT,-10.968,-37.061\n").toFile();
        File vias = Files.writeString(pasta.resolve("vias.csv"),
                "id,origem,destino,distanciaKm,tempoMin,habilitada\nV01,P01,P02,1.9,5.7,true\n").toFile();
        responder("/redes/importacoes", 201, """
                {"versaoRede":1,"pontosImportados":20,"viasImportadas":28,
                 "linhasRejeitadas":[{"arquivo":"vias.csv","linha":5,"motivo":"distanciaKm deve ser maior que zero."}]}
                """);

        ImportacaoResultado resultado = cliente.importarRede(pontos, vias);

        assertTrue(tipoConteudoRecebido.get().startsWith("multipart/form-data; boundary="));
        String corpo = corpoRecebido.get();
        assertTrue(corpo.contains("name=\"pontos\"; filename=\"pontos.csv\""));
        assertTrue(corpo.contains("name=\"vias\"; filename=\"vias.csv\""));
        assertTrue(corpo.contains("P01,UNIT,-10.968,-37.061"));
        assertTrue(corpo.contains("V01,P01,P02,1.9,5.7,true"));
        assertEquals(28, resultado.viasImportadas());
        assertTrue(resultado.teveRejeicoes());
        assertEquals("vias.csv, linha 5: distanciaKm deve ser maior que zero.",
                resultado.linhasRejeitadas().get(0).toString());
    }

    @Test
    void arquivoInexistenteEhRecusadoAntesDeChamarAApi(@TempDir Path pasta) {
        File inexistente = pasta.resolve("nao-existe.csv").toFile();

        ClienteApiException erro = assertThrows(ClienteApiException.class,
                () -> cliente.importarRede(inexistente, inexistente));

        assertTrue(erro.getMessage().startsWith("Arquivo de pontos não encontrado"), erro.getMessage());
        assertNull(metodoRecebido.get(), "a API não deveria ter sido chamada");
    }

    @Test
    void registrarIncidenteEnviaViaEDescricao() throws Exception {
        responder("/incidentes", 201, """
                {"id":1,"viaId":"V24","descricao":"Alagamento simulado","ativo":true,
                 "registradoEm":"2026-10-26T10:16:00-03:00","versaoRede":2}
                """);

        IncidenteResultado incidente = cliente.registrarIncidente("V24", "Alagamento simulado");

        assertEquals("{\"viaId\":\"V24\",\"descricao\":\"Alagamento simulado\"}", corpoRecebido.get());
        assertEquals("V24", incidente.viaId());
        assertTrue(incidente.ativo());
        assertEquals("2026-10-26T10:16-03:00", incidente.registradoEm().toString(), "o fuso da API deve ser mantido");
        assertEquals(2, incidente.versaoRede());
    }

    @Test
    void urlInvalidaEhRecusadaNaCriacao() {
        assertThrows(IllegalArgumentException.class, () -> new ClienteApiHttp("localhost:8080"));
    }

    @Test
    void urlComBarraFinalOuPrefixoPreservaOCaminho() {
        assertEquals("http://localhost:8080/", new ClienteApiHttp("http://localhost:8080//").getUrlBase().toString());
        assertEquals("http://host/api/", new ClienteApiHttp("http://host/api").getUrlBase().toString());
    }
}
