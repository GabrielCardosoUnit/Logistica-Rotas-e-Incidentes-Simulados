package com.logistica.roteirizacao.files;

import com.logistica.roteirizacao.client.ClienteApiHttp;
import com.logistica.roteirizacao.client.dto.Criterio;
import java.util.Objects;

/**
 * Preferências da sessão guardadas no config.bin: critério padrão, última origem e último
 * destino escolhidos e a URL da API. Guarda só parâmetros, nunca dados do domínio.
 *
 * <p>Imutável: para mudar um valor, crie outra configuração com os métodos {@code com...}.
 */
public final class Configuracao {

    private final Criterio criterioPadrao;
    private final String ultimaOrigemId;
    private final String ultimoDestinoId;
    private final String urlApi;

    /**
     * @param ultimaOrigemId  id do ponto ou "" quando nenhuma rota foi calculada ainda
     * @param ultimoDestinoId id do ponto ou "" quando nenhuma rota foi calculada ainda
     * @param urlApi          endereço http(s) da API
     */
    public Configuracao(Criterio criterioPadrao, String ultimaOrigemId, String ultimoDestinoId, String urlApi) {
        this.criterioPadrao = Objects.requireNonNull(criterioPadrao, "critério padrão é obrigatório.");
        this.ultimaOrigemId = Objects.requireNonNull(ultimaOrigemId, "última origem não pode ser nula.").trim();
        this.ultimoDestinoId = Objects.requireNonNull(ultimoDestinoId, "último destino não pode ser nulo.").trim();
        String url = Objects.requireNonNull(urlApi, "URL da API é obrigatória.").trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL da API deve começar com http:// ou https:// (valor: " + url + ").");
        }
        this.urlApi = url;
    }

    /** Valores usados na primeira execução, quando ainda não existe config.bin. */
    public static Configuracao padrao() {
        return new Configuracao(Criterio.MENOR_DISTANCIA, "", "", ClienteApiHttp.URL_PADRAO);
    }

    /** Guarda a rota que o usuário acabou de pedir (origem, destino e critério). */
    public Configuracao comUltimaRota(String origemId, String destinoId, Criterio criterio) {
        return new Configuracao(criterio, origemId, destinoId, urlApi);
    }

    public Configuracao comUrlApi(String novaUrl) {
        return new Configuracao(criterioPadrao, ultimaOrigemId, ultimoDestinoId, novaUrl);
    }

    public boolean temUltimaRota() {
        return !ultimaOrigemId.isEmpty() && !ultimoDestinoId.isEmpty();
    }

    public Criterio getCriterioPadrao() { return criterioPadrao; }
    public String getUltimaOrigemId() { return ultimaOrigemId; }
    public String getUltimoDestinoId() { return ultimoDestinoId; }
    public String getUrlApi() { return urlApi; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Configuracao)) {
            return false;
        }
        Configuracao c = (Configuracao) o;
        return criterioPadrao == c.criterioPadrao
                && ultimaOrigemId.equals(c.ultimaOrigemId)
                && ultimoDestinoId.equals(c.ultimoDestinoId)
                && urlApi.equals(c.urlApi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(criterioPadrao, ultimaOrigemId, ultimoDestinoId, urlApi);
    }

    @Override
    public String toString() {
        String rota = temUltimaRota() ? ultimaOrigemId + " → " + ultimoDestinoId : "nenhuma";
        return "Configuracao[critério=" + criterioPadrao.getDescricao() + ", última rota=" + rota
                + ", API=" + urlApi + "]";
    }
}
