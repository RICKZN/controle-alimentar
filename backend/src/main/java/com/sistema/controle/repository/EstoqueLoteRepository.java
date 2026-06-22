package com.sistema.controle.repository;

import com.sistema.controle.model.EstoqueLote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EstoqueLoteRepository extends JpaRepository<EstoqueLote, Long> {
    List<EstoqueLote> findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(String nome);
    List<EstoqueLote> findByQuantidadeGreaterThanOrderByDataValidadeAscDataCompraAscIdAsc(Double quantidade);
    List<EstoqueLote> findByDataCompraBetween(LocalDate inicio, LocalDate fim);
    List<EstoqueLote> findByDataValidadeBetween(LocalDate inicio, LocalDate fim);
}
