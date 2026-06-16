<template>
  <div class="app-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- Overlay para mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">SISTEMA<span>IF</span> v3.5</div>
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
        <h1>{{ tabTitles[currentTab] }}</h1>
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
              <input type="text" v-model="novoAluno.turno" placeholder="Turno" />
              <button @click="cadastrarAluno" class="btn btn-success">Cadastrar</button>
            </div>
          </div>

          <div class="card glass-effect">
            <div class="table-header">
              <h3>Listagem de Estudantes</h3>
              <div class="actions">
                <button @click="carregarAlunos" class="btn-refresh">🔄 Atualizar</button>
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
<div v-if="currentTab === 'estoque'" class="tab-pane">

  <div class="card glass-effect" style="padding:0.8rem 1.5rem; margin-bottom:1rem; display:flex; gap:8px">
    <button @click="abaEstoque = 'atual'" :class="['btn', abaEstoque==='atual' ? 'btn-primary' : 'btn-secondary']">📦 Estoque Atual</button>
    <button @click="abaEstoque = 'semanal'" :class="['btn', abaEstoque==='semanal' ? 'btn-primary' : 'btn-secondary']">📊 Semanal</button>
    <button @click="abaEstoque = 'mensal'" :class="['btn', abaEstoque==='mensal' ? 'btn-primary' : 'btn-secondary']">📈 Mensal</button>
  </div>

  <div v-if="abaEstoque === 'atual'">
    <div class="card glass-effect">
      <div class="table-header">
        <h3>Estoque Atual</h3>
        <button @click="carregarEstoque" class="btn-refresh">🔄 Atualizar</button>
      </div>
    </div>
    <div class="card glass-effect">
      <h3>Adicionar Novo Alimento</h3>
      <div class="input-group-row">
        <input type="text" v-model="novoItem.nome" placeholder="Nome do alimento (Ex: Arroz)" />
        <input type="text" v-model="novoItem.unidade" placeholder="Unidade (Ex: kg)" style="width: 100px" />
        <input type="number" v-model="novoItem.quantidade" placeholder="Qtd" style="width: 80px" />
        <button @click="cadastrarNovoAlimento" class="btn btn-success">Adicionar</button>
      </div>
    </div>
    <div>
        <!-- TELA DE GERAÇÃO -->
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
      </div>

      <!-- Toast de Notificação -->
<Transition name="slide-fade">
  <div v-if="mensagem" :class="['toast', mensagemTipo]">
    {{ mensagem }}
  </div>
</Transition>
</main>

<div v-if="alunoEditando"class="modal-overlay" @click.self="alunoEditando = null">
  <div class="modal-box">
    <h3>Editar Aluno</h3>

    <input v-model="editNome" placeholder="Nome" class="input-field" />
    <input v-model="editMatricula" placeholder="Matrícula" class="input-field" />
    <input v-model="editCurso" placeholder="Curso" class="input-field" />
    <input v-model="editModalidade" placeholder="Modalidade" class="input-field" />
    <input v-model="editTurma" placeholder="Turma" class="input-field" />
    <input v-model="editTurno" placeholder="Turno" class="input-field" />

    <div style="display:flex; gap:8px; margin-top:12px">
      <button @click="salvarEdicao" class="btn-primary">
        Salvar
      </button>

      <button @click="alunoEditando = null" class="btn-secondary">
        Cancelar
      </button>
    </div>
  </div>
</div>


</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { Html5Qrcode } from 'html5-qrcode';

const API_URL = import.meta.env.VITE_API_URL || '/api';

// Estados
const currentTab = ref('validacao');
const isSidebarOpen = ref(false);
const isOnline = ref(true);
const isCameraActive = ref(false);
const currentCameraId = ref('environment');

const tabTitles = {
  validacao: 'Validação de Acesso',
  alunos: 'Banco de Estudantes',
  estoque: 'Gestão de Estoque',
  geracao: 'Geração de Fichas'
};

