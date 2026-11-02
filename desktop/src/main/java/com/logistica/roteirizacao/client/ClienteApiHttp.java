package com.logistica.roteirizacao.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ErroApi;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.IncidenteResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;

/** {@link ClienteApi} sobre HTTP, com {@link HttpClient} do Java e JSON via Jackson. */
public class ClienteApiHttp implements ClienteApi {

    public static final String URL_PADRAO = "http://localhost:8080";
    public static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(10);

    private static final String JSON = "application/json";
    private static final String CSV = "text/csv";

    private final URI urlBase;
    private final Duration tempoLimite;
    private final HttpClient http;
    private final ObjectMapper json;

    public ClienteApiHttp() {
        this(URL_PADRAO);
    }

    public ClienteApiHttp(String urlBase) {
        this(urlBase, TEMPO_LIMITE_PADRAO);
    }

    /**
     * @param urlBase     endereço da API, por exemplo {@value #URL_PADRAO}
     * @param tempoLimite tempo máximo de espera por requisição
     */
    public ClienteApiHttp(String urlBase, Duration tempoLimite) {
        this.urlBase = validarUrl(urlBase);
        this.tempoLimite = Objects.requireNonNull(tempoLimite, "tempoLimite é obrigatório.");
        this.http = HttpClient.newBuilder().connectTimeout(tempoLimite).build();
        this.json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // Mantém o fuso enviado pela API (-03:00) em vez de converter para UTC.
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                // Campos novos na API não devem quebrar um cliente antigo.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public URI getUrlBase() {
        return urlBase;
    }

    @Override
    public ImportacaoResultado importarRede(File pontos, File vias) throws ClienteApiException {
        exigirArquivoLegivel(pontos, "pontos");
        exigirArquivoLegivel(vias, "vias");

        CorpoMultipart corpo;
        try {
            corpo = CorpoMultipart.novo()
                    .adicionarArquivo("pontos", pontos, CSV)
                    .adicionarArquivo("vias", vias, CSV);
        } catch (IOException e) {
            throw new ClienteApiException("Não foi possível ler os arquivos da rede: " + e.getMessage(), e);
        }

        HttpRequest pedido = requisicao("/redes/importacoes")
                .header("Content-Type", corpo.tipoConteudo())
                .POST(corpo.publicador())
                .build();
        return enviar(pedido, ImportacaoResultado.class);
    }

    @Override
    public RotaResultado calcularRota(String origemId, String destinoId, Criterio criterio)
            throws ClienteApiException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("origemId", origemId);
        corpo.put("destinoId", destinoId);
        corpo.put("criterio", criterio);
        return enviar(postJson("/rotas", corpo), RotaResultado.class);
    }

    @Override
    public RotaResultado buscarRota(long id) throws ClienteApiException {
        HttpRequest pedido = requisicao("/rotas/" + id).GET().build();
        return enviar(pedido, RotaResultado.class);
    }

    @Override
    public IncidenteResultado registrarIncidente(String viaId, String descricao) throws ClienteApiException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("viaId", viaId);
        corpo.put("descricao", descricao);
        return enviar(postJson("/incidentes", corpo), IncidenteResultado.class);
    }

    private HttpRequest postJson(String caminho, Map<String, Object> corpo) throws ClienteApiException {
        try {
            return requisicao(caminho)
                    .header("Content-Type", JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                    .build();
        } catch (JsonProcessingException e) {
            throw new ClienteApiException("Não foi possível montar a requisição para a API.", e);
        }
    }

    /** {@code caminho} começa com "/"; resolve relativo à base para preservar um prefixo como /api/. */
    private HttpRequest.Builder requisicao(String caminho) {
        return HttpRequest.newBuilder(urlBase.resolve(caminho.substring(1)))
                .timeout(tempoLimite)
                .header("Accept", JSON);
    }

    /** Ponto único de envio: converte o sucesso no DTO e toda falha em {@link ClienteApiException}. */
    private <T> T enviar(HttpRequest pedido, Class<T> tipo) throws ClienteApiException {
        HttpResponse<String> resposta;
        try {
            resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException e) {
            throw new ClienteApiException("Não foi possível conectar à API em " + urlBase
                    + ". Verifique se ela está rodando.", e);
        } catch (HttpTimeoutException e) {
            throw new ClienteApiException("A API demorou mais de " + tempoLimite.toSeconds()
                    + " s para responder. Tente novamente.", e);
        } catch (IOException e) {
            throw new ClienteApiException("Falha de comunicação com a API em " + urlBase + ": "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClienteApiException("A operação foi interrompida.", e);
        }

        int status = resposta.statusCode();
        if (status >= 200 && status < 300) {
            try {
                return json.readValue(resposta.body(), tipo);
            } catch (JsonProcessingException e) {
                throw new ClienteApiException("A API respondeu num formato inesperado.", e);
            }
        }
        throw erroDe(status, resposta.body());
    }

    /** Usa o corpo padrão de erro quando existe; senão, uma mensagem pelo código HTTP. */
    private ClienteApiException erroDe(int status, String corpo) {
        try {
            ErroApi erro = json.readValue(corpo, ErroApi.class);
            if (erro.mensagem() != null && !erro.mensagem().isBlank()) {
                return new ClienteApiException(erro);
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // corpo não está no formato padrão: cai na mensagem genérica abaixo
        }
        return new ClienteApiException(mensagemPorStatus(status));
    }

    static String mensagemPorStatus(int status) {
        return switch (status) {
            case 400 -> "A API recusou os dados enviados (HTTP 400). Confira os campos.";
            case 404 -> "Recurso não encontrado na API (HTTP 404).";
            case 409 -> "A operação conflita com o estado atual da rede (HTTP 409).";
            case 503 -> "A API está indisponível no momento (HTTP 503). Tente novamente.";
            default -> "A API respondeu com erro inesperado (HTTP " + status + ").";
        };
    }

    private static void exigirArquivoLegivel(File arquivo, String nome) throws ClienteApiException {
        if (arquivo == null) {
            throw new ClienteApiException("Escolha o arquivo de " + nome + " (CSV).");
        }
        if (!arquivo.isFile() || !arquivo.canRead()) {
            throw new ClienteApiException("Arquivo de " + nome + " não encontrado ou sem permissão de leitura: "
                    + arquivo.getAbsolutePath());
        }
    }

    private static URI validarUrl(String urlBase) {
        Objects.requireNonNull(urlBase, "urlBase é obrigatória.");
        String limpa = urlBase.strip();
        while (limpa.endsWith("/")) {
            limpa = limpa.substring(0, limpa.length() - 1);
        }
        URI uri = URI.create(limpa + "/");
        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("A URL da API deve começar com http:// ou https://: " + urlBase);
        }
        return uri;
    }

    @Override
    public String toString() {
        return "ClienteApiHttp[" + urlBase + "]";
    }
}
