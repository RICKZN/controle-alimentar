import { createRouter, createWebHistory } from 'vue-router'
import { createApp, h } from 'vue'
import App from './App.vue'
import Login from './components/Login.vue'

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

// Guarda de segurança global do Campus Brumado
router.beforeEach((to, from, next) => {
  const loggedIn = localStorage.getItem('isAuthenticated') === 'true'
  
  if (to.path !== '/login' && !loggedIn) {
    next('/login')
  } else {
    next()
  }
})

// COMPONENTE ANÔNIMO DE ENTRADA: Ignora erros de tags de casca de arquivos externos
const RootComponent = {
  render() {
    return h('div', { id: 'app' }, [h(router.currentRoute.value.matched[0]?.component || Login)])
  }
}

const app = createApp(RootComponent)
app.use(router)
app.mount('#app')
