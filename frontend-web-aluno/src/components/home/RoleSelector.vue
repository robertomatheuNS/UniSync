<script setup>
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()

function selectRole(role) {
  router.push({ name: 'register', query: { perfil: role } })
}
</script>

<template>
  <section class="role-selector">
    <h2 class="role-selector__title">Eu sou</h2>
    <div class="role-selector__options">
      <BaseButton @click="selectRole('professor')">Professor</BaseButton>
      <BaseButton @click="selectRole('aluno')">Aluno</BaseButton>
    </div>
  </section>
</template>

<style scoped>
.role-selector {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  background: var(--gradient-landing);
}

.role-selector__title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  font-weight: 400;
}

.role-selector__options {
  display: flex;
  gap: var(--space-md);
}
</style>