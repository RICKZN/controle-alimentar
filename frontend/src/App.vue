<template>
  <div class="app-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- Overlay para mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">CONTROLE<span>IF</span></div>
      </div>
      <nav class="sidebar-nav">
        <button 
          @click="currentTab = 'validacao'; isSidebarOpen = false" 
          :class="{ active: currentTab === 'validacao' }"
        >
          <span class="icon">📷</span> Validação
        </button>
        <button 
          @click="currentTab = 'estoque'; isSidebarOpen = false" 
          :class="{ active: currentTab === 'estoque' }"
        >
          <span class="icon">📦</span> Estoque
        </button>
        <button 
          @click="currentTab = 'geracao'; isSidebarOpen = false" 
          :class="{ active: currentTab === 'geracao' }"
        >
          <span class="icon">🎟️</span> Gerar Fichas
        </button>
      </nav>
      <div class="sidebar-footer">
        <p>v2.0 - Nuvem</p>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="top-bar">
        <button class="menu-toggle" @click="isSidebarOpen = true">☰</button>
        <h1>{{ tabTitles[currentTab] }}</h1>
        <div class="user-status">Online</div>
      </header>

      <div class="content-area">
        <!-- TELA DE VALIDAÇÃO (QR CODE) -->
        <div v-if="currentTab === 'validacao'" class="tab-pane">
          <div class="scanner-container card glass-effect">
            <div id="reader" class="scanner-preview"></div>
            
            <!-- Botão de Validação que aparece após o Scan -->
            <div v-if="matriculaLida" class="validation-confirm-overlay">
              <div class="confirm-card">
                <h3>Estudante Detectado!</h3>
                <div class="matricula-badge">{{ matriculaLida }}</div>
                <button @click="confirmarValidacao" class="btn btn-validate pulse">VALIDAR ACESSO</button>
                <button @click="cancelarLeitura" class="btn-cancel">Cancelar</button>
              </div>
            </div>

            <div v-if="!matriculaLida" class="scanner-instructions">
              <div class="scan-target"></div>
              <p>Posicione o QR Code no centro</p>
            </div>
          </div>

          <div class="manual-input-card card glass-effect">
            <h3>Entrada Manual</h3>
            <div class="input-group-row">
              <input type="text" v-model="matriculaParaValidar" placeholder="Digite a matrícula" />
              <button @click="validarFicha(matriculaParaValidar)" class="btn btn-primary">Validar</button>
            </div>
          </div>

          <div v-if="mensagem" :class="['alert-toast', mensagemTipo]">
            {{ mensagem }}
          </div>
        </div>

        <!-- TELA DE ESTOQUE -->
        <div v-if="currentTab === 'estoque'" class="tab-pane">
          <div class="estoque-grid">
            <div v-for="item in estoqueList" :key="item.id" class="estoque-card glass-effect">
              <div class="item-header">
                <h3>{{ item.nome }}</h3>
                <span class="unit-tag">{{ item.unidade }}</span>
              </div>
              <div class="item-body">
                <button @click="ajustarEstoque(item.id, -1)" class="adjust-btn minus">-</button>
                <div class="qty-display">
                  <span class="qty-value">{{ formatarQuantidade(item.quantidade) }}</span>
                </div>
                <button @click="ajustarEstoque(item.id, 1)" class="adjust-btn plus">+</button>
              </div>
            </div>
          </div>
        </div>

        <!-- TELA DE GERAÇÃO -->
        <div v-if="currentTab === 'geracao'" class="tab-pane">
          <div class="card glass-effect">
            <div class="input-group">
              <label>Número de Matrícula</label>
              <input type="text" v-model="matriculaParaGerar" placeholder="Ex: 202410099" />
              <button @click="gerarQrCode" class="btn btn-primary mt-1">Gerar QR Code</button>
            </div>
            
            <div v-if="qrCodeGerado" class="qr-result">
              <qrcode-vue :value="matriculaParaGerar" :size="200" level="H" class="qr-code-img" />
              <div class="qr-info">
                <strong>Matrícula:</strong> {{ matriculaParaGerar }}
              </div>
              <button @click="imprimirFicha" class="btn btn-secondary">Imprimir Ficha</button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { Html5QrcodeScanner } from 'html5-qrcode';

const API_URL = import.meta.env.VITE_API_URL || '/api';

// Estado da UI
const currentTab = ref('validacao');
const isSidebarOpen = ref(false);
const tabTitles = {
  validacao: 'Validar Estudante',
  estoque: 'Gerenciar Estoque',
  geracao: 'Emitir Fichas'
};

// Dados
const estoqueList = ref([]);
const matriculaLida = ref(null);
const matriculaParaValidar = ref('');
const matriculaParaGerar = ref('');
const qrCodeGerado = ref(false);
const mensagem = ref('');
const mensagemTipo = ref('');

let scanner = null;

const carregarEstoque = async () => {
  try {
    const res = await axios.get(`${API_URL}/estoque`);
    estoqueList.value = res.data;
  } catch (err) { console.error(err); }
};

const ajustarEstoque = async (id, delta) => {
  try {
    await axios.post(`${API_URL}/estoque/${id}/ajustar?variacao=${delta}`);
    await carregarEstoque();
  } catch (err) { console.error(err); }
};

const onScanSuccess = (decodedText) => {
  if (matriculaLida.value) return; // Evita scans múltiplos
  matriculaLida.value = decodedText;
  // Feedback tátil no celular
  if (navigator.vibrate) navigator.vibrate(100);
};

const confirmarValidacao = async () => {
  await validarFicha(matriculaLida.value);
  matriculaLida.value = null;
};

const cancelarLeitura = () => {
  matriculaLida.value = null;
};

