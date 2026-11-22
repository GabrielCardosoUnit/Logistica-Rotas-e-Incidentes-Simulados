package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.dto.RotaResultado;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Seção 3: mostra o resultado do cálculo (rota passo a passo, totais com unidade, status e
 * mensagem de "sem caminho"). O status aparece em TEXTO, e não só em cor.
 *
 * @serial exclude
 */
public class PainelResultado extends JPanel {

    private final JLabel lblStatus = new JLabel();
    private final JLabel lblTotais = new JLabel(" ");
    private final JLabel lblDetalhe = new JLabel(" ");
    private final JLabel lblVias = new JLabel(" ");
    private final JTextArea txtMensagem = new JTextArea(2, 30);
    private final DefaultListModel<String> passos = new DefaultListModel<>();
    private final JList<String> listaPassos = new JList<>(passos);
    private final JButton btnExportar = new JButton("Exportar CSV…");

    /** Monta o painel vazio, com o botão Exportar desabilitado. */
    public PainelResultado() {
        setBorder(BorderFactory.createTitledBorder("3. Resultado"));
        setLayout(new BorderLayout(12, 0));

        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 15f));
        lblTotais.setFont(lblTotais.getFont().deriveFont(Font.BOLD, 13f));

        txtMensagem.setEditable(false);
        txtMensagem.setLineWrap(true);
        txtMensagem.setWrapStyleWord(true);
        txtMensagem.setOpaque(false);
        txtMensagem.setBorder(null);
        txtMensagem.setFocusable(false);

        JPanel topo = new JPanel(new GridLayout(0, 1, 0, 2));
        topo.add(lblStatus);
        topo.add(lblTotais);
        topo.add(lblDetalhe);

        listaPassos.setFocusable(true); // permite ler com teclado/leitor de tela
        listaPassos.getAccessibleContext().setAccessibleName("Pontos do percurso, na ordem");
        JScrollPane rolagem = new JScrollPane(listaPassos);

        btnExportar.setMnemonic(KeyEvent.VK_E);
        btnExportar.setToolTipText("Salva a rota calculada (pontos na ordem, vias e totais) em um arquivo CSV");
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        acoes.add(btnExportar);

        JPanel rodape = new JPanel(new BorderLayout(0, 4));
        rodape.add(lblVias, BorderLayout.NORTH);
        rodape.add(txtMensagem, BorderLayout.CENTER);
        rodape.add(acoes, BorderLayout.SOUTH);

        // Faixa abaixo do mapa: resumo (status, totais, vias, exportar) à esquerda e o
        // percurso ponto a ponto à direita.
        JPanel resumo = new JPanel(new BorderLayout(0, 6));
        resumo.add(topo, BorderLayout.NORTH);
        resumo.add(rodape, BorderLayout.CENTER);
        resumo.setPreferredSize(new Dimension(430, 1));

        rolagem.setPreferredSize(new Dimension(260, 150));
        add(resumo, BorderLayout.WEST);
        add(rolagem, BorderLayout.CENTER);
        setMinimumSize(new Dimension(0, 170));
        limpar();
    }

    /** Estado inicial / após nova importação. */
    public void limpar() {
        lblStatus.setForeground(Color.DARK_GRAY);
        lblStatus.setText("Nenhuma rota calculada ainda.");
        lblTotais.setText(" ");
        lblDetalhe.setText(" ");
        lblVias.setText(" ");
        txtMensagem.setText("");
        passos.clear();
        btnExportar.setEnabled(false);
    }

    /**
     * O botão Exportar só fica ativo quando há uma rota com caminho na tela.
     *
     * @param exportavel {@code true} para liberar o botão
     */
    public void setExportavel(boolean exportavel) {
        btnExportar.setEnabled(exportavel);
    }

    /**
     * Avisa quando o usuário clica em Exportar CSV.
     *
     * @param l ouvinte do clique
     */
    public void addExportarListener(ActionListener l) {
        btnExportar.addActionListener(l);
    }

    JButton getBotaoExportar() { return btnExportar; }

    /** Mostra que o cálculo está em andamento. */
    public void mostrarCalculando() {
        limpar();
        lblStatus.setText("Calculando…");
    }

    /**
     * Mostra a rota (ou o aviso de sem caminho).
     *
     * @param r           rota devolvida pela API
     * @param pontosPorId usado para trocar o id pelo nome do local (se faltar, mostra só o id)
     */
    public void mostrar(RotaResultado r, Map<String, PontoRede> pontosPorId) {
        limpar();
        lblDetalhe.setText(r.criterio().getDescricao() + "  |  rede versão " + r.versaoRede()
                + "  |  calculada em " + Mensagens.dataHora(r.calculadaEm()));

        if (r.semCaminho()) {
            lblStatus.setForeground(Estilo.LARANJA);
            lblStatus.setText("Sem caminho disponível");
            lblTotais.setText("Distância: —   |   Tempo: —");
            String msg = r.mensagem();
            txtMensagem.setText(msg != null && !msg.isBlank() ? msg
                    : "Não há caminho disponível entre os pontos escolhidos com as vias atuais.");
            return;
        }

        lblStatus.setForeground(Estilo.VERDE);
        lblStatus.setText("Rota calculada");
        lblTotais.setText("Distância total: " + Mensagens.km(r.distanciaTotalKm())
                + "   |   Tempo total: " + Mensagens.min(r.tempoTotalMin()));

        int n = 1;
        for (String id : r.pontos()) {
            PontoRede p = pontosPorId.get(id);
            passos.addElement(n++ + ". " + (p != null ? p.toString() : id));
        }
        if (!r.vias().isEmpty()) {
            lblVias.setText("Vias percorridas: " + String.join(" → ", r.vias()));
        }
        if (r.pontos().size() == 1) {
            txtMensagem.setText("Origem e destino coincidem: distância e tempo são zero.");
        }
    }
}
