package com.sistema.controle.controller;

import com.sistema.controle.model.Aluno;
import com.sistema.controle.model.Estoque;
import com.sistema.controle.model.EstoqueLote;
import com.sistema.controle.model.PratoDoDia;
import com.sistema.controle.model.PratoIngrediente;
import com.sistema.controle.model.RegistroAtendimento;
import com.sistema.controle.repository.AlunoRepository;
import com.sistema.controle.repository.EstoqueRepository;
import com.sistema.controle.repository.EstoqueLoteRepository;
import com.sistema.controle.repository.PratoDoDiaRepository;
import com.sistema.controle.repository.RegistroAtendimentoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.time.LocalDate;

import java.time.ZoneId;

import com.sistema.controle.model.RegistroConsumo;
import com.sistema.controle.repository.RegistroConsumoRepository;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Habilita CORS para o Vue.js
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

    @PostConstruct
    public void initData() {
        // Inicializa estoque
        if (loteRepo.count() == 0 && estoqueRepo.count() == 0) {
            adicionarItemInicial("Arroz", "kg", 50.0);
            adicionarItemInicial("Feijão", "kg", 30.0);
            adicionarItemInicial("Carnes Vermelhas", "kg", 20.0);
            adicionarItemInicial("Frango", "kg", 25.0);
            adicionarItemInicial("Óleo", "litros", 10.0);
        }

        // Inicializa alguns alunos de teste
        if (alunoRepo.count() == 0) {
            cadastrarAlunoInicial("2023001", "João Silva");
            cadastrarAlunoInicial("2023002", "Maria Oliveira");
            cadastrarAlunoInicial("2023003", "Pedro Santos");
        }
    }

    private void cadastrarAlunoInicial(String matricula, String nome) {
        Aluno a = new Aluno();
        a.setMatricula(matricula);
        a.setNome(nome);
        alunoRepo.save(a);
    }

    private void adicionarItemInicial(String nome, String unidade, Double quantidade) {
        EstoqueLote lote = new EstoqueLote();
        lote.setNome(nome);
        lote.setUnidade(unidade);
        lote.setQuantidade(quantidade);
        lote.setDataCompra(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        lote.setDataValidade(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusMonths(6));
        lote.setUsuarioResponsavel("Sistema");
        lote.setDataCadastro(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        loteRepo.save(lote);
        atualizarEstoqueConsolidado(nome);
    }

    private String normalizar(String nome) {
        return nome == null ? null : nome.trim();
    }

    private Estoque atualizarEstoqueConsolidado(String nomeOriginal) {
        String nome = normalizar(nomeOriginal);
        List<EstoqueLote> lotes = loteRepo.findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(nome);
        double total = lotes.stream().mapToDouble(l -> l.getQuantidade() == null ? 0 : l.getQuantidade()).sum();
        Estoque item = estoqueRepo.findAll().stream()
            .filter(e -> e.getNome() != null && e.getNome().trim().equalsIgnoreCase(nome))
            .findFirst().orElseGet(Estoque::new);
        item.setNome(nome);
        item.setUnidade(lotes.isEmpty() ? item.getUnidade() : lotes.get(0).getUnidade());
        item.setQuantidade(total);
        return estoqueRepo.save(item);
    }

    private void registrarConsumo(String nome, Double quantidade) {
        RegistroConsumo reg = new RegistroConsumo();
        reg.setNomeItem(nome);
        reg.setData(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        reg.setQuantidadeGasta(quantidade);
        consumoRepo.save(reg);
    }

    private void consumirPorFefo(String nome, Double quantidade) {
        double restante = quantidade == null ? 0 : quantidade;
        for (EstoqueLote lote : loteRepo.findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(nome)) {
            if (restante <= 0) break;
            double disponivel = lote.getQuantidade() == null ? 0 : lote.getQuantidade();
            double consumo = Math.min(disponivel, restante);
            lote.setQuantidade(disponivel - consumo);
            loteRepo.save(lote);
            restante -= consumo;
        }
        registrarConsumo(nome, quantidade - Math.max(restante, 0));
        atualizarEstoqueConsolidado(nome);
    }

    @GetMapping("/estoque")
    public List<Estoque> obterEstoque() {
        return estoqueRepo.findAll();
    }

   @PostMapping("/estoque/{id}/ajustar")
public ResponseEntity<?> ajustarEstoque(@PathVariable Long id, @RequestParam Double variacao) {
    Optional<Estoque> itemOpt = estoqueRepo.findById(id);
    if (itemOpt.isEmpty()) return ResponseEntity.badRequest().body("Item não encontrado.");
    Estoque item = itemOpt.get();
    if (variacao < 0) consumirPorFefo(item.getNome(), Math.abs(variacao));
    else {
        EstoqueLote lote = new EstoqueLote();
        lote.setNome(item.getNome());
        lote.setUnidade(item.getUnidade());
        lote.setQuantidade(variacao);
        lote.setDataCompra(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        lote.setDataValidade(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusMonths(6));
        lote.setUsuarioResponsavel("Ajuste manual");
        lote.setDataCadastro(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        loteRepo.save(lote);
        atualizarEstoqueConsolidado(item.getNome());
    }
    return ResponseEntity.ok(atualizarEstoqueConsolidado(item.getNome()));
}

    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirItemEstoque(@PathVariable Long id) {
        estoqueRepo.findById(id).ifPresent(item -> loteRepo.findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(item.getNome()).forEach(loteRepo::delete));
        estoqueRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/estoque")
    public ResponseEntity<?> adicionarNovoItem(@RequestBody EstoqueLote novoLote) {
        String nome = normalizar(novoLote.getNome());
        if (nome == null || nome.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Informe o nome do alimento."));
        }
        boolean jaExiste = estoqueRepo.findAll().stream()
            .anyMatch(e -> e.getNome() != null && e.getNome().trim().equalsIgnoreCase(nome));
        if (jaExiste) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "\"" + nome + "\" já está cadastrado. Use o botão \"+ Lote\" no card do alimento para adicionar mais estoque."
            ));
        }
        novoLote.setNome(nome);
        if (novoLote.getDataCadastro() == null) novoLote.setDataCadastro(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        if (novoLote.getUsuarioResponsavel() == null || novoLote.getUsuarioResponsavel().isBlank()) novoLote.setUsuarioResponsavel("Cozinha");
        loteRepo.save(novoLote);
        return ResponseEntity.ok(atualizarEstoqueConsolidado(nome));
    }

    @PostMapping("/estoque/{nome}/lotes")
    public ResponseEntity<?> adicionarLoteExistente(@PathVariable String nome, @RequestBody EstoqueLote novoLote) {
        String nomeNormalizado = normalizar(nome);
        boolean existe = estoqueRepo.findAll().stream()
            .anyMatch(e -> e.getNome() != null && e.getNome().trim().equalsIgnoreCase(nomeNormalizado));
        if (!existe) {
            return ResponseEntity.badRequest().body(Map.of("error", "Alimento não encontrado. Cadastre-o primeiro."));
        }
        novoLote.setNome(nomeNormalizado);
        if (novoLote.getDataCadastro() == null) novoLote.setDataCadastro(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        if (novoLote.getUsuarioResponsavel() == null || novoLote.getUsuarioResponsavel().isBlank()) novoLote.setUsuarioResponsavel("Cozinha");
        loteRepo.save(novoLote);
        return ResponseEntity.ok(atualizarEstoqueConsolidado(nomeNormalizado));
    }

    @PostMapping("/estoque/reconstruir")
    public ResponseEntity<?> reconstruirEstoque() {
        // Corrige duplicados já existentes: normaliza o nome de todos os lotes e recalcula o estoque do zero
        List<EstoqueLote> todosLotes = loteRepo.findAll();
        for (EstoqueLote lote : todosLotes) {
            String nomeLimpo = normalizar(lote.getNome());
            if (!nomeLimpo.equals(lote.getNome())) {
                lote.setNome(nomeLimpo);
                loteRepo.save(lote);
            }
        }
        estoqueRepo.deleteAll();
        Set<String> nomesUnicos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (EstoqueLote lote : loteRepo.findAll()) nomesUnicos.add(lote.getNome());
        for (String nome : nomesUnicos) atualizarEstoqueConsolidado(nome);
        return ResponseEntity.ok(estoqueRepo.findAll());
    }

    @GetMapping("/estoque/{nome}/lotes")
    public List<EstoqueLote> lotesPorAlimento(@PathVariable String nome) {
        return loteRepo.findByNomeIgnoreCaseOrderByDataValidadeAscDataCompraAscIdAsc(nome);
    }

    @GetMapping("/estoque/historico")
    public List<EstoqueLote> historicoEntradas(@RequestParam(required = false) String alimento,
                                               @RequestParam(required = false) LocalDate inicio,
                                               @RequestParam(required = false) LocalDate fim,
                                               @RequestParam(required = false) LocalDate validade,
                                               @RequestParam(required = false) Long lote) {
        if (lote != null) return loteRepo.findById(lote).map(List::of).orElse(List.of());
        return loteRepo.findAll().stream()
            .filter(l -> alimento == null || l.getNome().equalsIgnoreCase(alimento))
            .filter(l -> inicio == null || !l.getDataCompra().isBefore(inicio))
            .filter(l -> fim == null || !l.getDataCompra().isAfter(fim))
            .filter(l -> validade == null || l.getDataValidade().equals(validade))
            .toList();
    }

    @GetMapping("/alertas")
    public Map<String,Object> alertas(@RequestParam(defaultValue = "2") Double limiteBaixo) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Map<String,Object> map = new HashMap<>();
        map.put("estoqueBaixo", estoqueRepo.findAll().stream().filter(e -> e.getQuantidade() <= limiteBaixo).toList());
        map.put("vencimentos", loteRepo.findByQuantidadeGreaterThanOrderByDataValidadeAscDataCompraAscIdAsc(0.0).stream().filter(l -> !l.getDataValidade().isBefore(hoje) && !l.getDataValidade().isAfter(hoje.plusDays(30))).toList());
        map.put("pratoDoDia", pratoRepo.findTopByDataOrderByIdDesc(hoje).orElse(null));

        // Resumo diário real: conta refeições liberadas hoje a partir dos registros de validação
        long refeicoesLiberadasHoje = registroRepo.findAll().stream()
                .filter(r -> r.getDataHoraAtendimento() != null && r.getDataHoraAtendimento().toLocalDate().equals(hoje))
                .count();
        map.put("refeicoesLiberadasHoje", refeicoesLiberadasHoje);

        // Resumo diário real: soma o consumo registrado hoje (prato do dia + ajustes manuais de estoque)
        double alimentosUtilizadosHoje = consumoRepo.findByDataBetween(hoje, hoje).stream()
                .mapToDouble(RegistroConsumo::getQuantidadeGasta)
                .sum();
        map.put("alimentosUtilizadosHoje", alimentosUtilizadosHoje);

        return map;
    }

    @PostMapping("/prato-dia")
    public ResponseEntity<?> salvarPratoDia(@RequestBody PratoDoDia prato) {
        if (prato.getData() == null) prato.setData(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        if (prato.getRefeicoesLiberadas() == null) prato.setRefeicoesLiberadas(0);
        for (PratoIngrediente ing : prato.getIngredientes()) consumirPorFefo(ing.getNome(), ing.getQuantidade());
        return ResponseEntity.ok(pratoRepo.save(prato));
    }

    @GetMapping("/prato-dia/hoje")
    public ResponseEntity<?> pratoDiaHoje() {
        return ResponseEntity.ok(pratoRepo.findTopByDataOrderByIdDesc(LocalDate.now(ZoneId.of("America/Sao_Paulo"))).orElse(null));
    }

    @GetMapping("/prato-dia")
    public List<PratoDoDia> historicoPratos() {
        return pratoRepo.findAllByOrderByDataDescIdDesc();
    }

    @DeleteMapping("/prato-dia/{id}")
    public ResponseEntity<?> excluirPratoDoDia(@PathVariable Long id) {
        // Exclusão disponível apenas na aba Histórico de Pratos do frontend.
        // Não afeta o estoque: os alimentos já foram baixados no momento do registro.
        pratoRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() {
        return alunoRepo.findAll();
    }

    @PostMapping("/alunos")
    public ResponseEntity<?> cadastrarAluno(@RequestBody Aluno novoAluno) {
        if (alunoRepo.findByMatricula(novoAluno.getMatricula()).isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Matrícula já cadastrada!");
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(alunoRepo.save(novoAluno));
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> excluirAluno(@PathVariable Long id) {
        alunoRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
@PutMapping("/alunos/{id}")
public ResponseEntity<?> editarAluno(@PathVariable Long id, @RequestBody Aluno dadosNovos) {
    Optional<Aluno> alunoOpt = alunoRepo.findById(id);

    if (alunoOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    Aluno aluno = alunoOpt.get();

    aluno.setNome(dadosNovos.getNome());
    aluno.setMatricula(dadosNovos.getMatricula());
    aluno.setCurso(dadosNovos.getCurso());
    aluno.setModalidade(dadosNovos.getModalidade());
    aluno.setTurma(dadosNovos.getTurma());
    aluno.setTurno(dadosNovos.getTurno());

    return ResponseEntity.ok(alunoRepo.save(aluno));
}
  @PostMapping("/validar")
public ResponseEntity<?> validarFicha(@RequestParam String matricula) {
    Map<String, Object> response = new HashMap<>();
    
    LocalDateTime agora = LocalDateTime.now();
    LocalDateTime limite6Horas = agora.minusHours(6); 

    Optional<Aluno> alunoOpt = alunoRepo.findByMatricula(matricula);
    if (alunoOpt.isEmpty()) {
        response.put("error", "Estudante não cadastrado no sistema.");
        return ResponseEntity.status(404).body(response);
    }

    Aluno aluno = alunoOpt.get();

    if (aluno.getUltimaRefeicao() != null && aluno.getUltimaRefeicao().isAfter(limite6Horas)) {
        Duration restante = Duration.between(agora, aluno.getUltimaRefeicao().plusHours(6));
        long minutosFaltando = restante.toMinutes();
        long segundosFaltando = restante.toSeconds();

        response.put("error", "Já recebeu refeição recentemente.");
        response.put("horaUltimaRefeicao", aluno.getUltimaRefeicao().toString());
        response.put("proximaRefeicao", aluno.getUltimaRefeicao().plusHours(6).toString());
        response.put("minutosFaltando", minutosFaltando);
        response.put("segundosFaltando", segundosFaltando);
        response.put("espera", String.format("Aguarde mais %d horas e %d minutos.",
                     minutosFaltando / 60, minutosFaltando % 60));
        return ResponseEntity.status(429).body(response); // 
    }

    // só executa se liberado
// só executa se liberado
RegistroAtendimento reg = new RegistroAtendimento();
reg.setMatricula(matricula);
reg.setDataHoraAtendimento(agora);
registroRepo.save(reg);

aluno.setUltimaRefeicao(agora);
alunoRepo.save(aluno);

response.put("message", "Refeição liberada para " + aluno.getNome());
return ResponseEntity.ok(response);
}
@GetMapping("/consumo/diario")
public ResponseEntity<?> consumoDiario() {
    LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    return ResponseEntity.ok(consumoRepo.findByDataBetween(hoje, hoje));
}

@GetMapping("/consumo/semanal")
public ResponseEntity<?> consumoSemanal() {
    LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    LocalDate inicio = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);

    return ResponseEntity.ok(
        consumoRepo.findByDataBetween(inicio, hoje)
    );
}

@GetMapping("/consumo/mensal")
public ResponseEntity<?> consumoMensal() {
    LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    return ResponseEntity.ok(
        consumoRepo.findByDataBetween(
            hoje.withDayOfMonth(1),
            hoje
        )
    );
}
}
