import { createRouter, createWebHistory } from 'vue-router'
import AppDashboard from './App.vue'
import Login from './components/Login.vue' 
const routes = [
  { 
    path: '/', 
    component: AppDashboard,

    children: [
      { path: 'validacao', component: AppDashboard },
      { path: 'alunos', component: AppDashboard },
      { path: 'estoque', component: AppDashboard },
      { path: 'prato-do-dia', component: AppDashboard },
      { path: 'alertas', component: AppDashboard }
    ]
  },
  { path: '/login', component: Login },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


router.beforeEach((to, from, next) => {
  const loggedIn = localStorage.getItem('isAuthenticated') === 'true';
  const rotasRestritas = ['/alunos', '/estoque', '/alertas'];

  if (rotasRestritas.includes(to.path) && !loggedIn) {
    next('/login');
  } else {
    next();
  }
})

export default router;
