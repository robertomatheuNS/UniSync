<script setup>
import bannerImage from '@/assets/images/focus-banner.jpg'
</script>

<template>
  <section
    class="promo-banner"
    :style="{ backgroundImage: `url(${bannerImage})` }"
    aria-label="Destaque"
  >
    <h2 class="promo-banner__title">
      <span class="promo-banner__accent">Semana</span> De Foco&gt;
    </h2>
    <p class="promo-banner__subtitle">
      Dicas de estudos
      <button type="button" class="promo-banner__cta">AQUI</button>
    </p>
  </section>
</template>

<style scoped>
.promo-banner {
  min-height: 8rem;
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: var(--radius-lg);
  background-size: cover;
  background-position: center;
  color: var(--color-on-primary);
}

.promo-banner__title {
  font-size: 2.5rem;
}

.promo-banner__accent {
  color: var(--color-accent);
}

.promo-banner__cta {
  margin-left: var(--space-sm);
  padding: 0.2rem 0.8rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-highlight);
  color: var(--color-on-primary);
  font-weight: 700;
}
</style>