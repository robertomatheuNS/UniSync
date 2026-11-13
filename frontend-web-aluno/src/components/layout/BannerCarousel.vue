<script setup>
import { computed } from 'vue'
import PromoBanner from '@/components/layout/PromoBanner.vue'
import { BANNER_SLIDES } from '@/data/bannerSlides'
import { useCarousel } from '@/composables/useCarousel'

const { currentIndex, isPaused, goTo, pause, resume } = useCarousel(BANNER_SLIDES.length)

const currentSlide = computed(() => BANNER_SLIDES[currentIndex.value])
</script>

<template>
  <section
    class="banner-carousel"
    aria-roledescription="carousel"
    aria-label="Destaques"
    @mouseenter="pause"
    @mouseleave="resume"
    @focusin="pause"
    @focusout="resume"
  >
    <div class="banner-carousel__viewport" :aria-live="isPaused ? 'polite' : 'off'">
      <Transition name="banner-fade" mode="out-in">
        <PromoBanner :key="currentSlide.id" :slide="currentSlide" />
      </Transition>
    </div>

    <div class="banner-carousel__dots">
      <button
        v-for="(slide, index) in BANNER_SLIDES"
        :key="slide.id"
        type="button"
        class="banner-carousel__dot"
        :class="{ 'banner-carousel__dot--active': index === currentIndex }"
        :aria-label="`Ir para o destaque ${index + 1}`"
        :aria-current="index === currentIndex"
        @click="goTo(index)"
      />
    </div>
  </section>
</template>

<style scoped>
.banner-carousel {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.banner-carousel__viewport {
  min-height: var(--banner-height);
}

.banner-carousel__dots {
  display: flex;
  justify-content: center;
  gap: var(--space-sm);
}

.banner-carousel__dot {
  width: 3.5rem;
  height: 0.75rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-card);
}

.banner-carousel__dot--active {
  background: var(--color-text);
}

.banner-fade-enter-active,
.banner-fade-leave-active {
  transition: opacity 0.3s;
}

.banner-fade-enter-from,
.banner-fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .banner-fade-enter-active,
  .banner-fade-leave-active {
    transition: none;
  }
}
</style>