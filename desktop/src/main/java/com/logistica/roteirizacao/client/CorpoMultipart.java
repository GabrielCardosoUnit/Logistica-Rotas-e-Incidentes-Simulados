package com.logistica.roteirizacao.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Monta um corpo multipart/form-data, que o HttpClient não gera sozinho.
 *
 * <p>Formato (RFC 7578): cada parte começa com "--fronteira", traz seus
 * cabeçalhos, uma linha em branco e os bytes; o corpo termina com "--fronteira--".
 */
public final class CorpoMultipart {

    private static final String QUEBRA = "\r\n";

    private final String fronteira;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    private CorpoMultipart(String fronteira) {
        this.fronteira = fronteira;
    }

    /**
     * Corpo vazio com uma fronteira aleatória, que não aparece no conteúdo dos CSVs.
     *
     * @return novo corpo, sem partes
     */
    public static CorpoMultipart novo() {
        return new CorpoMultipart("----RoteirizacaoFronteira" + UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * Adiciona um arquivo como a parte {@code nomeCampo} (ex.: "pontos").
     *
     * @param nomeCampo    nome do campo no formulário
     * @param arquivo      arquivo a enviar
     * @param tipoConteudo Content-Type da parte, ex.: {@code text/csv}
     * @return este corpo, para encadear chamadas
     * @throws IOException se o arquivo não puder ser lido
     */
    public CorpoMultipart adicionarArquivo(String nomeCampo, File arquivo, String tipoConteudo) throws IOException {
        escrever("--" + fronteira + QUEBRA);
        escrever("Content-Disposition: form-data; name=\"" + nomeCampo + "\"; filename=\""
                + arquivo.getName().replace("\"", "") + "\"" + QUEBRA);
        escrever("Content-Type: " + tipoConteudo + QUEBRA);
        escrever(QUEBRA);
        bytes.write(Files.readAllBytes(arquivo.toPath()));
        escrever(QUEBRA);
        return this;
    }

    /**
     * Valor do cabeçalho Content-Type da requisição.
     *
     * @return {@code multipart/form-data} com a fronteira
     */
    public String tipoConteudo() {
        return "multipart/form-data; boundary=" + fronteira;
    }

    /**
     * Fecha o corpo e devolve o publisher para a requisição.
     *
     * @return o corpo pronto para o {@link java.net.http.HttpRequest}
     */
    public HttpRequest.BodyPublisher publicador() {
        ByteArrayOutputStream completo = new ByteArrayOutputStream();
        completo.writeBytes(bytes.toByteArray());
        completo.writeBytes(("--" + fronteira + "--" + QUEBRA).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArray(completo.toByteArray());
    }

    private void escrever(String texto) {
        bytes.writeBytes(texto.getBytes(StandardCharsets.UTF_8));
    }
}
