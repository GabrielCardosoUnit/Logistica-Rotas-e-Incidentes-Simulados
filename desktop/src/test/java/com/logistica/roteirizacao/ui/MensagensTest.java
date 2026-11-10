package com.logistica.roteirizacao.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MensagensTest {

    @Test
    void distanciaDaCidadeSaiEmQuilometrosComVirgula() {
        assertEquals("8,5 km", Mensagens.km(8.5));
        assertEquals("11,7 km", Mensagens.km(11.7));
        assertEquals("1,0 km", Mensagens.km(1.0));
    }

    @Test
    void distanciaDoCampusSaiEmMetros() {
        assertEquals("330 m", Mensagens.km(0.33));
        assertEquals("40 m", Mensagens.km(0.04));
        assertEquals("0 m", Mensagens.km(0.0));
    }

    @Test
    void semValorMostraTraco() {
        assertEquals("—", Mensagens.km(null));
        assertEquals("—", Mensagens.min(null));
    }

    @Test
    void tempoSaiEmMinutosComVirgula() {
        assertEquals("5,2 min", Mensagens.min(5.2));
    }
}