const estoqueList = ref([]);
const alunosList = ref([]);
const filtroAluno = ref('');
const matriculaLida = ref(null);
const matriculaParaValidar = ref('');
const matriculaParaGerar = ref('');
const qrCodeGerado = ref(false);
const mensagem = ref('');
const mensagemTipo = ref('');
const statusValidacao = ref(null);
const tempoEsperaReal = ref("");
const countdownTimer = ref(null);

  
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

const abaEstoque = ref('atual');
const consumoDiario = ref([]);
const consumoSemanal = ref([]);
const consumoMensal = ref([]);
const chartSemanalRef = ref(null);
const chartMensalRef = ref(null);
let chartSemanal = null;
let chartMensal = null;

const carregarConsumo = async () => {
  const [d, s, m] = await Promise.all([
    axios.get(`${API_URL}/consumo/diario`),
    axios.get(`${API_URL}/consumo/semanal`),
    axios.get(`${API_URL}/consumo/mensal`)
  ]);
  consumoDiario.value = d.data;
  consumoSemanal.value = s.data;
  consumoMensal.value = m.data;
};

const agrupar = (registros) => {
  const map = {};
  registros.forEach(r => {
    map[r.nomeItem] = (map[r.nomeItem] || 0) + r.quantidadeGasta;
  });
  return map;
};

const renderChart = (canvasRef, chartInstance, dados, titulo) => {
  if (chartInstance) chartInstance.destroy();
  const agrupado = agrupar(dados);
  return new Chart(canvasRef, {
    type: 'bar',
    data: {
      labels: Object.keys(agrupado),
      datasets: [{
        label: titulo,
        data: Object.values(agrupado),
        backgroundColor: 'rgba(79,70,229,0.7)',
        borderColor: '#4f46e5',
        borderWidth: 2,
        borderRadius: 6
      }]
    },
    options: {
      responsive: true,
      plugins: { legend: { labels: { color: '#f8fafc' } } },
      scales: {
        x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
        y: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.05)' } }
      }
    }
  });
};