const validarFicha = async (matricula) => {
  if (!matricula) return;
  try {
    const res = await axios.post(`${API_URL}/validar?matricula=${matricula}`);
    mostrarMensagem(res.data.message, 'success');
    matriculaParaValidar.value = '';
    carregarEstoque(); // Atualiza estoque se houver baixa automática
  } catch (err) {
    mostrarMensagem(err.response?.data?.error || "Erro na validação", 'error');
  }
};

const mostrarMensagem = (txt, tipo) => {
  mensagem.value = txt;
  mensagemTipo.value = tipo;
  setTimeout(() => mensagem.value = '', 4000);
};

const gerarQrCode = () => {
  if (!matriculaParaGerar.value) return;
  qrCodeGerado.value = true;
};

const imprimirFicha = () => window.print();

const formatarQuantidade = (q) => Number.isInteger(q) ? q : q.toFixed(1);

const initScanner = () => {
  if (scanner) scanner.clear();
  scanner = new Html5QrcodeScanner("reader", { 
    fps: 15, 
    qrbox: { width: 250, height: 250 },
    aspectRatio: 1.0,
    showTorchButtonIfSupported: true
  }, false);
  scanner.render(onScanSuccess, () => {});
};

onMounted(() => {
  carregarEstoque();
  if (currentTab.value === 'validacao') initScanner();
});

watch(currentTab, (newTab) => {
  if (newTab === 'validacao') {
    setTimeout(initScanner, 100);
  } else if (scanner) {
    scanner.clear();
  }
});

onUnmounted(() => {
  if (scanner) scanner.clear();
});
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap');

:root {
  --sidebar-width: 280px;
  --primary: #3b82f6;
  --success: #10b981;
  --danger: #ef4444;
  --bg-dark: #0f172a;
  --card-glass: rgba(30, 41, 59, 0.7);
  --border-glass: rgba(255, 255, 255, 0.1);
}

* { box-sizing: border-box; }

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-dark);
  color: #f8fafc;
  margin: 0;
  overflow-x: hidden;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

/* SIDEBAR STYLES */
.sidebar {
  width: var(--sidebar-width);
  background: #1e293b;
  border-right: 1px solid var(--border-glass);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  z-index: 100;
  transition: transform 0.3s ease;
}

.sidebar-header {
  padding: 2rem;
  text-align: center;
}

.logo {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -1px;
}

.logo span { color: var(--primary); }

.sidebar-nav {
  padding: 1rem;
  flex: 1;
}

.sidebar-nav button {
  width: 100%;
  padding: 1rem;
  margin-bottom: 0.5rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  text-align: left;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;
}

.sidebar-nav button.active {
  background: var(--primary);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.sidebar-nav button:hover:not(.active) {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

/* MAIN CONTENT STYLES */
.main-content {
  flex: 1;
  margin-left: var(--sidebar-width);
  min-width: 0;
}

.top-bar {
  height: 70px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  border-bottom: 1px solid var(--border-glass);
  position: sticky;
  top: 0;
  z-index: 90;
}

.top-bar h1 { font-size: 1.25rem; margin: 0; }

.menu-toggle {
  display: none;
  background: transparent;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
}

.content-area {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.card {
  background: var(--card-glass);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-glass);
  border-radius: 20px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}

/* SCANNER STYLES */
.scanner-container {
  position: relative;
  overflow: hidden;
  padding: 0;
  aspect-ratio: 1;
  max-width: 500px;
  margin: 0 auto 2rem;
}

.scanner-preview {
  width: 100% !important;
  height: 100% !important;
  border: none !important;
}

#reader__dashboard_section_csr button {
  background: var(--primary) !important;
  color: white !important;
  border-radius: 8px !important;
  padding: 8px 16px !important;
  cursor: pointer;
}

.validation-confirm-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  animation: fadeIn 0.2s ease;
}

.confirm-card {
  text-align: center;
  padding: 2rem;
}

.matricula-badge {
  font-size: 2rem;
  font-weight: 800;
  color: var(--primary);
  margin: 1rem 0 2rem;
}

.btn-validate {
  background: var(--success);
  color: white;
  font-size: 1.25rem;
  padding: 1.5rem 2.5rem;
  width: 100%;
  border-radius: 16px;
  border: none;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4);
}

.pulse { animation: pulseAnim 1.5s infinite; }

.btn-cancel {
  background: transparent;
  border: none;
  color: #94a3b8;
  margin-top: 1.5rem;
  cursor: pointer;
  text-decoration: underline;
}

/* ESTOQUE GRID */
.estoque-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.estoque-card {
  padding: 1.5rem;
  border-radius: 16px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.unit-tag {
  background: rgba(255,255,255,0.1);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  text-transform: uppercase;
}

.item-body {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
}

.adjust-btn {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: white;
  transition: transform 0.1s;
}

.adjust-btn:active { transform: scale(0.9); }
.adjust-btn.minus { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid #ef4444; }
.adjust-btn.plus { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid #10b981; }

.qty-display {
  font-size: 1.5rem;
  font-weight: 700;
  min-width: 80px;
  text-align: center;
}

/* UTILS */
.btn-primary { background: var(--primary); color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 8px; cursor: pointer; }
.alert-toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  padding: 1rem 2rem;
  border-radius: 12px;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}
.alert-toast.success { background: var(--success); color: white; }
.alert-toast.error { background: var(--danger); color: white; }

@keyframes pulseAnim {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }

/* MOBILE RESPONSIVE */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }
  .sidebar-open .sidebar {
    transform: translateX(0);
  }
  .main-content { margin-left: 0; }
  .menu-toggle { display: block; }
  .top-bar { padding: 0 1rem; }
  .sidebar-overlay {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    z-index: 95;
  }
}
</style>
