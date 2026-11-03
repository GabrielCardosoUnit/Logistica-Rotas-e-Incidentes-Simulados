package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.dto.ImportacaoResultado.LinhaRejeitada;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Janela que lista as linhas de CSV rejeitadas na importação (arquivo, linha, motivo). */
final class DialogoLinhasRejeitadas {

    private DialogoLinhasRejeitadas() {
    }

    static void mostrar(Component pai, List<LinhaRejeitada> linhas) {
        DefaultTableModel modelo = new DefaultTableModel(new Object[] {"Arquivo", "Linha", "Motivo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (LinhaRejeitada l : linhas) {
            modelo.addRow(new Object[] {l.arquivo(), l.linha(), l.motivo()});
        }
        JTable tabela = new JTable(modelo);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(520);

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(pai), "Linhas rejeitadas na importação",
                JDialog.ModalityType.APPLICATION_MODAL);
        JLabel aviso = new JLabel("<html>" + linhas.size() + (linhas.size() == 1 ? " linha foi rejeitada" : " linhas foram rejeitadas")
                + "; as demais foram importadas.<br>Corrija o CSV e importe novamente.</html>");
        aviso.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        JButton ok = new JButton("Fechar");
        ok.addActionListener(e -> d.dispose());
        JPanel rodape = new JPanel();
        rodape.add(ok);

        d.add(aviso, BorderLayout.NORTH);
        d.add(new JScrollPane(tabela), BorderLayout.CENTER);
        d.add(rodape, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(ok); // Enter fecha, Esc também
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                javax.swing.KeyStroke.getKeyStroke("ESCAPE"), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setPreferredSize(new Dimension(700, 300));
        d.pack();
        d.setLocationRelativeTo(pai);
        d.setVisible(true);
    }
}
