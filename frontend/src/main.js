import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

import { createRouter, createWebHistory } from 'vue-router'
import Login from './components/Login.vue'
import Alunos from './components/Alunos.vue' // Seu componente existente
import Estoque from './components/Estoque.vue' // Seu componente existente
import PratoDia from './components/PratoDia.vue' // Tela pública do Prato do Dia

const routes = [
  { path: '/login', component: Login },
  { path: '/prato-do-dia', component: PratoDia }, // Alunos usam esta sem login administrativo
  { path: '/alunos', component: Alunos, meta: { requiresAuth: true } },
  { path: '/estoque', component: Estoque, meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/prato-do-dia' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guarda de segurança que valida o acesso
router.beforeEach((to, from, next) => {
  const loggedIn = localStorage.getItem('isAuthenticated') === 'true';

  if (to.matched.some(record => record.meta.requiresAuth) && !loggedIn) {
    next('/login'); // Se não estiver logado, barra e joga pro login
  } else {
    next(); // Se estiver logado ou for rota pública (Prato do Dia), permite
  }
})

export default router;

createApp(App).mount('#app')
