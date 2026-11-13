<script setup>
import noticeIcon from '@/assets/icons/notice.svg'
import { formatDaysLeft } from '@/utils/formatDaysLeft'

defineProps({
  notice: { type: Object, required: true },
})
</script>

<template>
  <article class="notice-card">
    <header class="notice-card__header">
      <img :src="noticeIcon" alt="" class="notice-card__icon" />
      <h3 class="notice-card__type">{{ notice.type }}</h3>
      <span class="notice-card__deadline">{{ notice.past ? 'Encerrada' : formatDaysLeft(notice.daysLeft) }}</span>
    </header>

    <div>
      <p class="notice-card__title" :title="notice.title">{{ notice.title }}</p>
      <p class="notice-card__teacher">{{ notice.teacher }}</p>
    </div>
  </article>
</template>

<style scoped>
.notice-card {
  min-height: 6.5rem;
  padding: var(--space-sm);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text-muted);
}

.notice-card__header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.notice-card__icon {
  width: 1.25rem;
  height: 1.25rem;
}

.notice-card__type {
  font-size: 0.9rem;
}

.notice-card__deadline {
  margin-left: auto;
  font-size: 0.8rem;
  font-weight: 700;
}

.notice-card__title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-card__teacher {
  font-size: 0.75rem;
}
</style>
