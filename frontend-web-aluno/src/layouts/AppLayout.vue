<script setup>
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppCalendar from '@/components/layout/AppCalendar.vue'
import RemindersPanel from '@/components/layout/RemindersPanel.vue'
import BannerCarousel from '@/components/layout/BannerCarousel.vue'
import { ref } from 'vue'

const isSidebarCollapsed = ref(false)
</script>

<template>
  <div class="app-layout" :class="{ 'app-layout--collapsed': isSidebarCollapsed }">
  <AppHeader class="app-layout__header" />
  <AppSidebar v-model:collapsed="isSidebarCollapsed" class="app-layout__sidebar" />

    <main class="app-layout__content">
      <BannerCarousel />
      <RouterView />
    </main>

    <aside class="app-layout__aside">
      <AppCalendar />
      <RemindersPanel />
    </aside>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr) minmax(12rem, 15rem);
  grid-template-rows: var(--header-height) 1fr;
  grid-template-areas:
    'header header header'
    'sidebar content aside';
  gap: var(--space-md);
}

.app-layout__header {
  grid-area: header;
}

.app-layout__sidebar {
  grid-area: sidebar;
  margin-bottom: var(--space-md);
}

.app-layout__content {
  min-width: 0;
  grid-area: content;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.app-layout__aside {
  grid-area: aside;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding-right: var(--space-md);
}

@media (max-width: 960px) {
  .app-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    grid-template-areas:
      'header'
      'sidebar'
      'content'
      'aside';
  }

  .app-layout__aside {
    padding: 0 var(--space-md);
  }
  .app-layout__content { padding: 0 var(--space-md); }
}
.app-layout--collapsed {
  --sidebar-width: 5rem;
}
</style>
