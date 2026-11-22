package com.logistica.roteirizacao.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.geom.Point2D;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Zoom e escolha de ponto do mapa, sem abrir janela (só cálculo de posições). */
class PainelMapaTest {

    private PainelMapa mapa;

    @BeforeEach
    void montar() {
        mapa = new PainelMapa();
        mapa.setSize(600, 400);
        mapa.setRede(List.of(
                new PontoRede("A", "Ponto A", -10.90, -37.10),
                new PontoRede("B", "Ponto B", -10.95, -37.05)),
                List.of(new ViaRede("V1", "A", "B", true)));
    }

    @Test
    void zoomMantemFixoOPontoSobOCursor() {
        Point2D.Double a = mapa.calcularPosicoes().get("A");

        mapa.zoomEm(2.0, a);

        Point2D.Double depois = mapa.calcularPosicoes().get("A");
        assertEquals(2.0, mapa.getZoom(), 1e-9);
        assertEquals(a.x, depois.x, 1e-6);
        assertEquals(a.y, depois.y, 1e-6);
    }

    @Test
    void zoomFicaEntreOMinimoEOMaximo() {
        mapa.zoomEm(0.1, new Point2D.Double(300, 200));
        assertEquals(PainelMapa.ZOOM_MINIMO, mapa.getZoom(), 1e-9);

        mapa.zoomEm(1000, new Point2D.Double(300, 200));
        assertEquals(PainelMapa.ZOOM_MAXIMO, mapa.getZoom(), 1e-9);
    }

    @Test
    void ajustarVoltaParaARedeInteira() {
        Point2D.Double original = mapa.calcularPosicoes().get("B");
        mapa.zoomEm(3, new Point2D.Double(10, 10));

        mapa.ajustarVista();

        assertEquals(original, mapa.calcularPosicoes().get("B"));
    }

    @Test
    void cliquePertoDoPontoAchaOPontoEForaNaoAchaNada() {
        Point2D.Double b = mapa.calcularPosicoes().get("B");

        assertEquals("B", mapa.pontoEm(new Point2D.Double(b.x + 4, b.y - 3)));
        assertNull(mapa.pontoEm(new Point2D.Double(b.x + 40, b.y + 40)));
    }
}