watch(abaEstoque, async (val) => {
  await carregarConsumo();
  await nextTick();
  if (val === 'semanal' && chartSemanalRef.value)
    chartSemanal = renderChart(chartSemanalRef.value,
// Campos de Formulário (O que estava faltando!)
const novoItem = ref({ nome: '', unidade: '', quantidade: 0 });


const iniciarContagemRegressiva = (minutosRestantes) => {
  if (countdownTimer.value) clearInterval(countdownTimer.value);
  
 let segundosTotais = Math.floor(minutosRestantes);
  
  const atualizarTexto = () => {
    const h = Math.floor(segundosTotais / 3600);
    const m = Math.floor((segundosTotais % 3600) / 60);
    const s = segundosTotais % 60;
    tempoEsperaReal.value = `Aguarde mais ${h}h ${m}m ${s}s`;
  };
  

  atualizarTexto(); // Atualiza na hora

  countdownTimer.value = setInterval(() => {
    if (segundosTotais <= 0) {
      clearInterval(countdownTimer.value);
      tempoEsperaReal.value = "Pode comer agora!";
      return;
    }
    segundosTotais--;
    atualizarTexto();
  }, 1000);
};

let html5QrCode = null;

// Lógica de Negócio
const carregarEstoque = async () => {
  try {
    const res = await axios.get(`${API_URL}/estoque`);
    estoqueList.value = res.data;
    isOnline.value = true;
  } catch (err) { isOnline.value = false; }
};

const carregarAlunos = async () => {
  try {
    const res = await axios.get(`${API_URL}/alunos`);
    alunosList.value = res.data;
  } catch (err) { console.error("Erro ao carregar alunos"); }
};

const alunosFiltrados = computed(() => {
  const lista = alunosList.value || [];
  if (!filtroAluno.value) return lista;
  const f = filtroAluno.value.toLowerCase();
  return lista.filter(a => 
    (a.nome && a.nome.toLowerCase().includes(f)) || 
    (a.matricula && a.matricula.includes(f))
  );
});

const ajustarEstoque = async (id, delta) => {
  try {
    await axios.post(`${API_URL}/estoque/${id}/ajustar?variacao=${delta}`);
    await carregarEstoque();
  } catch (err) { mostrarMensagem("Erro ao atualizar", "error"); }
};

  
const cadastrarNovoAlimento = async () => {
  if (!novoItem.value.nome || !novoItem.value.unidade) return;
  try {
    await axios.post(`${API_URL}/estoque`, novoItem.value);
    novoItem.value = { nome: '', unidade: '', quantidade: 0 };
    await carregarEstoque();
    mostrarMensagem("Item adicionado!", "success");
  } catch (err) { mostrarMensagem("Erro ao cadastrar", "error"); }
};
const novoAluno = ref({ nome: '', matricula: '', curso: '', modalidade: '', turma: '', turno: '' });
  
const cadastrarAluno = async () => {
  if (!novoAluno.value.nome || !novoAluno.value.matricula) return;
  try {
    await axios.post(`${API_URL}/alunos`, novoAluno.value);
    novoAluno.value = { nome: '', matricula: '' };
    await carregarAlunos();
    mostrarMensagem("Aluno cadastrado!", "success");
  } catch (err) { 
    mostrarMensagem(err.response?.data?.error || "Erro ao cadastrar", "error"); 
  }
};

const excluirAlimento = async (id) => {
  if (!confirm("Excluir este alimento?")) return;
  try {
    await axios.delete(`${API_URL}/estoque/${id}`);
    await carregarEstoque();
  } catch (err) { mostrarMensagem("Erro ao excluir", "error"); }
};

const excluirAluno = async (id) => {
  if (!confirm("Excluir este aluno do sistema?")) return;
  try {
    await axios.delete(`${API_URL}/alunos/${id}`);
    await carregarAlunos();
    mostrarMensagem("Aluno removido", "success");
  } catch (err) { mostrarMensagem("Erro ao excluir", "error"); }
};
 const alunoEditando = ref(null);
const editNome = ref('');
const editMatricula = ref('');
const editCurso = ref('');
const editModalidade = ref('');
const editTurma = ref('');
const editTurno = ref('');

const editarAluno = (aluno) => {
  alunoEditando.value = aluno;
  editNome.value = aluno.nome;
  editMatricula.value = aluno.matricula;
  editCurso.value = aluno.curso;
  editModalidade.value = aluno.modalidade;
  editTurma.value = aluno.turma;
  editTurno.value = aluno.turno;
};

const salvarEdicao = async () => {
  await axios.put(`${API_URL}/alunos/${alunoEditando.value.id}`, {
    nome: editNome.value,
    matricula: editMatricula.value,
    curso: editCurso.value,
    modalidade: editModalidade.value,
    turma: editTurma.value,
    turno: editTurno.value
  });
  alunoEditando.value = null;
  carregarAlunos();
};

const validarFicha = async (matricula) => {
  if (!matricula) return;
  statusValidacao.value = null;
  tempoEsperaReal.value = "";
  if (countdownTimer.value) clearInterval(countdownTimer.value);
  try {
    const res = await axios.post(`${API_URL}/validar?matricula=${matricula}`);
    mostrarMensagem(res.data.message, 'success');
    statusValidacao.value = {
      tipo: 'success',
      titulo: 'Acesso Liberado',
      msg: res.data.message
    };
    matriculaParaValidar.value = '';
    carregarEstoque();
    carregarAlunos();
  } catch (err) {
    const errorData = err.response?.data;
    statusValidacao.value = {
    tipo: 'error',
    titulo: 'Acesso Negado',
    msg: errorData?.error || "Erro na validação",
    espera: errorData?.espera,
    ultimaRefeicao: errorData?.horaUltimaRefeicao,
    proximaRefeicao: errorData?.proximaRefeicao
};
    
    
if (errorData?.segundosFaltando) {
  iniciarContagemRegressiva(errorData.segundosFaltando);
}

    
    
    mostrarMensagem(errorData?.error || "Erro", 'error');
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

// Utils
// Converte string do backend (UTC sem fuso) para Date correto
const toUTC = (dateStr) => {
  if (!dateStr) return new Date();

  // Se já for um objeto Date
  if (dateStr instanceof Date) {
    return dateStr;
  }

  // Garante que seja string antes de usar endsWith
  const str = String(dateStr);

  return str.endsWith('Z')
    ? new Date(str)
    : new Date(str + 'Z');
};

const formatarData = (dateStr) => {
  if (!dateStr) return 'Nunca';
  return toUTC(dateStr).toLocaleString('pt-BR');
};

const podeComer = (dateStr) => {
  if (!dateStr) return true;
  const ultima = toUTC(dateStr);
  const agora = new Date();
  const diffHoras = (agora - ultima) / (1000 * 60 * 60);
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
  mensagem.value = txt;
  mensagemTipo.value = tipo;
  setTimeout(() => mensagem.value = '', 4000);
};

// Lógica do Scanner
const toggleCamera = async () => {
  if (isCameraActive.value) await stopCamera();
  else await startCamera();
};

const startCamera = async () => {
  if (!html5QrCode) html5QrCode = new Html5Qrcode("reader");
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
    mostrarMensagem("Câmera bloqueada ou não encontrada.", "error");
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
  currentTab.value = tab;
  isSidebarOpen.value = false;
  if (tab === 'alunos') carregarAlunos();
};

const gerarQrCode = () => { if (matriculaParaGerar.value) qrCodeGerado.value = true; };
const imprimirFicha = () => window.print();
const formatarQuantidade = (q) => Number.isInteger(q) ? q : q.toFixed(1);
  

onMounted(() => {
  carregarEstoque();
  carregarAlunos();
  setInterval(() => {
    alunosList.value = [...alunosList.value];
  }, 1000);
});
onUnmounted(stopCamera);
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap');

:root {
  --primary: #4f46e5;
  --success: #10b981;
  --danger: #f43f5e;
  --warning: #f59e0b;
  --bg-dark: #020617;
  --sidebar-bg: #0f172a;
  --glass: rgba(30, 41, 59, 0.6);
  --border: rgba(255, 255, 255, 0.08);
}

* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Outfit', sans-serif; background-color: var(--bg-dark); color: #f8fafc; overflow-x: hidden; }
.app-layout { display: flex; min-height: 100vh; }

/* SIDEBAR */
.sidebar { width: 280px; background: var(--sidebar-bg); border-right: 1px solid var(--border); position: fixed; height: 100vh; z-index: 100; transition: transform 0.3s; }
.sidebar-header { padding: 2rem; text-align: center; }
.logo { font-size: 1.5rem; font-weight: 800; }
.logo span { color: var(--primary); }
.sidebar-nav { padding: 1rem; flex: 1; }
.sidebar-nav button { width: 100%; padding: 1rem; margin-bottom: 0.5rem; background: transparent; border: none; color: #94a3b8; font-size: 1rem; font-weight: 600; border-radius: 12px; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: all 0.2s; }
.sidebar-nav button.active { background: var(--primary); color: white; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3); }

/* MAIN */
.main-content { flex: 1; margin-left: 280px; min-width: 0; }
.top-bar { height: 70px; background: rgba(2, 6, 23, 0.8); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: space-between; padding: 0 2rem; border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 90; }
.content-area { padding: 2rem; max-width: 1200px; margin: 0 auto; }

/* CARDS */
.card { background: var(--glass); backdrop-filter: blur(12px); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; margin-bottom: 1.5rem; }
.glass-effect { box-shadow: 0 8px 32px rgba(0,0,0,0.3); }

/* TABLES */
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; gap: 1rem; }
.search-input { background: rgba(255,255,255,0.05); border: 1px solid var(--border); padding: 0.6rem 1rem; border-radius: 10px; color: white; flex: 1; max-width: 400px; }
.table-wrapper { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; text-align: left; }
.data-table th { padding: 1rem; color: #94a3b8; border-bottom: 1px solid var(--border); font-weight: 600; }
.data-table td { padding: 1rem; border-bottom: 1px solid rgba(255,255,255,0.03); }
.status-badge { padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
.status-badge.can-eat { background: rgba(16, 185, 129, 0.2); color: var(--success); }
.status-badge.must-wait { background: rgba(245, 158, 11, 0.2); color: var(--warning); }

/* SCANNER UI */
.scanner-wrapper { max-width: 450px; margin: 0 auto 1.5rem; text-align: center; }
.preview-container { position: relative; aspect-ratio: 1; background: #000; border-radius: 15px; overflow: hidden; margin-bottom: 1rem; border: 2px solid var(--border); }
#reader { width: 100%; height: 100%; }
.camera-placeholder { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #475569; }
.scan-success-overlay { position: absolute; inset: 0; background: rgba(2, 6, 23, 0.95); display: flex; align-items: center; justify-content: center; z-index: 50; }
.status-card { text-align: center; border-left: 6px solid var(--primary); }
.status-card.success { border-color: var(--success); background: rgba(16, 185, 129, 0.1); }
.status-card.error { border-color: var(--danger); background: rgba(244, 63, 94, 0.1); }
.wait-time { font-weight: 800; color: var(--warning); font-size: 1.1rem; margin-top: 0.5rem; }

/* ESTOQUE UI */
.estoque-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.estoque-card { padding: 1.2rem; position: relative; }
.btn-delete { position: absolute; top: 10px; right: 10px; background: transparent; border: none; color: #64748b; font-size: 1.5rem; cursor: pointer; line-height: 1; }
.btn-delete:hover { color: var(--danger); }
.unit-label { font-size: 0.8rem; color: #64748b; margin-bottom: 1rem; }
.item-body { display: flex; align-items: center; justify-content: space-between; }
.adjust-btn { width: 36px; height: 36px; border-radius: 50%; border: none; color: white; font-size: 1.2rem; cursor: pointer; transition: 0.2s; }
.adjust-btn.minus { background: #334155; }
.adjust-btn.plus { background: var(--primary); }
.qty-value { font-size: 1.5rem; font-weight: 800; }

.btn-icon-delete {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 5px;
  border-radius: 8px;
  transition: background 0.2s;
}

.btn-icon-delete:hover {
  background: rgba(244, 63, 94, 0.1);
}
  .btn-icon-edit {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1rem;
  margin-right: 4px;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal-box {
  background: #1e1e2e;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 300px;
}
.modal-box h3 {
  color: #fff;
  margin: 0;
}

.empty-state {
  padding: 3rem;
  text-align: center;
  color: #64748b;
  font-style: italic;
}

/* FORM & BUTTONS */
.input-group-row { display: flex; gap: 10px; flex-wrap: wrap; }
.btn-refresh {
  background: rgba(255,255,255,0.1);
  border: 1px solid var(--border);
  color: white;
  padding: 5px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 10px;
}
.btn-refresh:hover { background: var(--primary); }
.actions { display: flex; align-items: center; flex: 1; justify-content: flex-end; }
.btn:active { transform: scale(0.95); }
.btn-primary { background: var(--primary); color: white; }
.btn-success { background: var(--success); color: white; }
.btn-danger { background: var(--danger); color: white; }
.btn-secondary { background: #334155; color: white; }
.btn-validate { background: var(--success); color: white; width: 100%; margin-top: 1rem; }

/* NOTIFICATIONS */
.toast { position: fixed; bottom: 2rem; right: 2rem; padding: 1rem 2rem; border-radius: 12px; z-index: 200; font-weight: 600; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }
.toast.success { background: var(--success); color: white; }
.toast.error { background: var(--danger); color: white; }

/* MOBILE */
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar-open .sidebar { transform: translateX(0); }
  .main-content { margin-left: 0; }
  .menu-toggle { display: block; background: transparent; border: none; color: white; font-size: 1.5rem; }
  .sidebar-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 95; backdrop-filter: blur(4px); }
  .content-area { padding: 1rem; }
}

@media print {
  body * { display: none !important; }
  .printable-area, .printable-area * { display: block !important; }
  .printable-area { position: absolute; top: 0; width: 100%; text-align: center; }
}
</style>
