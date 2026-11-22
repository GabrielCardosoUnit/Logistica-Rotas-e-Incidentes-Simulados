package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApiException;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos e formatações da tela em um só lugar (métodos estáticos).
 * Regra da equipe: a mensagem diz qual campo está errado e como corrigir.
 */
public final class Mensagens {

    /** Falta escolher pontos.csv. */
    public static final String ESCOLHER_PONTOS = "Escolha o arquivo pontos.csv.";
    /** Falta escolher vias.csv. */
    public static final String ESCOLHER_VIAS = "Escolha o arquivo vias.csv.";
    /** Falta escolher a origem. */
    public static final String ESCOLHER_ORIGEM = "Selecione um ponto de origem.";
    /** Falta escolher o destino. */
    public static final String ESCOLHER_DESTINO = "Selecione um ponto de destino.";
    /** Origem e destino iguais. */
    public static final String DESTINO_IGUAL_ORIGEM = "Selecione um ponto de destino diferente da origem.";
    /** Tentativa de calcular sem rede importada. */
    public static final String IMPORTAR_ANTES = "Importe a rede (pontos.csv e vias.csv) antes de calcular.";
    /** Falta escolher a via a bloquear. */
    public static final String ESCOLHER_VIA = "Selecione a via a bloquear.";
    /** Falta escolher a via a desbloquear. */
    public static final String ESCOLHER_VIA_BLOQUEADA = "Selecione a via a desbloquear.";
    /** Descrição do incidente fora do tamanho aceito. */
    public static final String DESCRICAO_INCIDENTE = "Descreva o incidente com 3 a 200 caracteres.";
    /** Tentativa de exportar sem rota com caminho. */
    public static final String CALCULAR_ANTES_DE_EXPORTAR = "Calcule uma rota com caminho antes de exportar.";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Mensagens() {
    }

    /**
     * A ClienteApiException já traz texto pronto para o usuário; para o resto, usamos um texto genérico.
     *
     * @param e falha ocorrida
     * @return mensagem para mostrar na tela
     */
    public static String doErro(Exception e) {
        if (e instanceof ClienteApiException && e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "Erro inesperado: " + e.getClass().getSimpleName() + ". Tente novamente.";
    }

    /**
     * Resumo da importação para a barra de mensagens.
     *
     * @param r resultado da importação
     * @return texto com pontos, vias, versão e linhas rejeitadas
     */
    public static String resumoImportacao(ImportacaoResultado r) {
        String base = "Rede importada: " + r.pontosImportados() + " pontos, "
                + r.viasImportadas() + " vias (versão " + r.versaoRede() + ").";
        int n = r.linhasRejeitadas().size();
        if (n == 0) {
            return base;
        }
        return base + " " + n + (n == 1 ? " linha rejeitada." : " linhas rejeitadas.");
    }

    /**
     * Formata distância. 8.5 -> "8,5 km"; abaixo de 1 km mostra metros
     * (0.33 -> "330 m"), para as rotas do campus; null -> "—".
     *
     * @param valor distância em km, ou {@code null}
     * @return texto com a unidade
     */
    public static String km(Double valor) {
        if (valor == null) {
            return "—";
        }
        if (valor < 1.0) {
            return String.format(PT_BR, "%.0f m", valor * 1000);
        }
        return String.format(PT_BR, "%.1f km", valor);
    }

    /**
     * Formata tempo. 14.5 -> "14,5 min"; null -> "—".
     *
     * @param valor tempo em minutos, ou {@code null}
     * @return texto com a unidade
     */
    public static String min(Double valor) {
        return valor == null ? "—" : String.format(PT_BR, "%.1f min", valor);
    }

    /**
     * Formata data e hora para a tela; null -> "—".
     *
     * @param data instante com fuso, ou {@code null}
     * @return texto no formato dd/MM/yyyy HH:mm:ss
     */
    public static String dataHora(OffsetDateTime data) {
        return data == null ? "—" : data.format(DATA_HORA);
    }
}
