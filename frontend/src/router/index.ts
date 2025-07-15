import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/trips',
      name: 'trips',
      component: () => import('../views/TripListView.vue'),
    },
    {
      path: '/charge',
      name: 'charge',
      component: () => import('../views/ChargeView.vue'),
    },
    {
      path: '/driving-stats',
      name: 'driving-stats',
      component: () => import('../views/DrivingStatsView.vue'),
    },
    {
      path: '/track-stats',
      name: 'track-stats',
      component: () => import('../views/TrackStatsView.vue'),
    },
    {
      path: '/data-features',
      name: 'data-features',
      component: () => import('../views/DataFeaturesView.vue'),
    },
  ],
})

export default router
