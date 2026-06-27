package com.sistema.controle.repository;

import com.sistema.controle.model.PratoDoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PratoDoDiaRepository extends JpaRepository<PratoDoDia, Long> {
    Optional<PratoDoDia> findTopByDataOrderByIdDesc(LocalDate data);
    List<PratoDoDia> findAllByOrderByDataDescIdDesc();
}
