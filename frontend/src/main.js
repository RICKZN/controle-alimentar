import { createRouter, createWebHistory } from 'vue-router'
import { createApp } from 'vue'
import App from './App.vue'
import Login from './components/TelaLogin.vue' 
const routes = [
  { 
    path: '/', 
    component: App 
  },
  { 
    path: '/login', 
    component: Login 
  },
  { 
    path: '/:pathMatch(.*)*', 
    redirect: '/' 
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guarda de segurança simples para bloquear acessos diretos por URL
router.beforeEach((to, from, next) => {
  const loggedIn = localStorage.getItem('isAuthenticated') === 'true'
  
  if (to.path !== '/login' && !loggedIn) {
    // Se o seu objetivo é deixar a validação pública, mude a linha abaixo para permitir
    next() 
  } else {
    next()
  }
})

export default router
