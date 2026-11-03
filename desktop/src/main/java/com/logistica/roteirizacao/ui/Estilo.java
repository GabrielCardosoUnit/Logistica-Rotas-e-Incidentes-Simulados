package com.logistica.roteirizacao.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

/** Cores e rótulos comuns. Erro sempre leva o prefixo "Erro:" além da cor (não depende só de cor). */
final class Estilo {

    static final Color VERMELHO = new Color(0xB0, 0x00, 0x20);
    static final Color VERDE = new Color(0x1B, 0x5E, 0x20);
    static final Color LARANJA = new Color(0x8A, 0x4B, 0x00);

    private Estilo() {
    }

    /** Rótulo de erro reservado sob um campo. Usa " " para manter a altura e o layout não "pular". */
    static JLabel rotuloErro() {
        JLabel l = new JLabel(" ");
        l.setForeground(VERMELHO);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    static void mostrarErro(JLabel rotulo, String mensagem) {
        rotulo.setText(mensagem == null || mensagem.isEmpty() ? " " : "Erro: " + mensagem);
    }

    static void limpar(JLabel rotulo) {
        rotulo.setText(" ");
    }
}
