package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApi;
import com.logistica.roteirizacao.client.ClienteApiHttp;
import com.logistica.roteirizacao.files.Configuracao;
import com.logistica.roteirizacao.files.ConfiguracaoBinaria;
import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ponto de entrada do cliente desktop.
 *
 * <p>Lê as preferências do config.bin (na pasta em que o programa foi iniciado). Argumento
 * opcional: URL da API; sem ele, vale a URL guardada no config.bin ou
 * {@value ClienteApiHttp#URL_PADRAO}.
 */
public final class Principal {

    private Principal() {
    }

    public static void main(String[] args) {
        File arquivo = new File(ConfiguracaoBinaria.NOME_ARQUIVO);
        Configuracao configuracao;
        String mensagem;
        boolean ehAviso = false;
        if (!arquivo.isFile()) {
            configuracao = Configuracao.padrao();
            mensagem = "Primeira execução: as preferências serão salvas em " + arquivo.getName()
                    + " ao calcular uma rota.";
        } else {
            try {
                configuracao = ConfiguracaoBinaria.ler(arquivo);
                mensagem = "Preferências recuperadas de " + arquivo.getName() + ": "
                        + configuracao.getCriterioPadrao().getDescricao()
                        + (configuracao.temUltimaRota() ? ", última rota " + configuracao.getUltimaOrigemId()
                                + " → " + configuracao.getUltimoDestinoId() : "") + ".";
            } catch (IOException e) {
                configuracao = Configuracao.padrao();
                mensagem = e.getMessage() + " Usando os valores padrão; o arquivo será regravado.";
                ehAviso = true;
            }
        }
        if (args.length > 0) {
            configuracao = configuracao.comUrlApi(args[0]);
        }

        final Configuracao config = configuracao;
        final String texto = mensagem;
        final boolean aviso = ehAviso;
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // segue com o visual padrão do Swing
            }
            ClienteApi cliente = new ClienteApiHttp(config.getUrlApi());
            JanelaPrincipal janela = new JanelaPrincipal(cliente, config, arquivo);
            janela.mostrarMensagemInicial(texto, aviso);
            janela.setVisible(true);
        });
    }
}
