package com.logistica.roteirizacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.Ponto;

/** Acesso aos pontos da rede. */
public interface PontoRepository extends JpaRepository<Ponto, String> {
}
