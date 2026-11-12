<script setup>
import { NAV_ITEMS } from '@/constants/navigation'
import settingsIcon from '@/assets/icons/settings.svg'

const collapsed = defineModel('collapsed', { type: Boolean, default: false })
</script>

<template>
  <nav
    class="app-sidebar"
    :class="{ 'app-sidebar--collapsed': collapsed }"
    aria-label="Menu principal"
  >
    <button
      type="button"
      class="app-sidebar__toggle"
      :aria-label="collapsed ? 'Expandir menu' : 'Recolher menu'"
      :aria-expanded="!collapsed"
      @click="collapsed = !collapsed"
    >
      ◀
    </button>

    <ul class="app-sidebar__list">
      <li v-for="item in NAV_ITEMS" :key="item.routeName">
        <RouterLink
          :to="{ name: item.routeName }"
          class="app-sidebar__link"
          exact-active-class="app-sidebar__link--active"
        >
          <img :src="item.icon" alt="" class="app-sidebar__icon icon" />
          <span class="app-sidebar__label">{{ item.label }}</span>
        </RouterLink>
      </li>
    </ul>

    <!-- TODO: ligar à tela de configurações quando ela existir -->
    <button type="button" class="app-sidebar__settings" aria-label="Configurações">
      <img :src="settingsIcon" alt="" class="app-sidebar__icon icon" />
    </button>
  </nav>
</template>

<style scoped>
.app-sidebar {
  padding: var(--space-md) 0;
  display: flex;
  flex-direction: column;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  background: var(--color-sidebar);
}

.app-sidebar__toggle {
  align-self: flex-end;
  width: 1.5rem;
  height: 1.5rem;
  margin: 0 var(--space-sm) var(--space-sm) 0;
  border: none;
  border-radius: 0.4rem;
  background: var(--color-card);
  color: var(--color-white);
  font-size: 0.7rem;
}

.app-sidebar__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.app-sidebar__link {
  position: relative;
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
  flex-shrink: 0;
}

.app-sidebar__label {
  flex: 1;
  text-align: center;
}

.app-sidebar__settings {
  margin-top: auto;
  align-self: flex-start;
  padding: var(--space-sm);
  border: none;
  background: none;
}

.app-sidebar--collapsed .app-sidebar__toggle {
  align-self: center;
  margin-right: 0;
  transform: rotate(180deg);
}

.app-sidebar--collapsed .app-sidebar__link {
  justify-content: center;
}

/* Esconde o texto só visualmente: leitores de tela continuam lendo */
.app-sidebar--collapsed .app-sidebar__label {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}
</style>