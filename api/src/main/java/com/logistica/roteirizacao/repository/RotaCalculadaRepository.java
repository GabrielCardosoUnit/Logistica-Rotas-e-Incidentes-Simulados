package com.logistica.roteirizacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.RotaCalculada;

/** Acesso às rotas já calculadas. */
public interface RotaCalculadaRepository extends JpaRepository<RotaCalculada, Long> {
}
