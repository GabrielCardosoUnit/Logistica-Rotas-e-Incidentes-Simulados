package com.logistica.roteirizacao;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RoteirizacaoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoteirizacaoApiApplication.class, args);
    }

    /**
     * Relógio único da aplicação. Injetado nos serviços para que os instantes
     * ("calculadaEm", "registradoEm") saiam no fuso configurado e possam ser
     * controlados nos testes.
     */
    @Bean
    public Clock relogio(@Value("${rede.fuso-horario}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
