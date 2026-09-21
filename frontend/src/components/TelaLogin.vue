<template>
  <div class="login-container">
    <div class="login-card">
      <h2>Controle Alimentar - IFBA BDO</h2>
      <form @submit.prevent="handleLogin">
        <div class="input-group">
          <label for="username">Usuário</label>
          <input type="text" id="username" v-model="username" required placeholder="Digite o usuário" />
        </div>
        <div class="input-group">
          <label for="password">Senha</label>
          <input type="password" id="password" v-model="password" required placeholder="Digite a senha" />
        </div>
        <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
        <button type="submit">Entrar</button>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      username: '',
      password: '',
      errorMessage: ''
    }
  },
  methods: {
    handleLogin() {
      // Validação com as credenciais fixas exigidas
      if (this.username === 'ifbabdo123' && this.password === 'campusbdo321') {
        localStorage.setItem('isAuthenticated', 'true');
        this.errorMessage = '';
        this.$router.push('/alunos'); // Redireciona para Alunos após o login
      } else {
        this.errorMessage = 'Usuário ou senha incorretos!';
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #121212;
}
.login-card {
  background-color: #1e1e1e;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  color: #fff;
  width: 100%;
  max-width: 400px;
}
h2 { text-align: center; margin-bottom: 1.5rem; color: #2e7d32; }
.input-group { margin-bottom: 1.2rem; display: flex; flex-direction: column; }
label { margin-bottom: 0.5rem; font-size: 0.9rem; }
input { padding: 0.75rem; border-radius: 4px; border: 1px solid #333; background-color: #2a2a2a; color: #fff; }
button { width: 100%; padding: 0.75rem; border: none; border-radius: 4px; background-color: #2e7d32; color: #fff; font-weight: bold; cursor: pointer; transition: background 0.2s; }
button:hover { background-color: #1b5e20; }
.error-msg { color: #e53935; font-size: 0.85rem; margin-bottom: 1rem; text-align: center; }
</style>
