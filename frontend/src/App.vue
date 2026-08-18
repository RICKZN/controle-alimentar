<template>
  <div class="app-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- Overlay para mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <img src="/ifba-logo.png" class="ifba-logo-img" alt="Instituto Federal Bahia" />
        <div class="logo">SISTEMA<span>IF</span></div>
        <div class="logo-version">v3.5 · Controle Alimentar</div>
      </div>
      <nav class="sidebar-nav">
        <button @click="changeTab('validacao')" :class="{ active: currentTab === 'validacao' }">
          <span class="icon">📷</span> Validação
        </button>
        <button @click="changeTab('alunos')" :class="{ active: currentTab === 'alunos' }">
          <span class="icon">👥</span> Alunos
        </button>
        <button @click="changeTab('estoque')" :class="{ active: currentTab === 'estoque' }">
          <span class="icon">📦</span> Estoque
        </button>
        <button @click="changeTab('prato')" :class="{ active: currentTab === 'prato' }">
          <span class="icon">🍽️</span> Prato do Dia
        </button>
        <button @click="changeTab('alertas')" :class="{ active: currentTab === 'alertas' }">
          <span class="icon">⚠️</span> Alertas
        </button>
        <button @click="changeTab('geracao')" :class="{ active: currentTab === 'geracao' }">
          <span class="icon">🎟️</span> Gerar Fichas
        </button>
      </nav>
      <div class="sidebar-footer">
        <p>Controle de Refeições</p>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="top-bar">
        <button class="menu-toggle" @click="isSidebarOpen = true">☰</button>
        <div class="top-bar-brand">
          <img src="/ifba-logo.png" class="top-bar-logo" alt="IFBA" />
          <h1>{{ tabTitles[currentTab] }}</h1>
        </div>
        <div class="user-status" :class="{ online: isOnline }">
          {{ isOnline ? 'Online' : 'Offline' }}
        </div>
      </header>

      <div class="content-area">

        <!-- TELA DE VALIDAÇÃO -->
        <div v-if="currentTab === 'validacao'" class="tab-pane">
          <div class="scanner-wrapper card glass-effect">
            <div class="scanner-header">
              <p v-if="!isCameraActive" class="camera-info">A câmera está desligada</p>
              <p v-else class="camera-info active">Buscando QR Code...</p>
            </div>

            <div class="preview-container">
              <div id="reader"></div>

              <!-- Overlay de Detecção -->
              <div v-if="matriculaLida" class="scan-success-overlay">
                <div class="success-card">
                  <div class="success-icon">✅</div>
                  <h3>QR Code Detectado</h3>
                  <div class="id-display">{{ matriculaLida }}</div>
                  <button @click="confirmarValidacao" class="btn btn-validate pulse">LIBERAR ACESSO</button>
                  <button @click="resetScan" class="btn-link">Cancelar</button>
                </div>
              </div>

              <!-- Placeholder quando câmera está desligada -->
              <div v-if="!isCameraActive && !matriculaLida" class="camera-placeholder" @click="toggleCamera">
                <span class="icon-large">📷</span>
                <p>Clique para ligar a câmera</p>
              </div>
            </div>

            <div class="scanner-controls">
              <button @click="toggleCamera" :class="['btn', isCameraActive ? 'btn-danger' : 'btn-success']">
                {{ isCameraActive ? 'Desligar Câmera' : 'Ligar Câmera' }}
              </button>
              <button v-if="isCameraActive" @click="switchCamera" class="btn btn-secondary">
                Trocar Câmera
              </button>
            </div>
          </div>

          <!-- Mensagem de bloqueio de 6h -->
          <div v-if="statusValidacao" class="card status-card" :class="statusValidacao.tipo">
            <p>{{ statusValidacao.msg }}</p>
            <p v-if="statusValidacao.ultimaRefeicao" style="color:#94a3b8; margin-top:0.5rem">
              🍽️ Última refeição: {{ formatarData(statusValidacao.ultimaRefeicao) }}
            </p>
            <p v-if="statusValidacao.proximaRefeicao" style="color:#94a3b8">
              🔓 Próxima liberação: {{ formatarData(statusValidacao.proximaRefeicao) }}
            </p>
            <p v-if="tempoEsperaReal" class="wait-time">⏳ {{ tempoEsperaReal }}</p>
          </div>

          <div class="card glass-effect">
            <h3>Entrada Manual</h3>
            <div class="input-group-row">
              <input type="text" v-model="matriculaParaValidar" placeholder="Matrícula" @keyup.enter="validarFicha(matriculaParaValidar)" />
              <button @click="validarFicha(matriculaParaValidar)" class="btn btn-primary">Validar</button>
            </div>
          </div>
        </div>

        <!-- TELA DE ALUNOS -->
        <div v-if="currentTab === 'alunos'" class="tab-pane">
          <div class="card glass-effect">
            <h3>Cadastrar Novo Aluno</h3>
            <div class="input-group-row">
              <input type="text" v-model="novoAluno.nome" placeholder="Nome completo do aluno" />
              <input type="text" v-model="novoAluno.matricula" placeholder="Número da matrícula" style="width: 180px" />
              <input type="text" v-model="novoAluno.curso" placeholder="Curso" />
              <input type="text" v-model="novoAluno.modalidade" placeholder="Modalidade" />
              <input type="text" v-model="novoAluno.turma" placeholder="Turma" />
              <select v-model="novoAluno.turno" class="input-field" style="width: 160px">
                <option value="" disabled>Turno</option>
                <option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option>
              </select>
              <button @click="cadastrarAluno" class="btn btn-success">Cadastrar</button>
            </div>
          </div>

          <div class="card glass-effect">
            <div class="table-header">
              <h3>Listagem de Estudantes</h3>
              <div class="actions">
                <button @click="carregarAlunos" class="btn-refresh">🔄 Atualizar</button>
                <select v-model="filtroTurno" class="input-field" style="width: 160px">
                  <option value="">Todos os turnos</option>
                  <option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option>
                </select>
                <input type="text" v-model="filtroAluno" placeholder="Pesquisar..." class="search-input" />
              </div>
            </div>
            <div class="table-wrapper">
              <div v-if="alunosFiltrados.length === 0" class="empty-state">
                <p>Nenhum aluno encontrado. Cadastre o primeiro aluno no formulário acima!</p>
              </div>
              <table v-else class="data-table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Matrícula</th>
                    <th>Curso</th>
                    <th>Modalidade</th>
                    <th>Turma</th>
                    <th>Turno</th>
                    <th>Último Acesso</th>
                    <th>Próxima Liberação</th>
                    <th>Tempo Restante</th>
                    <th>Status</th>
                    <th>Ação</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="aluno in alunosFiltrados" :key="aluno.id">
                    <td>{{ aluno.nome }}</td>
                    <td><code>{{ aluno.matricula }}</code></td>
                    <td>{{ aluno.curso }}</td>
                    <td>{{ aluno.modalidade }}</td>
                    <td>{{ aluno.turma }}</td>
                    <td>{{ aluno.turno }}</td>
                    <td>{{ formatarData(aluno.ultimaRefeicao) }}</td>
                    <td>{{ aluno.ultimaRefeicao ? formatarData(new Date(toUTC(aluno.ultimaRefeicao).getTime() + 6*60*60*1000)) : '—' }}</td>
                    <td>
                      <span v-if="!podeComer(aluno.ultimaRefeicao)" style="color:#f59e0b; font-weight:700">
                        {{ tempoRestanteAluno(aluno.ultimaRefeicao) }}
                      </span>
                      <span v-else style="color:#10b981">—</span>
                    </td>
                    <td>
                      <span :class="['status-badge', podeComer(aluno.ultimaRefeicao) ? 'can-eat' : 'must-wait']">
                        {{ podeComer(aluno.ultimaRefeicao) ? 'Pode comer' : 'Aguardar' }}
                      </span>
                    </td>
                    <td>
                      <button @click="editarAluno(aluno)" class="btn-icon-edit">✏️</button>
                      <button @click="excluirAluno(aluno.id)" class="btn-icon-delete">🗑️</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- TELA DE ESTOQUE -->
        <!-- FIX: era uma bagunça de divs mal fechadas com a aba geracao aninhada aqui dentro -->
        <div v-if="currentTab === 'estoque'" class="tab-pane">
          <div class="card glass-effect" style="padding:0.8rem 1.5rem; margin-bottom:1rem; display:flex; gap:8px">
            <button @click="abaEstoque = 'atual'" :class="['btn', abaEstoque==='atual' ? 'btn-primary' : 'btn-secondary']">📦 Estoque Atual</button>
            <button @click="abaEstoque = 'historico'" :class="['btn', abaEstoque==='historico' ? 'btn-primary' : 'btn-secondary']">📥 Entradas de Estoque</button>
            <button @click="abaEstoque = 'historico-pratos'" :class="['btn', abaEstoque==='historico-pratos' ? 'btn-primary' : 'btn-secondary']">🍽️ Histórico de Pratos</button>
          </div>

          <!-- Aba: Estoque Atual -->
          <div v-if="abaEstoque === 'atual'">
            <div class="card glass-effect">
              <div class="table-header">
                <h3>Estoque Atual</h3>
                <div style="display:flex; gap:8px">
                  <button @click="corrigirDuplicados" class="btn-refresh" title="Normaliza nomes e reconsolida o estoque, eliminando cards duplicados do mesmo alimento">🔧 Corrigir duplicados</button>
                  <button @click="carregarEstoque" class="btn-refresh">🔄 Atualizar</button>
                </div>
              </div>
            </div>

            <!-- FIX: grid de itens do estoque nunca era renderizada no template -->
            <div class="estoque-grid">
              <div v-for="item in estoqueList" :key="item.id" class="card glass-effect estoque-card">
                <button class="btn-delete" @click="excluirAlimento(item.id)">🗑️</button>
                <h4>{{ item.nome }}</h4>
                <p class="unit-label">{{ item.unidade }}</p>
                <div style="display:flex; gap:10px; flex-wrap:wrap">
                  <button class="btn-link" @click="abrirLotes(item.nome)">Ver lotes e validades</button>
                  <button class="btn-link" @click="abrirNovoLote(item)">+ Lote</button>
                </div>
                <div class="item-body">
                  <button class="adjust-btn minus" @click="ajustarEstoque(item.id, -1)">−</button>
                  <span class="qty-value">{{ formatarQuantidade(item.quantidade) }}</span>
                  <button class="adjust-btn plus" @click="ajustarEstoque(item.id, 1)">+</button>
                </div>
              </div>
            </div>

            <div class="card glass-effect">
              <h3>Cadastrar Alimento Novo</h3>
              <p style="color:#94a3b8; font-size:0.85rem; margin-bottom:0.8rem">
                Use este formulário apenas para um alimento que ainda não existe no estoque. Para um alimento já cadastrado, use o botão "+ Lote" no card dele.
              </p>
              <div class="input-group-row">
                <input type="text" v-model="novoItem.nome" placeholder="Nome do alimento (Ex: Arroz)" />
                <input type="text" v-model="novoItem.unidade" placeholder="Unidade (kg ou g)" style="width: 130px" />
                <input type="number" v-model="novoItem.quantidade" placeholder="Qtd" style="width: 80px" />
                <input type="date" v-model="novoItem.dataCompra" title="Data da compra" />
                <input type="date" v-model="novoItem.dataValidade" title="Data de validade" />
                <input type="text" v-model="novoItem.usuarioResponsavel" placeholder="Responsável" />
                <button @click="cadastrarNovoAlimento" class="btn btn-success">Cadastrar Alimento</button>
              </div>
            </div>
          </div>

          <div v-if="abaEstoque === 'historico'" class="card glass-effect">
            <h3>Histórico de Entradas</h3>
            <div class="input-group-row"><input v-model="filtroHistorico.alimento" placeholder="Filtrar por alimento" /><input type="date" v-model="filtroHistorico.inicio" /><input type="date" v-model="filtroHistorico.fim" /><input type="date" v-model="filtroHistorico.validade" /><input type="number" v-model="filtroHistorico.lote" placeholder="Lote" /><button @click="carregarHistorico" class="btn btn-primary">Filtrar</button></div>
            <div class="table-wrapper"><table class="data-table"><thead><tr><th>Lote</th><th>Alimento</th><th>Quantidade</th><th>Compra</th><th>Validade</th><th>Responsável</th></tr></thead><tbody><tr v-for="l in historicoEntradas" :key="l.id"><td>{{ l.id }}</td><td>{{ l.nome }}</td><td>{{ formatarQuantidade(l.quantidade) }} {{ l.unidade }}</td><td>{{ formatarDataCurta(l.dataCompra) }}</td><td>{{ formatarDataCurta(l.dataValidade) }}</td><td>{{ l.usuarioResponsavel }}</td></tr></tbody></table></div>
          </div>

          <div v-if="abaEstoque === 'historico-pratos'" class="card glass-effect">
            <h3>Histórico de Pratos</h3>
            <p style="color:#6B7280; font-size:0.85rem; margin-bottom:0.8rem">
              Excluir um registro aqui remove apenas o histórico — não devolve os alimentos ao estoque.
            </p>
            <div v-if="historicoPratos.length === 0" class="empty-state">
              <p>Nenhum prato registrado ainda.</p>
            </div>
            <div v-else class="table-wrapper">
              <table class="data-table">
                <thead><tr><th>Data</th><th>Turno</th><th>Prato</th><th>Ingredientes utilizados</th><th>Ação</th></tr></thead>
                <tbody>
                  <tr v-for="p in historicoPratos" :key="p.id">
                    <td>{{ formatarDataCurta(p.data) }}</td>
                    <td>{{ p.turno || '—' }}</td>
                    <td>{{ p.nome }}</td>
                    <td>
                      <span v-for="(ing, i) in p.ingredientes" :key="i">
                        {{ ing.nome }} ({{ formatarQuantidade(ing.quantidade) }} {{ ing.unidade }})<span v-if="i < p.ingredientes.length - 1">, </span>
                      </span>
                    </td>
                    <td><button @click="excluirPratoHistorico(p.id)" class="btn-icon-delete">🗑️</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div v-if="currentTab === 'prato'" class="tab-pane"><div class="card glass-effect"><h3>Registrar Prato do Dia</h3><div class="input-group-row"><input v-model="pratoDoDia.nome" placeholder="Nome do prato/refeição" /><input type="date" v-model="pratoDoDia.data" /><select v-model="pratoDoDia.turno" class="input-field" style="width:160px"><option value="" disabled>Turno</option><option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option></select></div><h4 style="margin-top:1rem">Ingredientes utilizados</h4><div v-for="(ing, idx) in pratoDoDia.ingredientes" :key="idx" class="input-group-row" style="margin-top:0.5rem"><select v-model="ing.nome" class="input-field"><option value="">Selecione</option><option v-for="item in estoqueList" :key="item.id" :value="item.nome">{{ item.nome }} ({{ formatarQuantidade(item.quantidade) }} {{ item.unidade }})</option></select><input type="number" v-model="ing.quantidade" placeholder="Quantidade" /><input v-model="ing.unidade" placeholder="Unidade" /><button @click="removerIngrediente(idx)" class="btn btn-danger">Remover</button></div><button @click="adicionarIngrediente" class="btn btn-secondary">+ Ingrediente</button><button @click="salvarPratoDia" class="btn btn-success" style="margin-left:0.5rem">Registrar e baixar estoque</button></div></div>

        <div v-if="currentTab === 'alertas'" class="tab-pane">
          <div class="card glass-effect" style="margin-bottom:1rem; display:flex; align-items:center; gap:10px">
            <label style="color:#94a3b8; font-size:0.9rem">⚠️ Limite mínimo para alerta de estoque baixo:</label>
            <input type="number" v-model.number="limiteBaixo" @change="carregarAlertas" style="width:80px" class="input-field" />
          </div>
          <div class="dashboard-grid">
            <div class="card glass-effect alert-card alert-card-amarelo">
              <h3>🟡 Estoque Baixo</h3>
              <p v-if="!alertas.estoqueBaixo?.length">Nenhum item abaixo do limite.</p>
              <ul><li v-for="i in alertas.estoqueBaixo" :key="i.id">{{ i.nome }}: {{ formatarQuantidade(i.quantidade) }} {{ i.unidade }} restantes</li></ul>
            </div>
            <div class="card glass-effect alert-card alert-card-vermelho">
              <h3>🔴 Estoque Esgotado</h3>
              <p v-if="!alertas.estoqueEsgotado?.length">Nenhum item esgotado.</p>
              <ul><li v-for="i in alertas.estoqueEsgotado" :key="i.id">{{ i.nome }} — acabou</li></ul>
            </div>
            <div class="card glass-effect alert-card alert-card-amarelo">
              <h3>🟡 Vencendo em Breve</h3>
              <p v-if="!alertas.vencendoEm?.length">Nenhum lote em aviso de vencimento (30/25/20/15/10/5 dias).</p>
              <ul><li v-for="l in alertas.vencendoEm" :key="l.id">{{ l.nome }} (lote {{ l.id }}) — vence em {{ l.diasRestantes }} dias</li></ul>
            </div>
            <div class="card glass-effect alert-card alert-card-vermelho">
              <h3>🔴 Vencidos</h3>
              <p v-if="!alertas.vencidos?.length">Nenhum lote vencido.</p>
              <ul>
                <li v-for="l in alertas.vencidos" :key="l.id">
                  {{ l.nome }} (lote {{ l.id }}) — {{ l.diasRestantes === 0 ? 'vence hoje' : `venceu há ${Math.abs(l.diasRestantes)} dia(s)` }}
                </li>
              </ul>
            </div>
            <div class="card glass-effect alert-card">
              <h3>🍽️ Resumo Diário</h3>
              <p>Prato: {{ alertas.pratoDoDia?.nome || 'Não registrado' }}</p>
              <p>Refeições liberadas: {{ alertas.refeicoesLiberadasHoje || 0 }}</p>
              <p>Alimentos utilizados: {{ formatarQuantidade(alertas.alimentosUtilizadosHoje || 0) }}</p>
            </div>
          </div>
        </div>

        <!-- TELA DE GERAÇÃO -->
        <!-- FIX: estava aninhada dentro do bloco de estoque, agora é irmã das outras abas -->
        <div v-if="currentTab === 'geracao'" class="tab-pane">
          <div class="card glass-effect">
            <h3>Gerar Ficha QR Code</h3>
            <div class="input-group">
              <input type="text" v-model="matriculaParaGerar" placeholder="Matrícula do aluno" />
              <button @click="gerarQrCode" class="btn btn-primary mt-1">Gerar Agora</button>
            </div>

            <div v-if="qrCodeGerado" class="qr-result">
              <div class="printable-area">
                <qrcode-vue :value="matriculaParaGerar" :size="200" level="H" />
                <p class="qr-label">ALUNO: {{ matriculaParaGerar }}</p>
              </div>
              <button @click="imprimirFicha" class="btn btn-secondary">Imprimir</button>
            </div>
          </div>
        </div>

      </div><!-- /content-area -->

      <!-- Toast de Notificação -->
      <!-- FIX: estava preso dentro da aba de estoque -->
      <Transition name="slide-fade">
        <div v-if="mensagem" :class="['toast', mensagemTipo]">
          {{ mensagem }}
        </div>
      </Transition>

    </main><!-- /main-content -->

    <div v-if="alunoEditando" class="modal-overlay" @click.self="alunoEditando = null">
      <div class="modal-box">
        <h3>Editar Aluno</h3>
        <input v-model="editNome" placeholder="Nome" class="input-field" />
        <input v-model="editMatricula" placeholder="Matrícula" class="input-field" />
        <input v-model="editCurso" placeholder="Curso" class="input-field" />
        <input v-model="editModalidade" placeholder="Modalidade" class="input-field" />
        <input v-model="editTurma" placeholder="Turma" class="input-field" />
        <select v-model="editTurno" class="input-field">
          <option value="" disabled>Turno</option>
          <option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option>
        </select>
        <div style="display:flex; gap:8px; margin-top:12px">
          <!-- FIX: botões sem a classe .btn base — não tinham padding nem border-radius -->
          <button @click="salvarEdicao" class="btn btn-primary">Salvar</button>
          <button @click="alunoEditando = null" class="btn btn-secondary">Cancelar</button>
        </div>
      </div>
    </div>

    <div v-if="alimentoSelecionado" class="modal-overlay" @click.self="alimentoSelecionado = null"><div class="modal-box modal-wide"><h3>Lotes de {{ alimentoSelecionado }}</h3><div class="table-wrapper"><table class="data-table"><thead><tr><th>Lote</th><th>Quantidade</th><th>Compra</th><th>Validade</th></tr></thead><tbody><tr v-for="l in lotesSelecionados" :key="l.id"><td>{{ l.id }}</td><td>{{ formatarQuantidade(l.quantidade) }} {{ l.unidade }}</td><td>{{ formatarDataCurta(l.dataCompra) }}</td><td>{{ formatarDataCurta(l.dataValidade) }}</td></tr></tbody></table></div><button @click="alimentoSelecionado = null" class="btn btn-secondary">Fechar</button></div></div>

    <div v-if="loteParaAdicionar" class="modal-overlay" @click.self="loteParaAdicionar = null">
      <div class="modal-box">
        <h3>Adicionar Lote — {{ loteParaAdicionar.nome }}</h3>
        <div class="input-group" style="display:flex; flex-direction:column; gap:10px">
          <input type="number" v-model="novoLoteExtra.quantidade" placeholder="Quantidade" />
          <input type="date" v-model="novoLoteExtra.dataCompra" title="Data da compra" />
          <input type="date" v-model="novoLoteExtra.dataValidade" title="Data de validade" />
          <input type="text" v-model="novoLoteExtra.usuarioResponsavel" placeholder="Responsável" />
        </div>
        <div style="display:flex; gap:8px; margin-top:14px">
          <button @click="confirmarNovoLote" class="btn btn-success">Adicionar Lote</button>
          <button @click="loteParaAdicionar = null" class="btn btn-secondary">Cancelar</button>
        </div>
      </div>
    </div>

  </div><!-- /app-layout -->
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { Html5Qrcode } from 'html5-qrcode';

