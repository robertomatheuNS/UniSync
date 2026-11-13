<script setup>
import { computed, ref } from 'vue'
import trashIcon from '@/assets/icons/trash.svg'

const MIN_ROWS = 6

function createReminder() {
  return { id: crypto.randomUUID(), text: '', done: false }
}

const reminders = ref(Array.from({ length: MIN_ROWS }, createReminder))

const hasCompleted = computed(() => reminders.value.some((reminder) => reminder.done))

function addReminder() {
  reminders.value.push(createReminder())
}

function fillEmptyRows() {
  while (reminders.value.length < MIN_ROWS) {
    reminders.value.push(createReminder())
  }
}

function removeCompleted() {
  reminders.value = reminders.value.filter((reminder) => !reminder.done)
  fillEmptyRows()
}
</script>

<template>
  <section class="reminders" aria-labelledby="reminders-title">
    <h2 id="reminders-title" class="reminders__title">Lembretes</h2>

    <ul class="reminders__list">
      <li v-for="(reminder, index) in reminders" :key="reminder.id" class="reminders__item">
        <input
          v-model="reminder.done"
          type="checkbox"
          class="reminders__check"
          :aria-label="`Concluir lembrete ${index + 1}`"
        />
        <input
          v-model="reminder.text"
          type="text"
          class="reminders__text"
          :class="{ 'reminders__text--done': reminder.done }"
          :aria-label="`Texto do lembrete ${index + 1}`"
        />
      </li>
    </ul>

    <div class="reminders__footer">
      <button
        type="button"
        class="reminders__trash"
        :disabled="!hasCompleted"
        aria-label="Apagar lembretes marcados"
        @click="removeCompleted"
      >
        <img :src="trashIcon" alt="" class="reminders__trash-icon icon" />
      </button>

      <button
        type="button"
        class="reminders__add"
        aria-label="Adicionar lembrete"
        @click="addReminder"
      >
        +
      </button>
    </div>
  </section>
</template>

<style scoped>
.reminders {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.reminders__title {
  margin-bottom: var(--space-md);
  color: var(--color-text-muted);
  font-size: 1.5rem;
  text-align: center;
  text-transform: uppercase;
}

.reminders__list {
  list-style: none;
  max-height: 18rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.reminders__item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.reminders__check {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
  accent-color: var(--color-text-muted);
}

.reminders__text {
  flex: 1;
  min-width: 0;
  border: none;
  border-bottom: 2px solid var(--color-text-muted);
  background: transparent;
  font: inherit;
}

.reminders__text--done {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.reminders__footer {
  margin-top: var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reminders__trash {
  padding: 0.25rem;
  border: none;
  background: none;
}

.reminders__trash:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.reminders__trash-icon {
  width: 1.75rem;
  height: 1.75rem;
}

.reminders__add {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--color-text-muted);
  color: var(--color-on-primary);
  font-size: 1.25rem;
}
</style>