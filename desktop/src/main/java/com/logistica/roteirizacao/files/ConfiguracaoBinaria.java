package com.logistica.roteirizacao.files;

import com.logistica.roteirizacao.client.dto.Criterio;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.Objects;

/**
 * Lê e grava o config.bin com {@link DataOutputStream} e {@link DataInputStream}.
 *
 * <p>Formato (versão 1), na ordem:
 * <ol>
 *   <li>{@code int} versão do formato ({@value #VERSAO})</li>
 *   <li>{@code int} critério padrão: 0 = menor distância, 1 = menor tempo</li>
 *   <li>{@code UTF} última origem ("" se nenhuma)</li>
 *   <li>{@code UTF} último destino ("" se nenhum)</li>
 *   <li>{@code UTF} URL da API</li>
 * </ol>
 * Arquivo ausente, truncado, com versão não suportada, valor inválido ou bytes sobrando
 * gera {@link ConfiguracaoInvalidaException} com uma mensagem para o usuário.
 */
public final class ConfiguracaoBinaria {

    public static final int VERSAO = 1;
    public static final String NOME_ARQUIVO = "config.bin";

    private static final int CODIGO_DISTANCIA = 0;
    private static final int CODIGO_TEMPO = 1;

    private ConfiguracaoBinaria() {
    }

    public static void salvar(Configuracao config, File arquivo) throws IOException {
        Objects.requireNonNull(config, "configuração é obrigatória.");
        Objects.requireNonNull(arquivo, "arquivo é obrigatório.");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(arquivo)))) {
            out.writeInt(VERSAO);
            out.writeInt(codigoDe(config.getCriterioPadrao()));
            out.writeUTF(config.getUltimaOrigemId());
            out.writeUTF(config.getUltimoDestinoId());
            out.writeUTF(config.getUrlApi());
        }
    }

    public static Configuracao ler(File arquivo) throws IOException {
        Objects.requireNonNull(arquivo, "arquivo é obrigatório.");
        if (!arquivo.isFile()) {
            throw new ConfiguracaoInvalidaException("Arquivo " + arquivo.getName() + " não encontrado.");
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(arquivo)))) {
            int versao = in.readInt();
            if (versao != VERSAO) {
                throw new ConfiguracaoInvalidaException("Versão " + versao + " do " + arquivo.getName()
                        + " não é suportada (esperada: " + VERSAO + ").");
            }
            Criterio criterio = criterioDe(in.readInt());
            String origem = in.readUTF();
            String destino = in.readUTF();
            String url = in.readUTF();
            if (in.read() != -1) {
                throw new ConfiguracaoInvalidaException(arquivo.getName()
                        + " tem dados além do esperado para a versão " + VERSAO + ".");
            }
            return new Configuracao(criterio, origem, destino, url);
        } catch (EOFException e) {
            throw new ConfiguracaoInvalidaException(arquivo.getName() + " está incompleto (truncado).", e);
        } catch (UTFDataFormatException e) {
            throw new ConfiguracaoInvalidaException(arquivo.getName() + " tem texto corrompido.", e);
        } catch (FileNotFoundException e) {
            throw new ConfiguracaoInvalidaException("Não foi possível abrir " + arquivo.getName() + ".", e);
        } catch (IllegalArgumentException e) {
            // valores lidos que a Configuracao recusa (por exemplo, URL sem http://)
            throw new ConfiguracaoInvalidaException(arquivo.getName() + " tem um valor inválido: "
                    + e.getMessage(), e);
        }
    }

    static int codigoDe(Criterio criterio) {
        return criterio == Criterio.MENOR_TEMPO ? CODIGO_TEMPO : CODIGO_DISTANCIA;
    }

    static Criterio criterioDe(int codigo) throws ConfiguracaoInvalidaException {
        switch (codigo) {
            case CODIGO_DISTANCIA:
                return Criterio.MENOR_DISTANCIA;
            case CODIGO_TEMPO:
                return Criterio.MENOR_TEMPO;
            default:
                throw new ConfiguracaoInvalidaException("Critério " + codigo
                        + " inválido no config.bin (use 0 = distância ou 1 = tempo).");
        }
    }
}
