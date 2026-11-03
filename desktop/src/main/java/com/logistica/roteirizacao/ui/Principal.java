package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApi;
import com.logistica.roteirizacao.client.ClienteApiHttp;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ponto de entrada do cliente desktop.
 * Argumento opcional: URL da API (padrão {@value ClienteApiHttp#URL_PADRAO}).
 */
public final class Principal {

    private Principal() {
    }

    public static void main(String[] args) {
        final String url = args.length > 0 ? args[0] : ClienteApiHttp.URL_PADRAO;
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // segue com o visual padrão do Swing
            }
            ClienteApi cliente = new ClienteApiHttp(url);
            new JanelaPrincipal(cliente).setVisible(true);
        });
    }
}
