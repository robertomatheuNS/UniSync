<script setup>
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import studentsImage from '@/assets/images/students-looking-down.jpg'
import groupStudyImage from '@/assets/images/group-study.jpg'

const router = useRouter()

function goToRegister() {
  router.push({ name: 'register' })
}
</script>

<template>
  <section class="hero">
    <div class="hero__top">
      <h1 class="hero__title">Veja na prática</h1>
      <nav class="hero__actions" aria-label="Acesso">
        <BaseButton>Login</BaseButton>
        <BaseButton @click="goToRegister">Cadastre-se</BaseButton>
      </nav>
    </div>

    <div class="hero__bottom">
      <p class="hero__brand">UNISYNC</p>
      <div class="hero__images">
        <img :src="studentsImage" alt="Estudantes sorrindo vistos de baixo" />
        <img :src="groupStudyImage" alt="Grupo de estudantes usando notebook" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  min-height: 100vh;
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: var(--gradient-landing);
}

.hero__top,
.hero__bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.hero__bottom {
  align-items: flex-end;
}

.hero__title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  font-weight: 400;
}

.hero__actions,
.hero__images {
  display: flex;
  gap: var(--space-md);
}

.hero__brand {
  font-size: 2rem;
  letter-spacing: 0.05em;
}

.hero__images img {
  height: 14rem;
  border-radius: var(--radius-md);
  object-fit: cover;
}
</style>