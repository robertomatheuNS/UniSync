<script setup>
import { NAV_ITEMS } from '@/constants/navigation'
</script>

<template>
  <nav class="app-sidebar" aria-label="Menu principal">
    <ul class="app-sidebar__list">
      <li v-for="item in NAV_ITEMS" :key="item.routeName">
        <RouterLink
          :to="{ name: item.routeName }"
          class="app-sidebar__link"
          exact-active-class="app-sidebar__link--active"
        >
          <img :src="item.icon" alt="" class="app-sidebar__icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.app-sidebar {
  padding: var(--space-md) 0;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  background: var(--color-sidebar);
}

.app-sidebar__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.app-sidebar__link {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 700;
  text-decoration: none;
}

.app-sidebar__link--active {
  background: var(--color-card);
}

.app-sidebar__icon {
  width: 2rem;
  height: 2rem;
}
</style>