<script setup>
import { ref } from 'vue'

const INITIAL_ROWS = 6

function createReminder() {
  return { id: crypto.randomUUID(), text: '', done: false }
}

const reminders = ref(Array.from({ length: INITIAL_ROWS }, createReminder))

function addReminder() {
  reminders.value.push(createReminder())
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

    <button type="button" class="reminders__add" aria-label="Adicionar lembrete" @click="addReminder">
      +
    </button>
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

.reminders__add {
  display: block;
  margin: var(--space-md) 0 0 auto;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--color-text-muted);
  color: var(--color-on-primary);
  font-size: 1.25rem;
}
</style>