const API_URL = import.meta.env.VITE_API_URL || '/api';

// ── Estados ──────────────────────────────────────────────────────────────────
const currentTab       = ref('validacao');
const isSidebarOpen    = ref(false);
const isOnline         = ref(true);
const isCameraActive   = ref(false);
const currentCameraId  = ref('environment');

const tabTitles = {
  validacao: 'Validação de Acesso',
  alunos:    'Banco de Estudantes',
  estoque:   'Gestão de Estoque',
  prato:     'Prato do Dia',
  alertas:   'Dashboard de Alertas',
  geracao:   'Geração de Fichas'
};

const estoqueList         = ref([]);
const alunosList          = ref([]);
const filtroAluno         = ref('');
const matriculaLida       = ref(null);
const matriculaParaValidar = ref('');
const matriculaParaGerar  = ref('');
const qrCodeGerado        = ref(false);
const mensagem            = ref('');
const mensagemTipo        = ref('');
const statusValidacao     = ref(null);
const tempoEsperaReal     = ref('');
const countdownTimer      = ref(null);

// ── Estoque ────────────────────────────────────────────────────────────────────
const abaEstoque      = ref('atual');
const limiteBaixo = ref(2);

const TURNOS = ['Matutino', 'Vespertino', 'Integral', 'Noturno'];
const filtroTurno = ref('');

