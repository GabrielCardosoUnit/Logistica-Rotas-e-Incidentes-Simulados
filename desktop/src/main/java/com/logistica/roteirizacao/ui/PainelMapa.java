package com.logistica.roteirizacao.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Mapa esquemático: desenha os pontos nas posições (latitude/longitude) e as vias como linhas retas,
 * e destaca a rota calculada. É um desenho, não um mapa de ruas (a rede é simulada).
 *
 * <p>A rota não é indicada só por cor: tem traço mais grosso, círculos maiores, números de ordem
 * nos pontos, marcador quadrado na origem, losango no destino e uma legenda.
 */
public class PainelMapa extends JPanel {

    private static final int MARGEM = 28;
    private static final Color COR_VIA = new Color(0x9E, 0x9E, 0x9E);
    private static final Color COR_PONTO = new Color(0x37, 0x47, 0x4F);
    private static final Color COR_ROTA = new Color(0xD8, 0x43, 0x15);
    private static final Stroke TRACO_VIA = new BasicStroke(1.4f);
    private static final Stroke TRACO_VIA_BLOQUEADA = new BasicStroke(1.4f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] {5f, 5f}, 0f);

    private final List<PontoRede> pontos = new ArrayList<>();
    private final List<ViaRede> vias = new ArrayList<>();
    private final List<String> rota = new ArrayList<>();

    public PainelMapa() {
        setBorder(BorderFactory.createTitledBorder("Mapa esquemático"));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(520, 520));
        setToolTipText(""); // liga o tooltip; o texto vem de getToolTipText(MouseEvent)
        getAccessibleContext().setAccessibleName("Mapa esquemático da rede e da rota calculada");
    }

    /** Define a rede a desenhar e apaga a rota anterior. */
    public void setRede(List<PontoRede> novosPontos, List<ViaRede> novasVias) {
        pontos.clear();
        pontos.addAll(novosPontos);
        vias.clear();
        vias.addAll(novasVias);
        rota.clear();
        repaint();
    }

    /** Destaca a rota: ids dos pontos na ordem do percurso. */
    public void setRota(List<String> idsDosPontos) {
        rota.clear();
        rota.addAll(idsDosPontos);
        repaint();
    }

    public void limparRota() {
        rota.clear();
        repaint();
    }

    // ---- projeção: latitude/longitude -> pixels ------------------------------------------------

    /** Converte cada ponto em pixels, mantendo a proporção e deixando o norte para cima. */
    private Map<String, Point2D.Double> calcularPosicoes() {
        Map<String, Point2D.Double> pos = new LinkedHashMap<>();
        if (pontos.isEmpty()) {
            return pos;
        }
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (PontoRede p : pontos) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }
        // 1 grau de longitude é menor que 1 grau de latitude: corrige pela latitude média
        double fatorLon = Math.cos(Math.toRadians((minLat + maxLat) / 2));
        double largura = Math.max((maxLon - minLon) * fatorLon, 1e-9);
        double altura = Math.max(maxLat - minLat, 1e-9);

        Insets in = getInsets();
        double areaL = getWidth() - in.left - in.right - 2.0 * MARGEM;
        double areaA = getHeight() - in.top - in.bottom - 2.0 * MARGEM;
        double escala = Math.max(Math.min(areaL / largura, areaA / altura), 0.0);
        double sobraX = (areaL - largura * escala) / 2.0;
        double sobraY = (areaA - altura * escala) / 2.0;

        for (PontoRede p : pontos) {
            double x = in.left + MARGEM + sobraX + (p.getLongitude() - minLon) * fatorLon * escala;
            double y = in.top + MARGEM + sobraY + (maxLat - p.getLatitude()) * escala;
            pos.put(p.getId(), new Point2D.Double(x, y));
        }
        return pos;
    }

    // ---- desenho -------------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            if (pontos.isEmpty()) {
                desenharMensagemVazia(g2);
                return;
            }
            Map<String, Point2D.Double> pos = calcularPosicoes();
            desenharVias(g2, pos);
            desenharRota(g2, pos);
            desenharPontos(g2, pos);
            desenharLegenda(g2);
        } finally {
            g2.dispose();
        }
    }

    private void desenharMensagemVazia(Graphics2D g2) {
        String texto = "Importe a rede para ver o mapa.";
        g2.setColor(Color.GRAY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, getHeight() / 2);
    }

    private void desenharVias(Graphics2D g2, Map<String, Point2D.Double> pos) {
        g2.setColor(COR_VIA);
        for (ViaRede v : vias) {
            Point2D a = pos.get(v.getOrigemId());
            Point2D b = pos.get(v.getDestinoId());
            if (a == null || b == null) {
                continue;
            }
            g2.setStroke(v.isHabilitada() ? TRACO_VIA : TRACO_VIA_BLOQUEADA);
            g2.draw(new Line2D.Double(a, b));
        }
    }

    private void desenharRota(Graphics2D g2, Map<String, Point2D.Double> pos) {
        if (rota.size() < 2) {
            return;
        }
        Path2D.Double caminho = new Path2D.Double();
        boolean primeiro = true;
        for (String id : rota) {
            Point2D p = pos.get(id);
            if (p == null) {
                continue;
            }
            if (primeiro) {
                caminho.moveTo(p.getX(), p.getY());
                primeiro = false;
            } else {
                caminho.lineTo(p.getX(), p.getY());
            }
        }
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE); // contorno branco: o traço continua visível sobre as vias cinza
        g2.draw(caminho);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COR_ROTA);
        g2.draw(caminho);
    }

    private void desenharPontos(Graphics2D g2, Map<String, Point2D.Double> pos) {
        Font normal = g2.getFont().deriveFont(Font.PLAIN, 11f);
        Font negrito = g2.getFont().deriveFont(Font.BOLD, 12f);
        String origem = rota.isEmpty() ? null : rota.get(0);
        String destino = rota.isEmpty() ? null : rota.get(rota.size() - 1);

        for (PontoRede p : pontos) {
            Point2D.Double c = pos.get(p.getId());
            int ordem = rota.indexOf(p.getId());
            boolean naRota = ordem >= 0;

            if (naRota && p.getId().equals(origem)) {
                g2.setColor(COR_ROTA);
                g2.fill(new Rectangle2D.Double(c.x - 8, c.y - 8, 16, 16)); // origem: quadrado
            } else if (naRota && p.getId().equals(destino)) {
                Path2D.Double losango = new Path2D.Double();
                losango.moveTo(c.x, c.y - 10);
                losango.lineTo(c.x + 10, c.y);
                losango.lineTo(c.x, c.y + 10);
                losango.lineTo(c.x - 10, c.y);
                losango.closePath();
                g2.setColor(COR_ROTA);
                g2.fill(losango); // destino: losango
            } else if (naRota) {
                g2.setColor(COR_ROTA);
                g2.fill(new Ellipse2D.Double(c.x - 6, c.y - 6, 12, 12));
            } else {
                g2.setColor(COR_PONTO);
                g2.fill(new Ellipse2D.Double(c.x - 4, c.y - 4, 8, 8));
            }

            g2.setFont(naRota ? negrito : normal);
            g2.setColor(naRota ? COR_ROTA.darker() : COR_PONTO);
            String rotulo = naRota ? (ordem + 1) + ". " + p.getId() : p.getId();
            g2.drawString(rotulo, (float) c.x + 10, (float) c.y - 8);
        }
    }

    private void desenharLegenda(Graphics2D g2) {
        Insets in = getInsets();
        int x = in.left + 10;
        int y = getHeight() - in.bottom - 8 - 5 * 18;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));

        g2.setStroke(new BasicStroke(1f)); // a borda da caixa não herda a espessura da rota
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRect(x - 4, y - 14, 178, 5 * 18 + 8);
        g2.setColor(COR_VIA);
        g2.drawRect(x - 4, y - 14, 178, 5 * 18 + 8);

        legendaLinha(g2, x, y, TRACO_VIA, COR_VIA, "Via da rede");
        legendaLinha(g2, x, y + 18, TRACO_VIA_BLOQUEADA, COR_VIA, "Via bloqueada ou desabilitada");
        legendaLinha(g2, x, y + 36, new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
                COR_ROTA, "Rota calculada");
        g2.setColor(COR_ROTA);
        g2.fill(new Rectangle2D.Double(x + 8, y + 46, 10, 10));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Origem (quadrado)", x + 32, y + 56);
        Path2D.Double l = new Path2D.Double();
        l.moveTo(x + 13, y + 63);
        l.lineTo(x + 19, y + 69);
        l.lineTo(x + 13, y + 75);
        l.lineTo(x + 7, y + 69);
        l.closePath();
        g2.setColor(COR_ROTA);
        g2.fill(l);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Destino (losango)", x + 32, y + 74);
    }

    private void legendaLinha(Graphics2D g2, int x, int y, Stroke traco, Color cor, String texto) {
        g2.setStroke(traco);
        g2.setColor(cor);
        g2.draw(new Line2D.Double(x, y - 4, x + 24, y - 4));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(texto, x + 32, y);
    }

    /** Ao passar o mouse sobre um ponto, mostra id e nome. */
    @Override
    public String getToolTipText(MouseEvent e) {
        for (Map.Entry<String, Point2D.Double> entrada : calcularPosicoes().entrySet()) {
            if (entrada.getValue().distance(e.getPoint()) <= 10) {
                for (PontoRede p : pontos) {
                    if (p.getId().equals(entrada.getKey())) {
                        return p.toString();
                    }
                }
            }
        }
        return null;
    }
}
