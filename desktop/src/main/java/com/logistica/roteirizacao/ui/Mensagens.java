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

    public static final String ESCOLHER_PONTOS = "Escolha o arquivo pontos.csv.";
    public static final String ESCOLHER_VIAS = "Escolha o arquivo vias.csv.";
    public static final String ESCOLHER_ORIGEM = "Selecione um ponto de origem.";
    public static final String ESCOLHER_DESTINO = "Selecione um ponto de destino.";
    public static final String DESTINO_IGUAL_ORIGEM = "Selecione um ponto de destino diferente da origem.";
    public static final String IMPORTAR_ANTES = "Importe a rede (pontos.csv e vias.csv) antes de calcular.";
    public static final String ESCOLHER_VIA = "Selecione a via a bloquear.";
    public static final String DESCRICAO_INCIDENTE = "Descreva o incidente com 3 a 200 caracteres.";
    public static final String CALCULAR_ANTES_DE_EXPORTAR = "Calcule uma rota com caminho antes de exportar.";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Mensagens() {
    }

    /** A ClienteApiException já traz texto pronto para o usuário; para o resto, usamos um texto genérico. */
    public static String doErro(Exception e) {
        if (e instanceof ClienteApiException && e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "Erro inesperado: " + e.getClass().getSimpleName() + ". Tente novamente.";
    }

    public static String resumoImportacao(ImportacaoResultado r) {
        String base = "Rede importada: " + r.pontosImportados() + " pontos, "
                + r.viasImportadas() + " vias (versão " + r.versaoRede() + ").";
        int n = r.linhasRejeitadas().size();
        if (n == 0) {
            return base;
        }
        return base + " " + n + (n == 1 ? " linha rejeitada." : " linhas rejeitadas.");
    }

    /** 8.5 -> "8,5 km"; null -> "—". */
    public static String km(Double valor) {
        return valor == null ? "—" : String.format(PT_BR, "%.1f km", valor);
    }

    /** 14.5 -> "14,5 min"; null -> "—". */
    public static String min(Double valor) {
        return valor == null ? "—" : String.format(PT_BR, "%.1f min", valor);
    }

    public static String dataHora(OffsetDateTime data) {
        return data == null ? "—" : data.format(DATA_HORA);
    }
}