const hojeISO = () => new Date().toISOString().slice(0, 10);
const novoItem = ref({ nome: '', unidade: 'kg', quantidade: 0, dataCompra: hojeISO(), dataValidade: '', usuarioResponsavel: '' });
const alimentoSelecionado = ref(null);
const lotesSelecionados = ref([]);
const historicoEntradas = ref([]);
const historicoPratos = ref([]);
const filtroHistorico = ref({ alimento: '', inicio: '', fim: '', validade: '', lote: '' });
const alertas = ref({ estoqueBaixo: [], estoqueEsgotado: [], vencendoEm: [], vencidos: [], pratoDoDia: null });
const pratoDoDia = ref({ nome: '', data: hojeISO(), turno: '', ingredientes: [{ nome: '', unidade: 'kg', quantidade: 0 }] });

const carregarHistoricoPratos = async () => {
  const res = await axios.get(`${API_URL}/prato-dia`);
  historicoPratos.value = res.data;
};

const excluirPratoHistorico = async (id) => {
  if (!confirm('Excluir este registro do histórico? Os alimentos já baixados não retornarão ao estoque.')) return;
  try {
    await axios.delete(`${API_URL}/prato-dia/${id}`);
    await carregarHistoricoPratos();
    mostrarMensagem('Registro removido do histórico', 'success');
  } catch (err) { mostrarMensagem('Erro ao excluir registro', 'error'); }
};

