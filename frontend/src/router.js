import { createRouter, createWebHistory } from 'vue-router'
import store from './store'
import Login from './views/Login.vue'
import Layout from './views/Layout.vue'
import Dashboard from './views/Dashboard.vue'
import Customers from './views/Customers.vue'
import Requests from './views/Requests.vue'
import Beds from './views/Beds.vue'
import CareItems from './views/CareItems.vue'
import CareLevels from './views/CareLevels.vue'
import CustomerServices from './views/CustomerServices.vue'
import NursingRecords from './views/NursingRecords.vue'
import Managers from './views/Managers.vue'
import Users from './views/Users.vue'
import MyCustomers from './views/MyCustomers.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', redirect: '/login' },
        { path: 'dashboard', component: Dashboard },
        { path: 'customers', component: Customers },
        { path: 'outing', component: Requests, props: { type: 'outing' } },
        { path: 'checkout', component: Requests, props: { type: 'checkout' } },
        { path: 'beds', component: Beds, props: { mode: 'overview' } },
        { path: 'bed-usage', component: Beds, props: { mode: 'usage' } },
        { path: 'care-items', component: CareItems },
        { path: 'care-levels', component: CareLevels },
        { path: 'services', component: CustomerServices },
        { path: 'records', component: NursingRecords },
        { path: 'managers', component: Managers },
        { path: 'my-customers', component: MyCustomers },
        { path: 'users', component: Users }
      ]
    }
  ]
})

router.beforeEach(to => {
  if (to.path !== '/login' && !store.getters.loggedIn) return '/login'
  return true
})

export default router
