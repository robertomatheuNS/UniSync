<script setup>
defineProps({
  slide: { type: Object, required: true },
})
</script>

<template>
  <article class="promo-banner" :style="{ backgroundImage: `url(${slide.image})` }">
    <h2 class="promo-banner__title">
      <span class="promo-banner__accent">{{ slide.accent }}</span>
      {{ slide.title }}
    </h2>
    <p class="promo-banner__subtitle">
      {{ slide.subtitle }}
      <!-- TODO: levar o botão para a página de destino quando ela existir -->
      <button type="button" class="promo-banner__cta">{{ slide.ctaLabel }}</button>
    </p>
  </article>
</template>

<style scoped>
.promo-banner {
  min-height: var(--banner-height);
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-sm);
  border-radius: var(--radius-lg);
  background-size: cover;
  background-position: center;
  color: var(--color-white);
}

.promo-banner__title {
  font-size: 3rem;
  line-height: 0.95;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.promo-banner__accent {
  display: block;
  width: fit-content;
  background: linear-gradient(90deg, var(--color-accent), #4fd1c5);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: none;
}

.promo-banner__subtitle {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.5rem;
  font-weight: 700;
  text-transform: uppercase;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.promo-banner__cta {
  margin-left: var(--space-sm);
  padding: 0.2rem 0.8rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-highlight);
  color: var(--color-white);
  font-family: var(--font-main);
  font-weight: 700;
}
</style>