watch(abaEstoque, async (val) => {
  if (val === 'historico') carregarHistorico();
  if (val === 'historico-pratos') carregarHistoricoPratos();
});

// ── Contagem regressiva ───────────────────────────────────────────────────────
// FIX: parâmetro renomeado de minutosRestantes para segundosFaltando
// (o backend envia segundosFaltando, que era tratado como segundos no corpo — confusão de nome)
const iniciarContagemRegressiva = (segundosFaltando) => {
  if (countdownTimer.value) clearInterval(countdownTimer.value);

  let segundosTotais = Math.floor(segundosFaltando);

  const atualizarTexto = () => {
    const h = Math.floor(segundosTotais / 3600);
    const m = Math.floor((segundosTotais % 3600) / 60);
    const s = segundosTotais % 60;
    tempoEsperaReal.value = `Aguarde mais ${h}h ${m}m ${s}s`;
  };

  atualizarTexto();

  countdownTimer.value = setInterval(() => {
    if (segundosTotais <= 0) {
      clearInterval(countdownTimer.value);
      tempoEsperaReal.value = 'Pode comer agora!';
      return;
    }
    segundosTotais--;
    atualizarTexto();
  }, 1000);
};

// ── Scanner ───────────────────────────────────────────────────────────────────
let html5QrCode = null;

// ── Estoque ───────────────────────────────────────────────────────────────────
const carregarEstoque = async () => {
  try {
    const res = await axios.get(`${API_URL}/estoque`);
    estoqueList.value = res.data;
    isOnline.value = true;
  } catch (err) { isOnline.value = false; }
};

const verificarAlertaEstoqueId = (id) => {
  const esgotado = (alertas.value.estoqueEsgotado || []).find(i => i.id === id);
  if (esgotado) { mostrarMensagem(`🔴 ${esgotado.nome} esgotou no estoque!`, 'error'); return; }
  const baixo = (alertas.value.estoqueBaixo || []).find(i => i.id === id);
  if (baixo) { mostrarMensagem(`🟡 Estoque baixo: ${baixo.nome} (${formatarQuantidade(baixo.quantidade)} ${baixo.unidade} restantes)`, 'warning'); }
};

