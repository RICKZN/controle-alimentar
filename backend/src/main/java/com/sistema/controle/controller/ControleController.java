package com.sistema.controle.controller;

import com.sistema.controle.model.*;
import com.sistema.controle.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/controle")
@CrossOrigin(origins = "*")
public class ControleController {

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EstoqueLoteRepository estoqueLoteRepository;

    @Autowired
    private PratoDoDiaRepository pratoDoDiaRepository;

    @Autowired
    private RegistroAtendimentoRepository registroAtendimentoRepository;


    // ============================================================
    // ALUNOS
    // ============================================================

    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() {
        return alunoRepository.findAll();
    }

    @PostMapping("/alunos")
    public ResponseEntity<?> criarAluno(@RequestBody Aluno aluno) {

        if (aluno == null || aluno.getMatricula() == null
                || aluno.getMatricula().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("A matrícula do aluno é obrigatória.");
        }

        if (alunoRepository.findByMatricula(aluno.getMatricula()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body("Matrícula já cadastrada.");
        }

        return ResponseEntity.ok(alunoRepository.save(aluno));
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> deletarAluno(@PathVariable Long id) {

        if (!alunoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        alunoRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }


    // ============================================================
    // ESTOQUE
    // ============================================================

    @GetMapping("/estoque")
    public List<Estoque> listarEstoque() {
        return estoqueRepository.findAll();
    }

    @GetMapping("/estoque/lotes")
    public List<EstoqueLote> listarLotes() {
        return estoqueLoteRepository.findAll();
    }

    @PostMapping("/estoque")
    public ResponseEntity<?> cadastrarAlimentoEDirecionarLote(
            @RequestBody Map<String, Object> payload) {

        try {

            String nome = payload.get("nomeAlimento") != null
                    ? payload.get("nomeAlimento").toString().trim()
                    : null;

            String unidade = payload.get("unidadeMedida") != null
                    ? payload.get("unidadeMedida").toString().trim()
                    : null;

            if (nome == null || nome.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("O nome do alimento é obrigatório.");
            }

            if (unidade == null || unidade.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("A unidade de medida é obrigatória.");
            }

            Double qtdInicial = Double.parseDouble(
                    payload.get("quantidadeInicial").toString()
            );

            LocalDate validade = LocalDate.parse(
                    payload.get("dataValidadeInicial").toString()
            );

            LocalDate entrada =
                    payload.get("dataEntradaInicial") != null
                            ? LocalDate.parse(
                            payload.get("dataEntradaInicial").toString()
                    )
                            : LocalDate.now();

            String resp = payload.get("responsavel") != null
                    ? payload.get("responsavel").toString()
                    : null;


            // Procurar alimento existente usando os campos reais
            // da entidade Estoque: nome, unidade e quantidade.
            Estoque estoqueItem = estoqueRepository.findAll()
                    .stream()
                    .filter(e ->
                            e.getNome() != null &&
                            e.getNome().trim().equalsIgnoreCase(nome)
                    )
                    .findFirst()
                    .orElse(null);


            // Criar estoque caso ainda não exista
            if (estoqueItem == null) {

                estoqueItem = new Estoque();

                estoqueItem.setNome(nome);
                estoqueItem.setUnidade(unidade);
                estoqueItem.setQuantidade(0.0);

                estoqueItem = estoqueRepository.save(estoqueItem);
            }


            // Criar lote
            EstoqueLote lote = new EstoqueLote();

            lote.setEstoque(estoqueItem);
            lote.setNomeAlimento(estoqueItem.getNome());
            lote.setQuantidadeInicial(qtdInicial);
            lote.setQuantidadeAtual(qtdInicial);
            lote.setDataValidade(validade);
            lote.setDataEntrada(entrada);
            lote.setResponsavel(resp);

            estoqueLoteRepository.save(lote);


            recalcularQuantidadeEstoque(estoqueItem.getId());

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Erro ao cadastrar alimento: " + e.getMessage());
        }
    }


    @PostMapping("/estoque/{id}/lote")
    public ResponseEntity<?> adicionarLoteExistente(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        try {

            Estoque estoqueItem = estoqueRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Estoque não encontrado.")
                    );


            Double quantidade = Double.parseDouble(
                    payload.get("quantidade").toString()
            );

            LocalDate validade = LocalDate.parse(
                    payload.get("dataValidade").toString()
            );

            LocalDate entrada =
                    payload.get("dataEntrada") != null
                            ? LocalDate.parse(
                            payload.get("dataEntrada").toString()
                    )
                            : LocalDate.now();

            String resp = payload.get("responsavel") != null
                    ? payload.get("responsavel").toString()
                    : null;


            EstoqueLote lote = new EstoqueLote();

            lote.setEstoque(estoqueItem);
            lote.setNomeAlimento(estoqueItem.getNome());
            lote.setQuantidadeInicial(quantidade);
            lote.setQuantidadeAtual(quantidade);
            lote.setDataValidade(validade);
            lote.setDataEntrada(entrada);
            lote.setResponsavel(resp);

            estoqueLoteRepository.save(lote);

            recalcularQuantidadeEstoque(id);

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Erro ao adicionar lote: " + e.getMessage());
        }
    }


    @PostMapping("/estoque/{id}/consumir")
    public ResponseEntity<?> consumirManualmente(
            @PathVariable Long id,
            @RequestParam Double quantidade) {

        try {

            Estoque estoqueItem = estoqueRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Estoque não encontrado.")
                    );


            if (quantidade == null || quantidade <= 0) {
                return ResponseEntity.badRequest()
                        .body("A quantidade deve ser maior que zero.");
            }


            if (estoqueItem.getQuantidade() == null ||
                    estoqueItem.getQuantidade() < quantidade) {

                return ResponseEntity.badRequest()
                        .body("Insumos insuficientes para baixa manual.");
            }


            abaterLotesInsumo(id, quantidade);

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Erro ao consumir estoque: " + e.getMessage());
        }
    }


    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirInsumoCompleto(
            @PathVariable Long id) {

        if (!estoqueRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        estoqueRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/estoque/lotes/{id}")
    public ResponseEntity<?> deletarLoteEspecifico(
            @PathVariable Long id) {

        try {

            EstoqueLote lote = estoqueLoteRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Lote não encontrado.")
                    );

            Long estoqueId = lote.getEstoque().getId();

            estoqueLoteRepository.deleteById(id);

            recalcularQuantidadeEstoque(estoqueId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Erro ao excluir lote: " + e.getMessage());
        }
    }


    @PostMapping("/estoque/corrigir-duplicados")
    public ResponseEntity<?> corrigirDuplicadosSaneamento() {

        List<Estoque> todos = estoqueRepository.findAll();

        Map<String, List<Estoque>> agrupados =
                todos.stream()
                        .filter(e -> e.getNome() != null)
                        .collect(Collectors.groupingBy(
                                e -> e.getNome()
                                        .trim()
                                        .toLowerCase()
                        ));


        for (Map.Entry<String, List<Estoque>> entry :
                agrupados.entrySet()) {

            List<Estoque> itens = entry.getValue();

            if (itens.size() <= 1) {
                continue;
            }


            Estoque principal = itens.get(0);


            for (int i = 1; i < itens.size(); i++) {

                Estoque duplicado = itens.get(i);


                List<EstoqueLote> lotesDuplicados =
                        estoqueLoteRepository.findAll()
                                .stream()
                                .filter(lote ->
                                        lote.getEstoque() != null &&
                                        lote.getEstoque().getId()
                                                .equals(duplicado.getId())
                                )
                                .toList();


                for (EstoqueLote lote : lotesDuplicados) {

                    lote.setEstoque(principal);

                    estoqueLoteRepository.save(lote);
                }


                estoqueRepository.deleteById(
                        duplicado.getId()
                );
            }


            recalcularQuantidadeEstoque(
                    principal.getId()
            );
        }


        return ResponseEntity.ok().build();
    }


    // ============================================================
    // PRATO DO DIA
    // ============================================================

    @GetMapping("/prato-do-dia")
    public List<PratoDoDia> listarHistoricoDePratos() {

        return pratoDoDiaRepository
                .findAllByOrderByDataPratoDesc();
    }


    @GetMapping("/prato-do-dia/busca")
    public ResponseEntity<?> buscarPratoPorData(
            @RequestParam String data) {

        try {

            LocalDate localDate =
                    LocalDate.parse(data);

            Optional<PratoDoDia> prato =
                    pratoDoDiaRepository
                            .findByDataPrato(localDate);


            if (prato.isPresent()) {
                return ResponseEntity.ok(prato.get());
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Data inválida.");
        }
    }


    @PostMapping("/prato-do-dia")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> salvarPratoDoDia(
            @RequestBody Map<String, Object> payload) {

        try {

            String nomePrato =
                    payload.get("nomePrato") != null
                            ? payload.get("nomePrato").toString().trim()
                            : null;


            if (nomePrato == null || nomePrato.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("O nome do prato é obrigatório.");
            }


            LocalDate dataPrato =
                    LocalDate.parse(
                            payload.get("dataPrato").toString()
                    );


            List<Map<String, Object>> ingredientesJson =
                    (List<Map<String, Object>>)
                            payload.get("ingredientes");


            if (ingredientesJson == null) {
                ingredientesJson = new ArrayList<>();
            }


            if (pratoDoDiaRepository
                    .findByDataPrato(dataPrato)
                    .isPresent()) {

                return ResponseEntity.badRequest()
                        .body(
                                "Já existe um cardápio cadastrado para esta data."
                        );
            }


            PratoDoDia prato =
                    new PratoDoDia();

            prato.setNomePrato(nomePrato);
            prato.setDataPrato(dataPrato);


            List<PratoIngrediente> listaIngredientes =
                    new ArrayList<>();


            // Primeiro verificar todo o estoque,
            // antes de salvar o prato.
            for (Map<String, Object> ingMap :
                    ingredientesJson) {

                Long estoqueId =
                        Long.parseLong(
                                ingMap
                                        .get("estoqueId")
                                        .toString()
                        );

                Double qtdGasta =
                        Double.parseDouble(
                                ingMap
                                        .get("quantidadeGasta")
                                        .toString()
                        );


                Estoque est =
                        estoqueRepository
                                .findById(estoqueId)
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Item de estoque não encontrado."
                                        )
                                );


                if (est.getQuantidade() == null ||
                        est.getQuantidade() < qtdGasta) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Estoque insuficiente para o item: "
                                            + est.getNome()
                            );
                }
            }


            // Criar os ingredientes usando exatamente
            // os campos existentes em PratoIngrediente.
            for (Map<String, Object> ingMap :
                    ingredientesJson) {

                Long estoqueId =
                        Long.parseLong(
                                ingMap
                                        .get("estoqueId")
                                        .toString()
                        );

                Double qtdGasta =
                        Double.parseDouble(
                                ingMap
                                        .get("quantidadeGasta")
                                        .toString()
                        );


                Estoque est =
                        estoqueRepository
                                .findById(estoqueId)
                                .orElseThrow();


                PratoIngrediente pi =
                        new PratoIngrediente();


                pi.setNome(est.getNome());
                pi.setUnidade(est.getUnidade());
                pi.setQuantidade(qtdGasta);


                listaIngredientes.add(pi);


                abaterLotesInsumo(
                        estoqueId,
                        qtdGasta
                );
            }


            prato.setIngredientes(
                    listaIngredientes
            );


            PratoDoDia pratoSalvo =
                    pratoDoDiaRepository.save(prato);


            return ResponseEntity.ok(pratoSalvo);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao salvar prato do dia: "
                                    + e.getMessage()
                    );
        }
    }


    @DeleteMapping("/prato-do-dia/{id}")
    public ResponseEntity<?> deletarCardapio(
            @PathVariable Long id) {

        if (!pratoDoDiaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        pratoDoDiaRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }


    // ============================================================
    // REFEITÓRIO / ATENDIMENTO / SCANNER
    // ============================================================

    @PostMapping("/atendimento/registrar")
    public ResponseEntity<?> registrarAtendimento(
            @RequestParam String identificador) {

        try {

            Aluno aluno =
                    alunoRepository
                            .findByMatricula(identificador)
                            .orElse(null);


            if (aluno == null) {

                return ResponseEntity.badRequest()
                        .body(
                                "Aluno não encontrado para a matrícula: "
                                        + identificador
                        );
            }


            RegistroAtendimento registro =
                    new RegistroAtendimento();


            registro.setMatricula(
                    aluno.getMatricula()
            );

            registro.setDataHoraAtendimento(
                    LocalDateTime.now()
            );


            registroAtendimentoRepository.save(
                    registro
            );


            return ResponseEntity.ok(registro);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao registrar atendimento: "
                                    + e.getMessage()
                    );
        }
    }


    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    private void abaterLotesInsumo(
            Long estoqueId,
            Double quantidade) {

        if (quantidade == null || quantidade <= 0) {
            return;
        }


        List<EstoqueLote> lotes =
                estoqueLoteRepository
                        .findAll()
                        .stream()
                        .filter(lote ->
                                lote.getEstoque() != null &&
                                lote.getEstoque()
                                        .getId()
                                        .equals(estoqueId) &&
                                lote.getQuantidadeAtual() != null &&
                                lote.getQuantidadeAtual() > 0
                        )
                        .sorted(
                                Comparator.comparing(
                                        EstoqueLote::getDataValidade,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                        )
                        .collect(Collectors.toList());


        double restante = quantidade;


        for (EstoqueLote lote : lotes) {

            if (restante <= 0) {
                break;
            }


            double disponivel =
                    lote.getQuantidadeAtual();


            if (disponivel <= restante) {

                lote.setQuantidadeAtual(0.0);

                restante -= disponivel;

            } else {

                lote.setQuantidadeAtual(
                        disponivel - restante
                );

                restante = 0;
            }


            estoqueLoteRepository.save(lote);
        }


        recalcularQuantidadeEstoque(
                estoqueId
        );
    }


    private void recalcularQuantidadeEstoque(
            Long estoqueId) {

        Estoque estoque =
                estoqueRepository
                        .findById(estoqueId)
                        .orElseThrow();


        double total =
                estoqueLoteRepository
                        .findAll()
                        .stream()
                        .filter(lote ->
                                lote.getEstoque() != null &&
                                lote.getEstoque()
                                        .getId()
                                        .equals(estoqueId)
                        )
                        .map(EstoqueLote::getQuantidadeAtual)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();


        estoque.setQuantidade(total);

        estoqueRepository.save(estoque);
    }
}
