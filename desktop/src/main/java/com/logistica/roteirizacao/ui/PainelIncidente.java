package com.logistica.roteirizacao.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Seção 4: bloquear uma via (registrar incidente). Depois do bloqueio, a janela recalcula a
 * última rota sem a via. Vias já bloqueadas saem da lista, porque a API recusaria (409).
 */
public class PainelIncidente extends JPanel {

    static final String DESCRICAO_PADRAO = "Alagamento simulado";
    private static final int DESCRICAO_MINIMA = 3;
    private static final int DESCRICAO_MAXIMA = 200;

    private final JComboBox<ViaRede> comboVia = new JComboBox<>();
    private final JTextField campoDescricao = new JTextField(DESCRICAO_PADRAO, 24);
    private final JButton btnBloquear = new JButton("Bloquear via");
    private final JLabel erroVia = Estilo.rotuloErro();
    private final JLabel erroDescricao = Estilo.rotuloErro();
    private final JLabel lblBloqueadas = new JLabel();
    private final TreeSet<String> bloqueadas = new TreeSet<>();
    private Map<String, PontoRede> pontosPorId = Map.of();

    public PainelIncidente() {
        setBorder(BorderFactory.createTitledBorder("4. Bloquear via (incidente)"));
        setLayout(new GridBagLayout());

        comboVia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                String texto = valor == null ? "Selecione uma via…" : descrever((ViaRede) valor);
                return super.getListCellRendererComponent(lista, texto, indice, selecionado, foco);
            }
        });
        comboVia.setPrototypeDisplayValue(new ViaRede("V00", "P00", "P00", true));
        campoDescricao.setToolTipText("Motivo do bloqueio, de " + DESCRICAO_MINIMA + " a " + DESCRICAO_MAXIMA
                + " caracteres");

        JLabel lblVia = new JLabel("Via:");
        JLabel lblDescricao = new JLabel("Descrição:");
        lblVia.setDisplayedMnemonic(KeyEvent.VK_A);
        lblVia.setLabelFor(comboVia);
        lblDescricao.setDisplayedMnemonic(KeyEvent.VK_R);
        lblDescricao.setLabelFor(campoDescricao);
        btnBloquear.setMnemonic(KeyEvent.VK_B);
        btnBloquear.setToolTipText("Registra o incidente na API e recalcula a última rota sem esta via");

        adicionar(lblVia, 0, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(comboVia, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(erroVia, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblDescricao, 0, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(campoDescricao, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(erroDescricao, 1, 3, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnBloquear, 0, 4, 1, 0, GridBagConstraints.NONE);
        adicionar(lblBloqueadas, 1, 4, 1, 1, GridBagConstraints.HORIZONTAL);

        setVias(List.of(), Map.of());
        setHabilitado(false);
    }

    private void adicionar(Component c, int x, int y, int largura, double pesoX, int fill) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = largura;
        g.weightx = pesoX;
        g.fill = fill;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 4, 2, 4);
        add(c, g);
    }

    /** "V24 — Distrito Industrial ↔ UNIT": nomes no lugar dos ids, quando conhecidos. */
    private String descrever(ViaRede v) {
        return v.getId() + " — " + nome(v.getOrigemId()) + " ↔ " + nome(v.getDestinoId());
    }

    private String nome(String pontoId) {
        PontoRede p = pontosPorId.get(pontoId);
        return p == null ? pontoId : p.getNome();
    }

    /** Lista as vias da rede importada. Vias desabilitadas no CSV já estão fora do cálculo e não entram. */
    public void setVias(List<ViaRede> vias, Map<String, PontoRede> pontos) {
        this.pontosPorId = Map.copyOf(pontos);
        bloqueadas.clear();
        DefaultComboBoxModel<ViaRede> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(null); // "Selecione uma via…"
        for (ViaRede v : vias) {
            if (v.isHabilitada()) {
                modelo.addElement(v);
            }
        }
        comboVia.setModel(modelo);
        campoDescricao.setText(DESCRICAO_PADRAO);
        atualizarBloqueadas();
        limparErros();
    }

    /** Depois do sucesso na API: tira a via da lista e a mostra entre as bloqueadas. */
    public void marcarBloqueada(String viaId) {
        bloqueadas.add(viaId);
        DefaultComboBoxModel<ViaRede> modelo = (DefaultComboBoxModel<ViaRede>) comboVia.getModel();
        for (int i = modelo.getSize() - 1; i >= 1; i--) {
            if (modelo.getElementAt(i).getId().equals(viaId)) {
                modelo.removeElementAt(i);
            }
        }
        comboVia.setSelectedIndex(0);
        atualizarBloqueadas();
    }

    private void atualizarBloqueadas() {
        lblBloqueadas.setText(bloqueadas.isEmpty() ? "Vias bloqueadas: nenhuma"
                : "Vias bloqueadas: " + String.join(", ", bloqueadas));
    }

    /** Mesmas regras da API (via obrigatória, descrição de 3 a 200 caracteres), com o erro sob o campo. */
    public boolean validar() {
        limparErros();
        boolean ok = true;
        if (getVia() == null) {
            Estilo.mostrarErro(erroVia, Mensagens.ESCOLHER_VIA);
            ok = false;
        }
        int tamanho = getDescricao().length();
        if (tamanho < DESCRICAO_MINIMA || tamanho > DESCRICAO_MAXIMA) {
            Estilo.mostrarErro(erroDescricao, Mensagens.DESCRICAO_INCIDENTE);
            ok = false;
        }
        return ok;
    }

    /** Erro da API por campo (viaId, descricao). */
    public void setErroCampo(String campo, String mensagem) {
        if ("viaId".equals(campo)) {
            Estilo.mostrarErro(erroVia, mensagem);
        } else if ("descricao".equals(campo)) {
            Estilo.mostrarErro(erroDescricao, mensagem);
        }
    }

    public void limparErros() {
        Estilo.limpar(erroVia);
        Estilo.limpar(erroDescricao);
    }

    public void setHabilitado(boolean habilitado) {
        comboVia.setEnabled(habilitado);
        campoDescricao.setEnabled(habilitado);
        btnBloquear.setEnabled(habilitado);
    }

    public ViaRede getVia() { return (ViaRede) comboVia.getSelectedItem(); }
    public String getDescricao() { return campoDescricao.getText().trim(); }

    /** Seleciona a via pelo id (usado nos testes). Devolve false se ela não está na lista. */
    boolean selecionarVia(String viaId) {
        for (int i = 1; i < comboVia.getItemCount(); i++) {
            if (comboVia.getItemAt(i).getId().equals(viaId)) {
                comboVia.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    public void addBloquearListener(ActionListener l) {
        btnBloquear.addActionListener(l);
    }

    JButton getBotaoBloquear() { return btnBloquear; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
