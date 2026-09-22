<template>
  <div class="login-obrigatorio-container" style="display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #0b1611; color: #fff; width: 100vw; position: fixed; inset: 0; z-index: 9999;">
    <div class="card glass-effect" style="max-width: 400px; width: 100%; padding: 2rem; background: #121e17; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.5); border: 1px solid #10b981;">
      
      <div style="text-align: center; margin-bottom: 1.5rem;">
        <img src="/ifba-logo.png" style="height: 60px; margin-bottom: 0.5rem;" alt="IFBA" />
        <h2 style="color: #10b981; margin: 0; font-size: 1.5rem; font-family: 'Outfit', sans-serif; font-weight: 700;">SISTEMA IFBA</h2>
        <small style="color: #94a3b8; font-family: 'Outfit', sans-serif;">Controle Alimentar · Campus Brumado</small>
      </div>

      <div style="display: flex; flex-direction: column; gap: 1.2rem;">
        <div style="display: flex; flex-direction: column; gap: 0.5rem;">
          <label style="font-size: 0.9rem; color: #cbd5e1; font-family: 'Outfit', sans-serif; font-weight: 500;">Usuário Administrativo:</label>
          <input type="text" v-model="usernameInput" placeholder="ifbabdo123" style="padding: 0.75rem; background: #1e2922; border: 1px solid #2d3748; color: #fff; border-radius: 4px; font-size: 1rem; width: 100%; outline: none;" />
        </div>
        
        <div style="display: flex; flex-direction: column; gap: 0.5rem;">
          <label style="font-size: 0.9rem; color: #cbd5e1; font-family: 'Outfit', sans-serif; font-weight: 500;">Senha de Acesso:</label>
          <input type="password" v-model="passwordInput" placeholder="••••••••" @keyup.enter="efetuarLoginAdmin" style="padding: 0.75rem; background: #1e2922; border: 1px solid #2d3748; color: #fff; border-radius: 4px; font-size: 1rem; width: 100%; outline: none;" />
        </div>

        <!-- MENSAGEM EM VERMELHO CASO ERRE -->
        <p v-if="loginError" style="color: #ef4444; font-size: 0.9rem; font-weight: bold; text-align: center; margin: 0; padding: 0.5rem; background: rgba(239, 68, 68, 0.1); border-radius: 4px; font-family: 'Outfit', sans-serif;">
          ⚠️ {{ loginError }}
        </p>

        <button @click="efetuarLoginAdmin" style="width: 100%; padding: 0.85rem; background: #10b981; color: #fff; font-weight: bold; border: none; border-radius: 4px; cursor: pointer; font-size: 1rem; font-family: 'Outfit', sans-serif; transition: background 0.2s;">
          Entrar no Sistema
        </button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const usernameInput = ref('');
const passwordInput = ref('');
const loginError = ref('');

function efetuarLoginAdmin() {
  // .value lê exatamente o texto inserido nos campos
  if (usernameInput.value === 'ifbabdo123' && passwordInput.value === 'campusbdo321') {
    localStorage.setItem('isAuthenticated', 'true');
    loginError.value = '';
    window.location.href = '/'; // Recarrega a aplicação e destranca o App.vue com sucesso
  } else {
    loginError.value = 'Usuário ou senha inválidos para o Campus Brumado!';
  }
}
</script>