const verificarAlertaPorNomes = (nomes) => {
  for (const nome of nomes) {
    const esgotado = (alertas.value.estoqueEsgotado || []).find(i => i.nome === nome);
    if (esgotado) { mostrarMensagem(`🔴 ${esgotado.nome} esgotou no estoque!`, 'error'); return true; }
  }
  for (const nome of nomes) {
    const baixo = (alertas.value.estoqueBaixo || []).find(i => i.nome === nome);
    if (baixo) { mostrarMensagem(`🟡 Estoque baixo: ${baixo.nome} (${formatarQuantidade(baixo.quantidade)} ${baixo.unidade} restantes)`, 'warning'); return true; }
  }
  return false;
};

const ajustarEstoque = async (id, delta) => {
  try {
    await axios.post(`${API_URL}/estoque/${id}/ajustar?variacao=${delta}`);
    await carregarEstoque();
    await carregarAlertas();
    verificarAlertaEstoqueId(id);
  } catch (err) { mostrarMensagem('Erro ao atualizar', 'error'); }
};

const cadastrarNovoAlimento = async () => {
  if (!novoItem.value.nome || !novoItem.value.unidade) return;
  try {
    await axios.post(`${API_URL}/estoque`, novoItem.value);
    novoItem.value = { nome: '', unidade: 'kg', quantidade: 0, dataCompra: hojeISO(), dataValidade: '', usuarioResponsavel: '' };
    await carregarEstoque();
    mostrarMensagem('Alimento cadastrado!', 'success');
  } catch (err) { mostrarMensagem(err.response?.data?.error || 'Erro ao cadastrar', 'error'); }
};

const loteParaAdicionar = ref(null);
const novoLoteExtra = ref({ quantidade: 0, dataCompra: hojeISO(), dataValidade: '', usuarioResponsavel: '' });

const abrirNovoLote = (item) => {
  loteParaAdicionar.value = item;
  novoLoteExtra.value = { quantidade: 0, dataCompra: hojeISO(), dataValidade: '', usuarioResponsavel: '' };
};

const confirmarNovoLote = async () => {
  if (!novoLoteExtra.value.quantidade || !novoLoteExtra.value.dataValidade) {
    mostrarMensagem('Informe quantidade e data de validade.', 'error');
    return;
  }
  try {
    const nome = loteParaAdicionar.value.nome;
    await axios.post(`${API_URL}/estoque/${encodeURIComponent(nome)}/lotes`, {
      unidade: loteParaAdicionar.value.unidade,
      ...novoLoteExtra.value
    });
    loteParaAdicionar.value = null;
    await carregarEstoque();
    mostrarMensagem('Lote adicionado!', 'success');
  } catch (err) { mostrarMensagem(err.response?.data?.error || 'Erro ao adicionar lote', 'error'); }
};

const corrigirDuplicados = async () => {
  try {
    await axios.post(`${API_URL}/estoque/reconstruir`);
    await carregarEstoque();
    mostrarMensagem('Estoque reconsolidado — duplicados corrigidos!', 'success');
  } catch (err) { mostrarMensagem('Erro ao corrigir estoque', 'error'); }
};


const abrirLotes = async (nome) => {
  const res = await axios.get(`${API_URL}/estoque/${encodeURIComponent(nome)}/lotes`);
  alimentoSelecionado.value = nome;
  lotesSelecionados.value = res.data;
};

const carregarHistorico = async () => {
  const params = new URLSearchParams();
  Object.entries(filtroHistorico.value).forEach(([k, v]) => { if (v) params.append(k, v); });
  const res = await axios.get(`${API_URL}/estoque/historico?${params.toString()}`);
  historicoEntradas.value = res.data;
};

const carregarAlertas = async () => {
  const res = await axios.get(`${API_URL}/alertas?limiteBaixo=${limiteBaixo.value}`);
  alertas.value = res.data;
};

const adicionarIngrediente = () => pratoDoDia.value.ingredientes.push({ nome: '', unidade: 'kg', quantidade: 0 });
const removerIngrediente = (idx) => pratoDoDia.value.ingredientes.splice(idx, 1);

const salvarPratoDia = async () => {
  const nomesConsumidos = pratoDoDia.value.ingredientes.map(i => i.nome).filter(Boolean);
  await axios.post(`${API_URL}/prato-dia`, pratoDoDia.value);
  pratoDoDia.value = { nome: '', data: hojeISO(), turno: '', ingredientes: [{ nome: '', unidade: 'kg', quantidade: 0 }] };
  await carregarEstoque();
  await carregarAlertas();
  await carregarHistoricoPratos();
  const teveAlerta = verificarAlertaPorNomes(nomesConsumidos);
  if (!teveAlerta) mostrarMensagem('Prato registrado e estoque baixado por validade mais próxima!', 'success');
};

const formatarDataCurta = (dateStr) => dateStr ? new Date(`${dateStr}T00:00:00`).toLocaleDateString('pt-BR') : '—';
const diasParaVencer = (dateStr) => Math.ceil((new Date(`${dateStr}T00:00:00`) - new Date()) / (1000 * 60 * 60 * 24));

const excluirAlimento = async (id) => {
  if (!confirm('Excluir este alimento?')) return;
  try {
    await axios.delete(`${API_URL}/estoque/${id}`);
    await carregarEstoque();
  } catch (err) { mostrarMensagem('Erro ao excluir', 'error'); }
};

// ── Alunos ────────────────────────────────────────────────────────────────────
const novoAluno = ref({ nome: '', matricula: '', curso: '', modalidade: '', turma: '', turno: '' });

const carregarAlunos = async () => {
  try {
    const res = await axios.get(`${API_URL}/alunos`);
    alunosList.value = res.data;
  } catch (err) { console.error('Erro ao carregar alunos'); }
};

const alunosFiltrados = computed(() => {
  let lista = alunosList.value || [];
  if (filtroTurno.value) lista = lista.filter(a => a.turno === filtroTurno.value);
  if (!filtroAluno.value) return lista;
  const f = filtroAluno.value.toLowerCase();
  return lista.filter(a =>
    (a.nome && a.nome.toLowerCase().includes(f)) ||
    (a.matricula && a.matricula.includes(f))
  );
});

const cadastrarAluno = async () => {
  if (!novoAluno.value.nome || !novoAluno.value.matricula) return;
  try {
    await axios.post(`${API_URL}/alunos`, novoAluno.value);
    // FIX: reset incompleto apagava só nome e matricula, perdendo curso/modalidade/turma/turno
    novoAluno.value = { nome: '', matricula: '', curso: '', modalidade: '', turma: '', turno: '' };
    await carregarAlunos();
    mostrarMensagem('Aluno cadastrado!', 'success');
  } catch (err) {
    mostrarMensagem(err.response?.data?.error || 'Erro ao cadastrar', 'error');
  }
};

const excluirAluno = async (id) => {
  if (!confirm('Excluir este aluno do sistema?')) return;
  try {
    await axios.delete(`${API_URL}/alunos/${id}`);
    await carregarAlunos();
    mostrarMensagem('Aluno removido', 'success');
  } catch (err) { mostrarMensagem('Erro ao excluir', 'error'); }
};

