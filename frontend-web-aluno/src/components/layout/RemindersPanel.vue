<script setup>
import { ref } from 'vue'

const reminders = ref([])
const newText = ref('')

function addReminder() {
  const text = newText.value.trim()
  if (!text) return

  reminders.value.push({ id: crypto.randomUUID(), text, done: false })
  newText.value = ''
}
</script>

<template>
  <section class="reminders" aria-labelledby="reminders-title">
    <h2 id="reminders-title" class="reminders__title">Lembretes</h2>

    <ul class="reminders__list">
      <li v-for="reminder in reminders" :key="reminder.id" class="reminders__item">
        <label>
          <input v-model="reminder.done" type="checkbox" />
          <span :class="{ 'reminders__text--done': reminder.done }">{{ reminder.text }}</span>
        </label>
      </li>
    </ul>

    <form class="reminders__form" @submit.prevent="addReminder">
      <input
        v-model="newText"
        type="text"
        class="reminders__input"
        placeholder="Novo lembrete"
        aria-label="Novo lembrete"
      />
      <button type="submit" class="reminders__add" aria-label="Adicionar lembrete">+</button>
    </form>
  </section>
</template>

<style scoped>
.reminders {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.reminders__title {
  margin-bottom: var(--space-sm);
  color: var(--color-text-muted);
  text-align: center;
}

.reminders__list {
  list-style: none;
  max-height: 14rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.reminders__text--done {
  text-decoration: line-through;
  color: var(--color-text-muted);
}

.reminders__form {
  margin-top: var(--space-md);
  display: flex;
  gap: var(--space-sm);
}

.reminders__input {
  flex: 1;
  min-width: 0;
  padding: 0.4rem;
  border: none;
  background: var(--color-field);
  font: inherit;
}

.reminders__add {
  width: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--color-text-muted);
  color: var(--color-on-primary);
  font-size: 1.25rem;
}
</style>