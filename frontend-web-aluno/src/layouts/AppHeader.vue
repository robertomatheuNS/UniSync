<script setup>
import logo from '@/assets/images/logo.jpg'
import avatar from '@/assets/images/avatar.jpg'
import bellIcon from '@/assets/icons/bell.svg'

// TODO: substituir por dados reais quando o back-end existir
const NOTIFICATION_COUNT = 1
</script>

<template>
  <header class="app-header">
    <img :src="logo" alt="UniSync" class="app-header__logo" />

    <div class="app-header__actions">
      <button type="button" class="app-header__bell" aria-label="Notificações">
        <img :src="bellIcon" alt="" class="app-header__bell-icon" />
        <span v-if="NOTIFICATION_COUNT > 0" class="app-header__badge">
          {{ NOTIFICATION_COUNT }}
        </span>
      </button>
      <img :src="avatar" alt="Foto do usuário" class="app-header__avatar" />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  padding: 0 var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-header);
}

.app-header__logo {
  height: 3rem;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.app-header__bell {
  position: relative;
  border: none;
  background: none;
}

.app-header__bell-icon {
  width: 2rem;
  height: 2rem;
}

.app-header__badge {
  position: absolute;
  top: -0.25rem;
  right: -0.25rem;
  min-width: 1.1rem;
  padding: 0 0.25rem;
  border-radius: var(--radius-pill);
  background: var(--color-error);
  color: var(--color-on-primary);
  font-size: 0.7rem;
  text-align: center;
}

.app-header__avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  object-fit: cover;
}
</style>