// ── Modal de edição ───────────────────────────────────────────────────────────
const alunoEditando  = ref(null);
const editNome       = ref('');
const editMatricula  = ref('');
const editCurso      = ref('');
const editModalidade = ref('');
const editTurma      = ref('');
const editTurno      = ref('');

const editarAluno = (aluno) => {
  alunoEditando.value  = aluno;
  editNome.value       = aluno.nome;
  editMatricula.value  = aluno.matricula;
  editCurso.value      = aluno.curso;
  editModalidade.value = aluno.modalidade;
  editTurma.value      = aluno.turma;
  editTurno.value      = aluno.turno;
};

const salvarEdicao = async () => {
  await axios.put(`${API_URL}/alunos/${alunoEditando.value.id}`, {
    nome:       editNome.value,
    matricula:  editMatricula.value,
    curso:      editCurso.value,
    modalidade: editModalidade.value,
    turma:      editTurma.value,
    turno:      editTurno.value
  });
  alunoEditando.value = null;
  carregarAlunos();
  carregarAlertas();
  carregarHistorico();
};

// ── Validação ─────────────────────────────────────────────────────────────────
const validarFicha = async (matricula) => {
  if (!matricula) return;
  statusValidacao.value = null;
  tempoEsperaReal.value = '';
  if (countdownTimer.value) clearInterval(countdownTimer.value);
  try {
    const res = await axios.post(`${API_URL}/validar?matricula=${matricula}`);
    mostrarMensagem(res.data.message, 'success');
    statusValidacao.value = { tipo: 'success', titulo: 'Acesso Liberado', msg: res.data.message };
    matriculaParaValidar.value = '';
    carregarEstoque();
    carregarAlunos();
  } catch (err) {
    const errorData = err.response?.data;
    statusValidacao.value = {
      tipo:          'error',
      titulo:        'Acesso Negado',
      msg:           errorData?.error || 'Erro na validação',
      espera:        errorData?.espera,
      ultimaRefeicao: errorData?.horaUltimaRefeicao,
      proximaRefeicao: errorData?.proximaRefeicao
    };
    if (errorData?.segundosFaltando) {
      iniciarContagemRegressiva(errorData.segundosFaltando);
    }
    mostrarMensagem(errorData?.error || 'Erro', 'error');
  }
};

const confirmarValidacao = async () => {
  await validarFicha(matriculaLida.value);
  resetScan();
};

const resetScan = () => {
  matriculaLida.value = null;
  if (isCameraActive.value) startCamera();
};

// ── Utilitários ───────────────────────────────────────────────────────────────
const toUTC = (dateStr) => {
  if (!dateStr) return new Date();
  if (dateStr instanceof Date) return dateStr;
  const str = String(dateStr);
  return str.endsWith('Z') ? new Date(str) : new Date(str + 'Z');
};

const formatarData = (dateStr) => {
  if (!dateStr) return 'Nunca';
  return toUTC(dateStr).toLocaleString('pt-BR');
};

const podeComer = (dateStr) => {
  if (!dateStr) return true;
  const ultima = toUTC(dateStr);
  const diffHoras = (new Date() - ultima) / (1000 * 60 * 60);
  return diffHoras >= 6;
};

const tempoRestanteAluno = (dateStr) => {
  if (!dateStr) return '—';
  const proxima = new Date(toUTC(dateStr).getTime() + 6 * 60 * 60 * 1000);
  const diff = Math.max(0, Math.floor((proxima - new Date()) / 1000));
  const h = Math.floor(diff / 3600);
  const m = Math.floor((diff % 3600) / 60);
  const s = diff % 60;
  return `${String(h).padStart(2,'0')}h ${String(m).padStart(2,'0')}m ${String(s).padStart(2,'0')}s`;
};

const mostrarMensagem = (txt, tipo) => {
  mensagem.value     = txt;
  mensagemTipo.value = tipo;
  setTimeout(() => mensagem.value = '', 4000);
};

const formatarQuantidade = (q) => Number.isInteger(q) ? q : q.toFixed(1);

// ── Câmera ────────────────────────────────────────────────────────────────────
const toggleCamera = async () => {
  if (isCameraActive.value) await stopCamera();
  else await startCamera();
};

const startCamera = async () => {
  if (!html5QrCode) html5QrCode = new Html5Qrcode('reader');
  const config = { fps: 15, qrbox: { width: 250, height: 250 } };
  try {
    await html5QrCode.start(
      { facingMode: currentCameraId.value },
      config,
      (decodedText) => {
        matriculaLida.value = decodedText;
        html5QrCode.stop();
        isCameraActive.value = false;
        if (navigator.vibrate) navigator.vibrate(200);
      }
    );
    isCameraActive.value = true;
  } catch (err) {
    mostrarMensagem('Câmera bloqueada ou não encontrada.', 'error');
  }
};

const stopCamera = async () => {
  if (html5QrCode && isCameraActive.value) {
    await html5QrCode.stop();
    isCameraActive.value = false;
  }
};

const switchCamera = async () => {
  await stopCamera();
  currentCameraId.value = currentCameraId.value === 'environment' ? 'user' : 'environment';
  await startCamera();
};

const changeTab = async (tab) => {
  if (isCameraActive.value) await stopCamera();
  currentTab.value    = tab;
  isSidebarOpen.value = false;
  if (tab === 'alunos') carregarAlunos();
  if (tab === 'alertas') carregarAlertas();
  if (tab === 'prato') carregarEstoque();
};

// ── QR Code / Impressão ───────────────────────────────────────────────────────
const gerarQrCode   = () => { if (matriculaParaGerar.value) qrCodeGerado.value = true; };
const imprimirFicha = () => window.print();

// ── Lifecycle ─────────────────────────────────────────────────────────────────
onMounted(() => {
  carregarEstoque();
  carregarAlunos();
  carregarAlertas();
  carregarHistorico();
  carregarHistoricoPratos();
  // Atualiza contadores de tempo restante a cada segundo
  setInterval(() => { alunosList.value = [...alunosList.value]; }, 1000);
});

onUnmounted(stopCamera);
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap');

:root {
  --primary:        #006B3D;
  --primary-hover:  #005530;
  --primary-light:  #E6F4ED;
  --success:        #059669;
  --success-light:  #ECFDF5;
  --danger:         #DC2626;
  --danger-light:   #FEF2F2;
  --warning:        #D97706;
  --warning-light:  #FFFBEB;
  --bg:             #F3F6F4;
  --surface:        #FFFFFF;
  --sidebar-bg:     #0F1E13;
  --sidebar-text:   #8FB49A;
  --text-primary:   #111827;
  --text-secondary: #6B7280;
  --text-muted:     #9CA3AF;
  --border:         #E5E7EB;
  --border-strong:  #D1D5DB;
  --shadow-sm:      0 1px 3px rgba(0,0,0,0.07), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md:      0 4px 12px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.04);
  --shadow-lg:      0 10px 30px rgba(0,0,0,0.10), 0 4px 8px rgba(0,0,0,0.05);
  --radius:         12px;
}

* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Outfit', sans-serif; background-color: var(--bg); color: var(--text-primary); overflow-x: hidden; }
.app-layout { display: flex; min-height: 100vh; }

/* SIDEBAR */
.sidebar { width: 256px; background: var(--sidebar-bg); position: fixed; height: 100vh; z-index: 100; transition: transform 0.3s; display: flex; flex-direction: column; }
.sidebar-header { padding: 1.75rem 1.25rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.07); }
.ifba-logo-img { width: 66px; height: auto; margin-bottom: 0.75rem; }
.logo { font-size: 1.15rem; font-weight: 700; color: #FFFFFF; letter-spacing: 0.05em; }
.logo span { color: #4ADE80; }
.logo-version { font-size: 0.68rem; color: var(--sidebar-text); font-weight: 400; margin-top: 0.25rem; }
.sidebar-nav { padding: 1.25rem 0.75rem; flex: 1; display: flex; flex-direction: column; gap: 2px; }
.sidebar-nav button { width: 100%; padding: 0.72rem 1rem; background: transparent; border: none; color: var(--sidebar-text); font-size: 0.88rem; font-weight: 500; border-radius: 8px; cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all 0.16s; font-family: inherit; text-align: left; }
.sidebar-nav button:hover { background: rgba(255,255,255,0.07); color: #FFFFFF; }
.sidebar-nav button.active { background: var(--primary); color: #FFFFFF; font-weight: 600; box-shadow: 0 2px 10px rgba(0,107,61,0.40); }
.sidebar-footer { padding: 1rem 1.25rem; color: var(--sidebar-text); font-size: 0.72rem; border-top: 1px solid rgba(255,255,255,0.07); text-align: center; }

/* MAIN */
.main-content { flex: 1; margin-left: 256px; min-width: 0; display: flex; flex-direction: column; }
.top-bar { height: 62px; background: var(--surface); display: flex; align-items: center; justify-content: space-between; padding: 0 2rem; border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 90; box-shadow: var(--shadow-sm); }
.top-bar h1 { font-size: 0.95rem; font-weight: 600; color: var(--text-primary); }
.top-bar-brand { display: flex; align-items: center; gap: 0.7rem; }
.top-bar-logo { height: 30px; width: auto; display: none; }
.content-area { padding: 1.75rem 2rem; max-width: 1200px; margin: 0 auto; width: 100%; }
.menu-toggle { display: none; background: transparent; border: none; color: var(--text-primary); font-size: 1.35rem; cursor: pointer; }

/* Online/Offline */
.user-status { padding: 3px 11px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; background: var(--danger-light); color: var(--danger); border: 1px solid #FECACA; }
.user-status.online { background: var(--success-light); color: var(--success); border-color: #A7F3D0; }

/* CARDS */
.card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 1.5rem; margin-bottom: 1.25rem; box-shadow: var(--shadow-sm); }
.glass-effect { box-shadow: var(--shadow-md); }
.card h3 { font-size: 1rem; font-weight: 700; color: var(--text-primary); margin-bottom: 1rem; }
.card h4 { font-size: 0.9rem; font-weight: 600; color: var(--text-secondary); margin-bottom: 0.75rem; }

/* BUTTONS */
.btn { padding: 0.55rem 1.15rem; border: none; border-radius: 8px; font-size: 0.88rem; font-weight: 600; cursor: pointer; transition: all 0.16s; font-family: inherit; }
.btn:active { transform: scale(0.97); }
.btn-primary   { background: var(--primary); color: white; }
.btn-primary:hover { background: var(--primary-hover); }
.btn-success   { background: var(--success); color: white; }
.btn-success:hover { background: #047857; }
.btn-danger    { background: var(--danger); color: white; }
.btn-secondary { background: #F3F4F6; color: var(--text-primary); border: 1px solid var(--border-strong); }
.btn-secondary:hover { background: #E9EAEC; }
.btn-validate  { background: var(--success); color: white; width: 100%; margin-top: 1rem; }
.btn-link { background: transparent; border: none; color: var(--primary); cursor: pointer; margin-top: 0.5rem; font-size: 0.85rem; font-family: inherit; font-weight: 500; }
.btn-link:hover { text-decoration: underline; }
.btn-refresh { background: #F9FAFB; border: 1px solid var(--border-strong); color: var(--text-secondary); padding: 5px 12px; border-radius: 8px; cursor: pointer; font-size: 0.83rem; margin-right: 8px; font-family: inherit; transition: 0.16s; }
.btn-refresh:hover { background: var(--primary-light); color: var(--primary); border-color: var(--primary); }
.mt-1 { margin-top: 0.5rem; }

/* INPUTS */
.input-group-row { display: flex; gap: 8px; flex-wrap: wrap; }
.input-group-row input,
.input-group input {
  flex: 1;
  min-width: 120px;
  padding: 0.55rem 0.9rem;
  background: #FAFAFA;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: inherit;
  font-size: 0.88rem;
  transition: border-color 0.16s, box-shadow 0.16s;
}
.input-group-row input:focus,
.input-group input:focus { outline: none; border-color: var(--primary); background: #fff; box-shadow: 0 0 0 3px rgba(0,107,61,0.10); }
.input-group { display: flex; flex-direction: column; gap: 8px; }
.input-field { width: 100%; padding: 0.55rem 0.9rem; background: #FAFAFA; border: 1px solid var(--border-strong); border-radius: 8px; color: var(--text-primary); font-size: 0.88rem; font-family: inherit; transition: border-color 0.16s; }
.input-field:focus { outline: none; border-color: var(--primary); background: #fff; box-shadow: 0 0 0 3px rgba(0,107,61,0.10); }
.search-input { background: #FAFAFA; border: 1px solid var(--border-strong); padding: 0.55rem 0.9rem; border-radius: 8px; color: var(--text-primary); flex: 1; max-width: 340px; font-family: inherit; font-size: 0.88rem; }
.search-input:focus { outline: none; border-color: var(--primary); }
input::placeholder { color: var(--text-muted); }
select.input-field { background: #FAFAFA; }
select.input-field option { background: white; color: var(--text-primary); }

/* TABLES */
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; gap: 1rem; flex-wrap: wrap; }
.table-wrapper { overflow-x: auto; border-radius: 8px; border: 1px solid var(--border); }
.data-table { width: 100%; border-collapse: collapse; text-align: left; }
.data-table th { padding: 0.75rem 1rem; color: var(--text-secondary); border-bottom: 2px solid var(--border); font-weight: 600; font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.05em; background: #FAFAFA; white-space: nowrap; }
.data-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); font-size: 0.88rem; color: var(--text-primary); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #FAFFFE; }
.data-table code { background: var(--primary-light); color: var(--primary); padding: 2px 7px; border-radius: 5px; font-size: 0.82rem; font-weight: 600; }
.status-badge { padding: 3px 9px; border-radius: 10px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; white-space: nowrap; }
.status-badge.can-eat   { background: var(--success-light); color: var(--success); }
.status-badge.must-wait { background: var(--warning-light); color: var(--warning); }
.actions { display: flex; align-items: center; flex: 1; justify-content: flex-end; gap: 8px; }
.empty-state { padding: 3rem; text-align: center; color: var(--text-muted); }

/* SCANNER */
.scanner-wrapper { max-width: 400px; margin: 0 auto 1.25rem; text-align: center; }
.scanner-header { margin-bottom: 0.5rem; }
.camera-info { color: var(--text-muted); font-size: 0.88rem; }
.camera-info.active { color: var(--success); font-weight: 600; }
.preview-container { position: relative; aspect-ratio: 1; background: #111827; border-radius: var(--radius); overflow: hidden; margin-bottom: 1rem; border: 2px solid var(--border); }
#reader { width: 100%; height: 100%; }
.camera-placeholder { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #6B7280; gap: 0.5rem; background: #1F2937; }
.icon-large { font-size: 2.5rem; }
.scan-success-overlay { position: absolute; inset: 0; background: rgba(10, 20, 10, 0.96); display: flex; align-items: center; justify-content: center; z-index: 50; }
.success-card { text-align: center; padding: 1.5rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; color: white; }
.id-display { font-size: 1.15rem; font-weight: 700; background: rgba(255,255,255,0.12); padding: 0.5rem 1rem; border-radius: 8px; margin: 0.5rem 0; }
.scanner-controls { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.status-card { border-left: 4px solid var(--primary); }
.status-card.success { border-color: var(--success); background: var(--success-light); }
.status-card.success p { color: #065F46; }
.status-card.error   { border-color: var(--danger); background: var(--danger-light); }
.status-card.error p { color: #991B1B; }
.wait-time { font-weight: 700; color: var(--warning); font-size: 1rem; margin-top: 0.5rem; }

/* ESTOQUE */
.estoque-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 1rem; margin-bottom: 1.25rem; }
.estoque-card { padding: 1.25rem; position: relative; border: 1px solid var(--primary); border-top: 4px solid var(--primary); background: #FFFFFF; box-shadow: 0 2px 8px rgba(0,107,61,0.10); }
.estoque-card h4 { font-size: 0.95rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem; }
.btn-delete { position: absolute; top: 10px; right: 10px; background: transparent; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer; line-height: 1; padding: 4px; border-radius: 6px; transition: 0.16s; }
.btn-delete:hover { color: var(--danger); background: var(--danger-light); }
.unit-label { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 1rem; }
.item-body { display: flex; align-items: center; justify-content: space-between; margin-top: 0.75rem; }
.adjust-btn { width: 32px; height: 32px; border-radius: 50%; border: none; font-size: 1rem; cursor: pointer; transition: 0.16s; font-weight: 700; }
.adjust-btn.minus { background: #F3F4F6; color: var(--text-primary); border: 1px solid var(--border-strong); }
.adjust-btn.minus:hover { background: var(--danger-light); color: var(--danger); }
.adjust-btn.plus  { background: var(--primary); color: white; }
.adjust-btn.plus:hover  { background: var(--primary-hover); }
.qty-value { font-size: 1.5rem; font-weight: 800; color: var(--text-primary); }

/* BOTOES DE ACAO */
.btn-icon-delete { background: transparent; border: none; font-size: 1rem; cursor: pointer; padding: 5px 7px; border-radius: 6px; transition: background 0.16s; }
.btn-icon-delete:hover { background: var(--danger-light); }
.btn-icon-edit { background: none; border: none; cursor: pointer; font-size: 1rem; margin-right: 2px; padding: 5px 7px; border-radius: 6px; transition: background 0.16s; }
.btn-icon-edit:hover { background: var(--primary-light); }

/* MODAL */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 999; backdrop-filter: blur(3px); }
.modal-box { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 28px; display: flex; flex-direction: column; gap: 12px; min-width: 340px; box-shadow: var(--shadow-lg); }
.modal-wide { width: min(760px, 94vw); }
.modal-box h3 { color: var(--text-primary); margin: 0; font-size: 1.05rem; font-weight: 700; }
.dashboard-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem; }
.alert-card ul { margin: 0.75rem 0 0 1.2rem; }
.alert-card li { font-size: 0.88rem; padding: 0.2rem 0; }
.alert-card-amarelo { border-left: 5px solid var(--warning); background: var(--warning-light); }
.alert-card-amarelo ul { color: #92400E; }
.alert-card-vermelho { border-left: 5px solid var(--danger); background: var(--danger-light); }
.alert-card-vermelho ul { color: #991B1B; }
.chip { background: var(--primary-light); color: var(--primary); padding: 3px 11px; border-radius: 20px; font-size: 0.78rem; font-weight: 600; }
.ranking-list { list-style: none; padding: 0; }
.ranking-list li { padding: 0.6rem 0; border-bottom: 1px solid var(--border); display: flex; gap: 8px; font-size: 0.88rem; color: var(--text-primary); }
.ranking-pos { font-weight: 800; color: var(--primary); min-width: 28px; }

/* QR CODE */
.qr-result { margin-top: 1.5rem; display: flex; flex-direction: column; align-items: center; gap: 1rem; }
.qr-label { margin-top: 0.5rem; font-weight: 700; color: var(--text-primary); }

/* TOAST */
.toast { position: fixed; bottom: 2rem; right: 2rem; padding: 0.85rem 1.75rem; border-radius: 10px; z-index: 200; font-weight: 600; box-shadow: var(--shadow-lg); font-size: 0.88rem; }
.toast.success { background: var(--success); color: white; }
.toast.error   { background: var(--danger);  color: white; }
.toast.warning { background: var(--warning); color: white; }

.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateX(20px); opacity: 0; }

.pulse { animation: pulse 1.5s infinite; }
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 rgba(5, 150, 105, 0.4); }
  70%  { box-shadow: 0 0 0 10px rgba(5, 150, 105, 0); }
  100% { box-shadow: 0 0 0 0 rgba(5, 150, 105, 0); }
}

/* MOBILE */
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar-open .sidebar { transform: translateX(0); }
  .main-content { margin-left: 0; }
  .menu-toggle { display: block; }
  .top-bar-logo { display: block; }
  .sidebar-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 95; }
  .content-area { padding: 1rem; }
  .top-bar { padding: 0 1rem; }
}

/* IMPRESSAO */
@media print {
  body * { display: none !important; }
  .printable-area, .printable-area * { display: block !important; }
  .printable-area { position: absolute; top: 0; width: 100%; text-align: center; }
}
</style>
