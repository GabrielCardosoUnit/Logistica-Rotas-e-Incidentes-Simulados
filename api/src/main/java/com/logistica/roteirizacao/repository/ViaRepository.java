package com.logistica.roteirizacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.Via;

/** Acesso às vias da rede. */
public interface ViaRepository extends JpaRepository<Via, String> {
}
