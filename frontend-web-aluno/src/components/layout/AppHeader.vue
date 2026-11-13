<script setup>
import logo from '@/assets/images/logo.gif'
import avatar from '@/assets/images/avatar.jpg'
import bellIcon from '@/assets/icons/bell.svg'
import { logout } from '@/services/shellAuth'

const NOTIFICATION_COUNT = 1
</script>

<template>
  <header class="app-header">
    <img
      :src="logo"
      alt="UniSync"
      class="app-header__logo"
    />

    <div class="app-header__actions">
      <button
        type="button"
        class="app-header__bell"
        aria-label="Notificações"
      >
        <img
          :src="bellIcon"
          alt=""
          class="app-header__bell-icon"
        />

        <span
          v-if="NOTIFICATION_COUNT > 0"
          class="app-header__badge"
        >
          {{ NOTIFICATION_COUNT }}
        </span>
      </button>

      <img
        :src="avatar"
        alt="Foto do usuário"
        class="app-header__avatar"
      />

      <button
        type="button"
        class="app-header__logout"
        @click="logout"
      >
        Sair
      </button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-header);
}

.app-header__logo {
  height: 4rem;
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
  cursor: pointer;
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

.app-header__logout {
  padding: 0.6rem 1rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: var(--color-on-primary);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.app-header__logout:hover {
  opacity: 0.85;
}
</style>