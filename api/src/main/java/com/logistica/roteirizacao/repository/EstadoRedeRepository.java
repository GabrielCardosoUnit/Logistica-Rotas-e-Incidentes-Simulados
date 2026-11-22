package com.logistica.roteirizacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.EstadoRede;

/** Acesso ao estado da rede (versão atual). */
public interface EstadoRedeRepository extends JpaRepository<EstadoRede, Long> {
}
