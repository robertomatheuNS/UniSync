<script setup>
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppCalendar from '@/components/layout/AppCalendar.vue'
import RemindersPanel from '@/components/layout/RemindersPanel.vue'
import PromoBanner from '@/components/layout/PromoBanner.vue'
</script>

<template>
  <div class="app-layout">
    <AppHeader class="app-layout__header" />
    <AppSidebar class="app-layout__sidebar" />

    <main class="app-layout__content">
      <PromoBanner />
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
  grid-template-columns: var(--sidebar-width) 1fr var(--aside-width);
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
}

.app-layout__content {
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
}
</style>