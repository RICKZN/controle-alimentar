package com.sistema.controle.controller;

import com.sistema.controle.model.Aluno;
import com.sistema.controle.model.Estoque;
import com.sistema.controle.model.EstoqueLote;
import com.sistema.controle.model.PratoDoDia;
import com.sistema.controle.model.PratoIngrediente;
import com.sistema.controle.model.RegistroAtendimento;
import com.sistema.controle.repository.AlunoRepository;
import com.sistema.controle.repository.EstoqueLoteRepository;
import com.sistema.controle.repository.EstoqueRepository;
import com.sistema.controle.repository.PratoDoDiaRepository;
import com.sistema.controle.repository.RegistroAtendimentoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        if (aluno == null) {
            return ResponseEntity.badRequest()
                    .body("Aluno inválido.");
        }

        if (aluno.getMatricula() == null ||
                aluno.getMatricula().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("A matrícula é obrigatória.");
        }

        if (alunoRepository
                .findByMatricula(aluno.getMatricula())
                .isPresent()) {

            return ResponseEntity.badRequest()
                    .body("Matrícula já cadastrada.");
        }

        return ResponseEntity.ok(
                alunoRepository.save(aluno)
        );
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> deletarAluno(
            @PathVariable Long id) {

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

        return estoqueLoteRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                EstoqueLote::getDataValidade,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .collect(Collectors.toList());
    }


    // ------------------------------------------------------------
    // CADASTRAR ALIMENTO + LOTE
    // ------------------------------------------------------------

    @PostMapping("/estoque")
    public ResponseEntity<?> cadastrarAlimento(
            @RequestBody Map<String, Object> payload) {

        try {

            String nome = obterTexto(
                    payload,
                    "nomeAlimento"
            );

            String unidade = obterTexto(
                    payload,
                    "unidadeMedida"
            );

            if (nome == null || nome.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("O nome do alimento é obrigatório.");
            }

            if (unidade == null || unidade.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("A unidade de medida é obrigatória.");
            }


            Double quantidadeInicial =
                    Double.parseDouble(
                            payload
                                    .get("quantidadeInicial")
                                    .toString()
                    );


            if (quantidadeInicial <= 0) {

                return ResponseEntity.badRequest()
                        .body(
                                "A quantidade inicial deve ser maior que zero."
                        );
            }


            LocalDate dataValidade =
                    payload.get("dataValidadeInicial") != null
                            ? LocalDate.parse(
                            payload
                                    .get("dataValidadeInicial")
                                    .toString()
                    )
                            : null;


            LocalDate dataCompra =
                    payload.get("dataEntradaInicial") != null
                            ? LocalDate.parse(
                            payload
                                    .get("dataEntradaInicial")
                                    .toString()
                    )
                            : LocalDate.now();


            String responsavel =
                    payload.get("responsavel") != null
                            ? payload.get("responsavel").toString()
                            : null;


            // ====================================================
            // ESTOQUE PRINCIPAL
            // ====================================================

            Estoque estoque =
                    estoqueRepository
                            .findAll()
                            .stream()
                            .filter(e ->
                                    e.getNome() != null &&
                                    e.getNome()
                                            .trim()
                                            .equalsIgnoreCase(nome)
                            )
                            .findFirst()
                            .orElse(null);


            if (estoque == null) {

                estoque = new Estoque();

                estoque.setNome(nome);
                estoque.setUnidade(unidade);
                estoque.setQuantidade(0.0);

                estoque =
                        estoqueRepository.save(estoque);
            }


            // ====================================================
            // LOTE
            // ====================================================

            EstoqueLote lote =
                    new EstoqueLote();

            lote.setNome(nome);
            lote.setUnidade(unidade);
            lote.setQuantidade(quantidadeInicial);
            lote.setDataCompra(dataCompra);
            lote.setDataValidade(dataValidade);
            lote.setUsuarioResponsavel(responsavel);
            lote.setDataCadastro(LocalDate.now());


            estoqueLoteRepository.save(lote);


            // Recalcular estoque principal
            recalcularQuantidadeEstoque(nome);


            return ResponseEntity.ok(
                    estoqueRepository.findById(
                            estoque.getId()
                    ).orElse(estoque)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao cadastrar alimento: "
                                    + e.getMessage()
                    );
        }
    }


    // ------------------------------------------------------------
    // ADICIONAR LOTE A UM ALIMENTO EXISTENTE
    // ------------------------------------------------------------

    @PostMapping("/estoque/{id}/lote")
    public ResponseEntity<?> adicionarLote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        try {

            Estoque estoque =
                    estoqueRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Alimento não encontrado."
                                    )
                            );


            Double quantidade =
                    Double.parseDouble(
                            payload
                                    .get("quantidade")
                                    .toString()
                    );


            if (quantidade <= 0) {

                return ResponseEntity.badRequest()
                        .body(
                                "A quantidade deve ser maior que zero."
                        );
            }


            LocalDate dataValidade =
                    payload.get("dataValidade") != null
                            ? LocalDate.parse(
                            payload
                                    .get("dataValidade")
                                    .toString()
                    )
                            : null;


            LocalDate dataCompra =
                    payload.get("dataEntrada") != null
                            ? LocalDate.parse(
                            payload
                                    .get("dataEntrada")
                                    .toString()
                    )
                            : LocalDate.now();


            String responsavel =
                    payload.get("responsavel") != null
                            ? payload
                                    .get("responsavel")
                                    .toString()
                            : null;


            EstoqueLote lote =
                    new EstoqueLote();

            lote.setNome(
                    estoque.getNome()
            );

            lote.setUnidade(
                    estoque.getUnidade()
            );

            lote.setQuantidade(
                    quantidade
            );

            lote.setDataCompra(
                    dataCompra
            );

            lote.setDataValidade(
                    dataValidade
            );

            lote.setUsuarioResponsavel(
                    responsavel
            );

            lote.setDataCadastro(
                    LocalDate.now()
            );


            estoqueLoteRepository.save(lote);


            recalcularQuantidadeEstoque(
                    estoque.getNome()
            );


            return ResponseEntity.ok(
                    estoqueRepository
                            .findById(id)
                            .orElse(estoque)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao adicionar lote: "
                                    + e.getMessage()
                    );
        }
    }


    // ------------------------------------------------------------
    // CONSUMIR ESTOQUE MANUALMENTE
    // ------------------------------------------------------------

    @PostMapping("/estoque/{id}/consumir")
    public ResponseEntity<?> consumirManualmente(
            @PathVariable Long id,
            @RequestParam Double quantidade) {

        try {

            if (quantidade == null ||
                    quantidade <= 0) {

                return ResponseEntity.badRequest()
                        .body(
                                "A quantidade deve ser maior que zero."
                        );
            }


            Estoque estoque =
                    estoqueRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Alimento não encontrado."
                                    )
                            );


            if (estoque.getQuantidade() == null ||
                    estoque.getQuantidade() < quantidade) {

                return ResponseEntity.badRequest()
                        .body(
                                "Estoque insuficiente."
                        );
            }


            boolean abatido =
                    abaterLotesInsumo(
                            estoque.getNome(),
                            quantidade
                    );


            if (!abatido) {

                return ResponseEntity.badRequest()
                        .body(
                                "Não foi possível realizar a baixa."
                        );
            }


            recalcularQuantidadeEstoque(
                    estoque.getNome()
            );


            return ResponseEntity.ok().build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao consumir estoque: "
                                    + e.getMessage()
                    );
        }
    }


    // ------------------------------------------------------------
    // EXCLUIR ALIMENTO
    // ------------------------------------------------------------

    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirInsumoCompleto(
            @PathVariable Long id) {

        Optional<Estoque> estoque =
                estoqueRepository.findById(id);


        if (estoque.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        String nome =
                estoque.get().getNome();


        // Excluir os lotes correspondentes
        List<EstoqueLote> lotes =
                estoqueLoteRepository
                        .findAll()
                        .stream()
                        .filter(lote ->
                                lote.getNome() != null &&
                                lote.getNome()
                                        .equalsIgnoreCase(nome)
                        )
                        .collect(Collectors.toList());


        for (EstoqueLote lote : lotes) {
            estoqueLoteRepository.deleteById(
                    lote.getId()
            );
        }


        estoqueRepository.deleteById(id);


        return ResponseEntity.ok().build();
    }


    // ------------------------------------------------------------
    // EXCLUIR LOTE
    // ------------------------------------------------------------

    @DeleteMapping("/estoque/lotes/{id}")
    public ResponseEntity<?> deletarLoteEspecifico(
            @PathVariable Long id) {

        Optional<EstoqueLote> lote =
                estoqueLoteRepository.findById(id);


        if (lote.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        String nome =
                lote.get().getNome();


        estoqueLoteRepository.deleteById(id);


        if (nome != null) {
            recalcularQuantidadeEstoque(nome);
        }


        return ResponseEntity.ok().build();
    }


    // ------------------------------------------------------------
    // CORRIGIR DUPLICADOS
    // ------------------------------------------------------------

    @PostMapping("/estoque/corrigir-duplicados")
    public ResponseEntity<?> corrigirDuplicadosSaneamento() {

        List<Estoque> todos =
                estoqueRepository.findAll();


        Map<String, List<Estoque>> agrupados =
                todos.stream()
                        .filter(e -> e.getNome() != null)
                        .collect(
                                Collectors.groupingBy(
                                        e -> e.getNome()
                                                .trim()
                                                .toLowerCase()
                                )
                        );


        for (Map.Entry<String, List<Estoque>> entry :
                agrupados.entrySet()) {

            List<Estoque> itens =
                    entry.getValue();


            if (itens.size() <= 1) {
                continue;
            }


            Estoque principal =
                    itens.get(0);


            for (int i = 1;
                 i < itens.size();
                 i++) {

                Estoque duplicado =
                        itens.get(i);


                // Somar a quantidade do duplicado
                double quantidadePrincipal =
                        principal.getQuantidade() != null
                                ? principal.getQuantidade()
                                : 0.0;

                double quantidadeDuplicada =
                        duplicado.getQuantidade() != null
                                ? duplicado.getQuantidade()
                                : 0.0;


                principal.setQuantidade(
                        quantidadePrincipal +
                                quantidadeDuplicada
                );


                estoqueRepository.deleteById(
                        duplicado.getId()
                );
            }


            estoqueRepository.save(principal);

            recalcularQuantidadeEstoque(
                    principal.getNome()
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

                return ResponseEntity.ok(
                        prato.get()
                );
            }


            return ResponseEntity
                    .notFound()
                    .build();

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Data inválida."
                    );
        }
    }


    // ------------------------------------------------------------
    // SALVAR PRATO DO DIA
    // ------------------------------------------------------------

    @PostMapping("/prato-do-dia")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> salvarPratoDoDia(
            @RequestBody Map<String, Object> payload) {

        try {

            String nomePrato =
                    payload.get("nomePrato") != null
                            ? payload
                            .get("nomePrato")
                            .toString()
                            .trim()
                            : null;


            if (nomePrato == null ||
                    nomePrato.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(
                                "O nome do prato é obrigatório."
                        );
            }


            LocalDate dataPrato =
                    LocalDate.parse(
                            payload
                                    .get("dataPrato")
                                    .toString()
                    );


            if (pratoDoDiaRepository
                    .findByDataPrato(dataPrato)
                    .isPresent()) {

                return ResponseEntity.badRequest()
                        .body(
                                "Já existe um cardápio cadastrado para esta data."
                        );
            }


            List<Map<String, Object>>
                    ingredientesJson =
                    payload.get("ingredientes")
                            instanceof List
                            ? (List<Map<String, Object>>)
                            payload.get("ingredientes")
                            : new ArrayList<>();


            // ====================================================
            // PRIMEIRO: VALIDAR TODOS OS INGREDIENTES
            // ====================================================

            for (Map<String, Object> ingrediente :
                    ingredientesJson) {

                if (ingrediente.get("estoqueId") == null ||
                        ingrediente.get("quantidadeGasta") == null) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Ingrediente inválido."
                            );
                }


                Long estoqueId =
                        Long.parseLong(
                                ingrediente
                                        .get("estoqueId")
                                        .toString()
                        );


                Double quantidade =
                        Double.parseDouble(
                                ingrediente
                                        .get("quantidadeGasta")
                                        .toString()
                        );


                if (quantidade <= 0) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "A quantidade do ingrediente deve ser maior que zero."
                            );
                }


                Estoque estoque =
                        estoqueRepository
                                .findById(estoqueId)
                                .orElse(null);


                if (estoque == null) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Ingrediente não encontrado no estoque."
                            );
                }


                if (estoque.getQuantidade() == null ||
                        estoque.getQuantidade() < quantidade) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Estoque insuficiente para o item: "
                                            + estoque.getNome()
                            );
                }
            }


            // ====================================================
            // CRIAR PRATO
            // ====================================================

            PratoDoDia prato =
                    new PratoDoDia();

            prato.setNomePrato(
                    nomePrato
            );

            prato.setDataPrato(
                    dataPrato
            );


            List<PratoIngrediente>
                    listaIngredientes =
                    new ArrayList<>();


            // ====================================================
            // PROCESSAR INGREDIENTES
            // ====================================================

            for (Map<String, Object> ingrediente :
                    ingredientesJson) {

                Long estoqueId =
                        Long.parseLong(
                                ingrediente
                                        .get("estoqueId")
                                        .toString()
                        );


                Double quantidade =
                        Double.parseDouble(
                                ingrediente
                                        .get("quantidadeGasta")
                                        .toString()
                        );


                Estoque estoque =
                        estoqueRepository
                                .findById(estoqueId)
                                .orElseThrow();


                PratoIngrediente pi =
                        new PratoIngrediente();


                // Campos reais de PratoIngrediente
                pi.setNome(
                        estoque.getNome()
                );

                pi.setUnidade(
                        estoque.getUnidade()
                );

                pi.setQuantidade(
                        quantidade
                );


                listaIngredientes.add(pi);


                // Baixar do estoque
                abaterLotesInsumo(
                        estoque.getNome(),
                        quantidade
                );
            }


            prato.setIngredientes(
                    listaIngredientes
            );


            PratoDoDia salvo =
                    pratoDoDiaRepository.save(
                            prato
                    );


            return ResponseEntity.ok(
                    salvo
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao salvar prato do dia: "
                                    + e.getMessage()
                    );
        }
    }


    // ------------------------------------------------------------
    // EXCLUIR PRATO DO DIA
    // ------------------------------------------------------------

    @DeleteMapping("/prato-do-dia/{id}")
    public ResponseEntity<?> deletarCardapio(
            @PathVariable Long id) {

        if (!pratoDoDiaRepository
                .existsById(id)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        pratoDoDiaRepository
                .deleteById(id);


        return ResponseEntity.ok().build();
    }


    // ============================================================
    // ATENDIMENTO / SCANNER
    // ============================================================

    @PostMapping("/atendimento/registrar")
    public ResponseEntity<?> registrarAtendimento(
            @RequestParam String identificador) {

        try {

            if (identificador == null ||
                    identificador.trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body(
                                "A matrícula é obrigatória."
                        );
            }


            Aluno aluno =
                    alunoRepository
                            .findByMatricula(
                                    identificador.trim()
                            )
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


            // Campos reais de RegistroAtendimento
            registro.setMatricula(
                    aluno.getMatricula()
            );

            registro.setDataHoraAtendimento(
                    LocalDateTime.now()
            );


            RegistroAtendimento salvo =
                    registroAtendimentoRepository
                            .save(registro);


            return ResponseEntity.ok(
                    salvo
            );

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

    /**
     * Retorna texto de um campo do JSON.
     */
    private String obterTexto(
            Map<String, Object> payload,
            String chave) {

        if (payload == null) {
            return null;
        }

        Object valor =
                payload.get(chave);

        if (valor == null) {
            return null;
        }

        return valor.toString().trim();
    }


    /**
     * Abate quantidade dos lotes de um alimento.
     *
     * Os lotes não possuem relacionamento com Estoque.
     * A ligação é feita pelo nome do alimento.
     *
     * Os lotes com validade mais próxima são consumidos primeiro.
     */
    private boolean abaterLotesInsumo(
            String nome,
            Double quantidade) {

        if (nome == null ||
                nome.isBlank() ||
                quantidade == null ||
                quantidade <= 0) {

            return false;
        }


        List<EstoqueLote> lotes =
                estoqueLoteRepository
                        .findAll()
                        .stream()
                        .filter(lote ->
                                lote.getNome() != null &&
                                lote.getNome()
                                        .trim()
                                        .equalsIgnoreCase(nome) &&
                                lote.getQuantidade() != null &&
                                lote.getQuantidade() > 0
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


        double disponivelTotal =
                lotes.stream()
                        .map(EstoqueLote::getQuantidade)
                        .filter(q -> q != null)
                        .mapToDouble(Double::doubleValue)
                        .sum();


        if (disponivelTotal < quantidade) {
            return false;
        }


        double restante =
                quantidade;


        for (EstoqueLote lote : lotes) {

            if (restante <= 0) {
                break;
            }


            double quantidadeLote =
                    lote.getQuantidade();


            if (quantidadeLote <= restante) {

                lote.setQuantidade(
                        0.0
                );

                restante -=
                        quantidadeLote;

            } else {

                lote.setQuantidade(
                        quantidadeLote -
                                restante
                );

                restante = 0;
            }


            estoqueLoteRepository.save(
                    lote
            );
        }


        recalcularQuantidadeEstoque(
                nome
        );


        return true;
    }


    /**
     * Recalcula a quantidade principal do Estoque
     * com base na soma dos lotes.
     */
    private void recalcularQuantidadeEstoque(
            String nome) {

        if (nome == null ||
                nome.isBlank()) {

            return;
        }


        Estoque estoque =
                estoqueRepository
                        .findAll()
                        .stream()
                        .filter(e ->
                                e.getNome() != null &&
                                e.getNome()
                                        .trim()
                                        .equalsIgnoreCase(nome)
                        )
                        .findFirst()
                        .orElse(null);


        if (estoque == null) {
            return;
        }


        double total =
                estoqueLoteRepository
                        .findAll()
                        .stream()
                        .filter(lote ->
                                lote.getNome() != null &&
                                lote.getNome()
                                        .trim()
                                        .equalsIgnoreCase(nome)
                        )
                        .map(EstoqueLote::getQuantidade)
                        .filter(q -> q != null)
                        .mapToDouble(Double::doubleValue)
                        .sum();


        estoque.setQuantidade(
                total
        );


        estoqueRepository.save(
                estoque
        );
    }
}
