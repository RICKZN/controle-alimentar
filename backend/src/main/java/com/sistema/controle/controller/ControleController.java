package com.sistema.controle.controller;

import com.sistema.controle.model.Aluno;
import com.sistema.controle.model.Estoque;
import com.sistema.controle.model.EstoqueLote;
import com.sistema.controle.model.PratoDoDia;
import com.sistema.controle.model.PratoIngrediente;
import com.sistema.controle.model.RegistroAtendimento;
import com.sistema.controle.model.RegistroConsumo;

import com.sistema.controle.repository.AlunoRepository;
import com.sistema.controle.repository.EstoqueLoteRepository;
import com.sistema.controle.repository.EstoqueRepository;
import com.sistema.controle.repository.PratoDoDiaRepository;
import com.sistema.controle.repository.RegistroAtendimentoRepository;
import com.sistema.controle.repository.RegistroConsumoRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ControleController {

    @Autowired
    private EstoqueRepository estoqueRepo;

    @Autowired
    private EstoqueLoteRepository loteRepo;

    @Autowired
    private PratoDoDiaRepository pratoRepo;

    @Autowired
    private RegistroAtendimentoRepository registroRepo;

    @Autowired
    private AlunoRepository alunoRepo;

    @Autowired
    private RegistroConsumoRepository consumoRepo;


    // ============================================================
    // INICIALIZAÇÃO
    // ============================================================

    @PostConstruct
    public void initData() {

        if (loteRepo.count() == 0 && estoqueRepo.count() == 0) {

            adicionarItemInicial("Arroz", "kg", 50.0);
            adicionarItemInicial("Feijão", "kg", 30.0);
            adicionarItemInicial("Carnes Vermelhas", "kg", 20.0);
            adicionarItemInicial("Frango", "kg", 25.0);
            adicionarItemInicial("Óleo", "litros", 10.0);
        }

        if (alunoRepo.count() == 0) {

            cadastrarAlunoInicial("2023001", "João Silva");
            cadastrarAlunoInicial("2023002", "Maria Oliveira");
            cadastrarAlunoInicial("2023003", "Pedro Santos");
        }
    }


    private void cadastrarAlunoInicial(
            String matricula,
            String nome) {

        Aluno aluno = new Aluno();

        aluno.setMatricula(matricula);
        aluno.setNome(nome);

        alunoRepo.save(aluno);
    }


    private void adicionarItemInicial(
            String nome,
            String unidade,
            Double quantidade) {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );

        EstoqueLote lote = new EstoqueLote();

        lote.setNome(nome);
        lote.setUnidade(unidade);
        lote.setQuantidade(quantidade);
        lote.setDataCompra(hoje);
        lote.setDataValidade(hoje.plusMonths(6));
        lote.setUsuarioResponsavel("Sistema");
        lote.setDataCadastro(hoje);

        loteRepo.save(lote);

        atualizarEstoqueConsolidado(nome);
    }


    // ============================================================
    // ESTOQUE
    // ============================================================

    private Estoque atualizarEstoqueConsolidado(
            String nome) {

        List<EstoqueLote> lotes =
                loteRepo
                        .findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(
                                nome
                        );

        double total =
                lotes.stream()
                        .mapToDouble(lote ->
                                lote.getQuantidade() == null
                                        ? 0.0
                                        : lote.getQuantidade()
                        )
                        .sum();


        Estoque item =
                estoqueRepo.findAll()
                        .stream()
                        .filter(e ->
                                e.getNome() != null &&
                                e.getNome()
                                        .equalsIgnoreCase(nome)
                        )
                        .findFirst()
                        .orElseGet(Estoque::new);


        item.setNome(nome);

        if (!lotes.isEmpty()) {
            item.setUnidade(
                    lotes.get(0).getUnidade()
            );
        }

        item.setQuantidade(total);

        return estoqueRepo.save(item);
    }


    @GetMapping("/estoque")
    public List<Estoque> obterEstoque() {

        return estoqueRepo.findAll();
    }


    @PostMapping("/estoque")
    public ResponseEntity<?> adicionarNovoItem(
            @RequestBody EstoqueLote novoLote) {

        try {

            if (novoLote == null) {
                return ResponseEntity.badRequest()
                        .body("Lote inválido.");
            }


            if (novoLote.getNome() == null ||
                    novoLote.getNome().trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("O nome do alimento é obrigatório.");
            }


            if (novoLote.getQuantidade() == null ||
                    novoLote.getQuantidade() <= 0) {

                return ResponseEntity.badRequest()
                        .body("A quantidade deve ser maior que zero.");
            }


            LocalDate hoje =
                    LocalDate.now(
                            ZoneId.of("America/Sao_Paulo")
                    );


            if (novoLote.getDataCadastro() == null) {
                novoLote.setDataCadastro(hoje);
            }


            if (novoLote.getDataCompra() == null) {
                novoLote.setDataCompra(hoje);
            }


            if (novoLote.getUsuarioResponsavel() == null ||
                    novoLote.getUsuarioResponsavel().isBlank()) {

                novoLote.setUsuarioResponsavel("Cozinha");
            }


            loteRepo.save(novoLote);


            return ResponseEntity.ok(
                    atualizarEstoqueConsolidado(
                            novoLote.getNome()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao adicionar item: "
                                    + e.getMessage()
                    );
        }
    }


    @GetMapping("/estoque/{nome}/lotes")
    public List<EstoqueLote> lotesPorAlimento(
            @PathVariable String nome) {

        return loteRepo
                .findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(
                        nome
                );
    }


    @GetMapping("/estoque/historico")
    public List<EstoqueLote> historicoEntradas(
            @RequestParam(required = false) String alimento,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(required = false) LocalDate validade,
            @RequestParam(required = false) Long lote) {


        if (lote != null) {

            return loteRepo
                    .findById(lote)
                    .map(List::of)
                    .orElse(List.of());
        }


        return loteRepo.findAll()
                .stream()

                .filter(l ->
                        alimento == null ||
                        l.getNome() == null ||
                        l.getNome()
                                .equalsIgnoreCase(alimento)
                )

                .filter(l ->
                        inicio == null ||
                        l.getDataCompra() == null ||
                        !l.getDataCompra()
                                .isBefore(inicio)
                )

                .filter(l ->
                        fim == null ||
                        l.getDataCompra() == null ||
                        !l.getDataCompra()
                                .isAfter(fim)
                )

                .filter(l ->
                        validade == null ||
                        l.getDataValidade() == null ||
                        l.getDataValidade()
                                .equals(validade)
                )

                .toList();
    }


    @PostMapping("/estoque/{id}/ajustar")
    public ResponseEntity<?> ajustarEstoque(
            @PathVariable Long id,
            @RequestParam Double variacao) {

        try {

            if (variacao == null || variacao == 0) {

                return ResponseEntity.badRequest()
                        .body(
                                "A variação deve ser diferente de zero."
                        );
            }


            Optional<Estoque> itemOpt =
                    estoqueRepo.findById(id);


            if (itemOpt.isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("Item não encontrado.");
            }


            Estoque item =
                    itemOpt.get();


            if (variacao < 0) {

                double quantidade =
                        Math.abs(variacao);


                if (item.getQuantidade() == null ||
                        item.getQuantidade() < quantidade) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Estoque insuficiente."
                            );
                }


                boolean sucesso =
                        consumirPorFefo(
                                item.getNome(),
                                quantidade
                        );


                if (!sucesso) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Não foi possível realizar a baixa."
                            );
                }

            } else {

                LocalDate hoje =
                        LocalDate.now(
                                ZoneId.of("America/Sao_Paulo")
                        );


                EstoqueLote lote =
                        new EstoqueLote();

                lote.setNome(
                        item.getNome()
                );

                lote.setUnidade(
                        item.getUnidade()
                );

                lote.setQuantidade(
                        variacao
                );

                lote.setDataCompra(
                        hoje
                );

                lote.setDataValidade(
                        hoje.plusMonths(6)
                );

                lote.setUsuarioResponsavel(
                        "Ajuste manual"
                );

                lote.setDataCadastro(
                        hoje
                );


                loteRepo.save(lote);

                atualizarEstoqueConsolidado(
                        item.getNome()
                );
            }


            return ResponseEntity.ok(
                    atualizarEstoqueConsolidado(
                            item.getNome()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao ajustar estoque: "
                                    + e.getMessage()
                    );
        }
    }


    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirItemEstoque(
            @PathVariable Long id) {

        Optional<Estoque> itemOpt =
                estoqueRepo.findById(id);


        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        String nome =
                itemOpt.get().getNome();


        loteRepo
                .findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(
                        nome
                )
                .forEach(loteRepo::delete);


        estoqueRepo.deleteById(id);


        return ResponseEntity.ok().build();
    }


    // ============================================================
    // CONSUMO POR FEFO
    // ============================================================

    private void registrarConsumo(
            String nome,
            Double quantidade) {

        if (quantidade == null ||
                quantidade <= 0) {

            return;
        }


        RegistroConsumo reg =
                new RegistroConsumo();

        reg.setNomeItem(nome);

        reg.setData(
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                )
        );

        reg.setQuantidadeGasta(
                quantidade
        );


        consumoRepo.save(reg);
    }


    private boolean consumirPorFefo(
            String nome,
            Double quantidade) {

        if (nome == null ||
                nome.isBlank() ||
                quantidade == null ||
                quantidade <= 0) {

            return false;
        }


        List<EstoqueLote> lotes =
                loteRepo
                        .findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(
                                nome
                        );


        double totalDisponivel =
                lotes.stream()
                        .mapToDouble(lote ->
                                lote.getQuantidade() == null
                                        ? 0.0
                                        : lote.getQuantidade()
                        )
                        .sum();


        if (totalDisponivel < quantidade) {
            return false;
        }


        double restante =
                quantidade;


        double consumido =
                0.0;


        for (EstoqueLote lote : lotes) {

            if (restante <= 0) {
                break;
            }


            double disponivel =
                    lote.getQuantidade() == null
                            ? 0.0
                            : lote.getQuantidade();


            double consumo =
                    Math.min(
                            disponivel,
                            restante
                    );


            lote.setQuantidade(
                    disponivel - consumo
            );


            loteRepo.save(lote);


            restante -= consumo;

            consumido += consumo;
        }


        registrarConsumo(
                nome,
                consumido
        );


        atualizarEstoqueConsolidado(
                nome
        );


        return consumido >= quantidade;
    }


    // ============================================================
    // ALERTAS
    // ============================================================

    @GetMapping("/alertas")
    public Map<String, Object> alertas(
            @RequestParam(defaultValue = "2")
            Double limiteBaixo) {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );


        Map<String, Object> resposta =
                new HashMap<>();


        resposta.put(
                "estoqueBaixo",
                estoqueRepo.findAll()
                        .stream()
                        .filter(e ->
                                e.getQuantidade() != null &&
                                e.getQuantidade() <= limiteBaixo
                        )
                        .toList()
        );


        resposta.put(
                "vencimentos",
                loteRepo
                        .findByQuantidadeGreaterThanOrderByDataValidadeAscDataCompraAscIdAsc(
                                0.0
                        )
                        .stream()
                        .filter(l ->
                                l.getDataValidade() != null &&
                                !l.getDataValidade()
                                        .isBefore(hoje) &&
                                !l.getDataValidade()
                                        .isAfter(
                                                hoje.plusDays(30)
                                        )
                        )
                        .toList()
        );


        resposta.put(
                "pratoDoDia",
                pratoRepo
                        .findTopByDataOrderByIdDesc(hoje)
                        .orElse(null)
        );


        return resposta;
    }


    // ============================================================
    // PRATO DO DIA
    // ============================================================

    @PostMapping("/prato-dia")
    public ResponseEntity<?> salvarPratoDia(
            @RequestBody PratoDoDia prato) {

        try {

            if (prato == null) {

                return ResponseEntity.badRequest()
                        .body("Prato inválido.");
            }


            LocalDate hoje =
                    LocalDate.now(
                            ZoneId.of("America/Sao_Paulo")
                    );


            if (prato.getData() == null) {

                prato.setData(hoje);
            }


            if (prato.getRefeicoesLiberadas() == null) {

                prato.setRefeicoesLiberadas(0);
            }


            if (prato.getIngredientes() == null) {

                prato.setIngredientes(
                        new java.util.ArrayList<>()
                );
            }


            // Verifica estoque antes de consumir
            for (PratoIngrediente ingrediente :
                    prato.getIngredientes()) {

                if (ingrediente == null ||
                        ingrediente.getNome() == null ||
                        ingrediente.getNome().isBlank()) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Ingrediente inválido."
                            );
                }


                if (ingrediente.getQuantidade() == null ||
                        ingrediente.getQuantidade() <= 0) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Quantidade inválida para o ingrediente: "
                                            + ingrediente.getNome()
                            );
                }


                Optional<Estoque> estoque =
                        estoqueRepo.findAll()
                                .stream()
                                .filter(e ->
                                        e.getNome() != null &&
                                        e.getNome()
                                                .equalsIgnoreCase(
                                                        ingrediente.getNome()
                                                )
                                )
                                .findFirst();


                if (estoque.isEmpty() ||
                        estoque.get().getQuantidade() == null ||
                        estoque.get().getQuantidade()
                                < ingrediente.getQuantidade()) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Estoque insuficiente para o item: "
                                            + ingrediente.getNome()
                            );
                }
            }


            // Consome os ingredientes
            for (PratoIngrediente ingrediente :
                    prato.getIngredientes()) {

                if (!consumirPorFefo(
                        ingrediente.getNome(),
                        ingrediente.getQuantidade()
                )) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Não foi possível consumir o ingrediente: "
                                            + ingrediente.getNome()
                            );
                }
            }


            return ResponseEntity.ok(
                    pratoRepo.save(prato)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao salvar prato: "
                                    + e.getMessage()
                    );
        }
    }


    @GetMapping("/prato-dia/hoje")
    public ResponseEntity<?> pratoDiaHoje() {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );


        return ResponseEntity.ok(
                pratoRepo
                        .findTopByDataOrderByIdDesc(hoje)
                        .orElse(null)
        );
    }


    // ============================================================
    // ALUNOS
    // ============================================================

    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() {

        return alunoRepo.findAll();
    }


    @PostMapping("/alunos")
    public ResponseEntity<?> cadastrarAluno(
            @RequestBody Aluno novoAluno) {

        try {

            if (novoAluno == null) {

                return ResponseEntity.badRequest()
                        .body("Aluno inválido.");
            }


            if (novoAluno.getMatricula() == null ||
                    novoAluno.getMatricula()
                            .trim()
                            .isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("A matrícula é obrigatória.");
            }


            if (alunoRepo
                    .findByMatricula(
                            novoAluno.getMatricula()
                    )
                    .isPresent()) {

                Map<String, String> response =
                        new HashMap<>();

                response.put(
                        "error",
                        "Matrícula já cadastrada!"
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }


            return ResponseEntity.ok(
                    alunoRepo.save(novoAluno)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(
                            "Erro ao cadastrar aluno: "
                                    + e.getMessage()
                    );
        }
    }


    @PutMapping("/alunos/{id}")
    public ResponseEntity<?> editarAluno(
            @PathVariable Long id,
            @RequestBody Aluno dadosNovos) {

        Optional<Aluno> alunoOpt =
                alunoRepo.findById(id);


        if (alunoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }


        Aluno aluno =
                alunoOpt.get();


        aluno.setNome(
                dadosNovos.getNome()
        );

        aluno.setMatricula(
                dadosNovos.getMatricula()
        );

        aluno.setCurso(
                dadosNovos.getCurso()
        );

        aluno.setModalidade(
                dadosNovos.getModalidade()
        );

        aluno.setTurma(
                dadosNovos.getTurma()
        );

        aluno.setTurno(
                dadosNovos.getTurno()
        );


        return ResponseEntity.ok(
                alunoRepo.save(aluno)
        );
    }


    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> excluirAluno(
            @PathVariable Long id) {

        if (!alunoRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }


        alunoRepo.deleteById(id);


        return ResponseEntity.ok().build();
    }


    // ============================================================
    // VALIDAÇÃO DA FICHA / REFEIÇÃO
    // ============================================================

    @PostMapping("/validar")
    public ResponseEntity<?> validarFicha(
            @RequestParam String matricula) {

        Map<String, Object> response =
                new HashMap<>();


        if (matricula == null ||
                matricula.trim().isEmpty()) {

            response.put(
                    "error",
                    "A matrícula é obrigatória."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        LocalDateTime agora =
                LocalDateTime.now();


        LocalDateTime limite6Horas =
                agora.minusHours(6);


        Optional<Aluno> alunoOpt =
                alunoRepo.findByMatricula(
                        matricula.trim()
                );


        if (alunoOpt.isEmpty()) {

            response.put(
                    "error",
                    "Estudante não cadastrado no sistema."
            );

            return ResponseEntity
                    .status(404)
                    .body(response);
        }


        Aluno aluno =
                alunoOpt.get();


        if (aluno.getUltimaRefeicao() != null &&
                aluno.getUltimaRefeicao()
                        .isAfter(limite6Horas)) {


            LocalDateTime proximaRefeicao =
                    aluno.getUltimaRefeicao()
                            .plusHours(6);


            Duration restante =
                    Duration.between(
                            agora,
                            proximaRefeicao
                    );


            long minutosFaltando =
                    Math.max(
                            0,
                            restante.toMinutes()
                    );


            long segundosFaltando =
                    Math.max(
                            0,
                            restante.toSeconds()
                    );


            response.put(
                    "error",
                    "Já recebeu refeição recentemente."
            );


            response.put(
                    "horaUltimaRefeicao",
                    aluno.getUltimaRefeicao()
                            .toString()
            );


            response.put(
                    "proximaRefeicao",
                    proximaRefeicao.toString()
            );


            response.put(
                    "minutosFaltando",
                    minutosFaltando
            );


            response.put(
                    "segundosFaltando",
                    segundosFaltando
            );


            response.put(
                    "espera",
                    String.format(
                            "Aguarde mais %d horas e %d minutos.",
                            minutosFaltando / 60,
                            minutosFaltando % 60
                    )
            );


            return ResponseEntity
                    .status(429)
                    .body(response);
        }


        RegistroAtendimento registro =
                new RegistroAtendimento();


        registro.setMatricula(
                aluno.getMatricula()
        );


        registro.setDataHoraAtendimento(
                agora
        );


        registroRepo.save(
                registro
        );


        aluno.setUltimaRefeicao(
                agora
        );


        alunoRepo.save(
                aluno
        );


        response.put(
                "message",
                "Refeição liberada para "
                        + aluno.getNome()
        );


        response.put(
                "matricula",
                aluno.getMatricula()
        );


        response.put(
                "dataHora",
                agora.toString()
        );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // RELATÓRIOS DE CONSUMO
    // ============================================================

    @GetMapping("/consumo/diario")
    public ResponseEntity<?> consumoDiario() {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );


        return ResponseEntity.ok(
                consumoRepo.findByDataBetween(
                        hoje,
                        hoje
                )
        );
    }


    @GetMapping("/consumo/semanal")
    public ResponseEntity<?> consumoSemanal() {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );


        LocalDate inicio =
                hoje.minusDays(
                        hoje.getDayOfWeek().getValue() - 1
                );


        return ResponseEntity.ok(
                consumoRepo.findByDataBetween(
                        inicio,
                        hoje
                )
        );
    }


    @GetMapping("/consumo/mensal")
    public ResponseEntity<?> consumoMensal() {

        LocalDate hoje =
                LocalDate.now(
                        ZoneId.of("America/Sao_Paulo")
                );


        return ResponseEntity.ok(
                consumoRepo.findByDataBetween(
                        hoje.withDayOfMonth(1),
                        hoje
                )
        );
    }
}
