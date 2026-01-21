import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'layout',
      component: () => import('../views/Layout.vue'),
      redirect: '/jobs',
      children: [
        {
          path: '/jobs',
          name: 'jobs',
          component: () => import('../views/Jobs.vue')
        },
        {
          path: '/workflows',
          name: 'workflows',
          component: () => import('../views/Workflows.vue')
        },
        {
          path: '/workflow/edit/:id?',
          name: 'workflow-edit',
          component: () => import('../views/WorkflowEditor.vue')
        },
        {
          path: '/instances',
          name: 'instances',
          component: () => import('../views/Instances.vue')
        },
        {
          path: '/monitor',
          name: 'monitor',
          component: () => import('../views/Monitor.vue')
        }
      ]
    }
  ]
})

export default router
