package com.sistema.controle.repository;
import com.sistema.controle.model.RegistroConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RegistroConsumoRepository extends JpaRepository<RegistroConsumo, Long> {
    List<RegistroConsumo> findByDataBetween(LocalDate inicio, LocalDate fim);
}
