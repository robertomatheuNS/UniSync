<script setup>
import NoticeCard from '@/components/dashboard/NoticeCard.vue'

defineProps({
  notices: { type: Array, required: true },
  label: { type: String, default: 'Próximos avisos' },
})
</script>

<template>
  <section class="notice-list" :aria-label="label">
    <ul class="notice-list__grid">
      <li v-for="notice in notices" :key="notice.id">
        <NoticeCard :notice="notice">
          <template #actions>
            <slot name="actions" :notice="notice" />
          </template>
        </NoticeCard>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.notice-list {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-sidebar);
}

.notice-list__grid {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(11rem, 1fr));
  gap: var(--space-md);
}
</style>