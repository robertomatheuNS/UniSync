<script setup>
import { buildMonthCells } from '@/utils/calendar'

const WEEKDAYS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

const today = new Date()
const monthLabel = today.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
const cells = buildMonthCells(today)

function isSunday(index) {
  return index % 7 === 0
}
</script>

<template>
  <section class="calendar" aria-label="Calendário">
    <h2 class="calendar__month">{{ monthLabel }}</h2>

    <div class="calendar__grid">
      <span
        v-for="(weekday, index) in WEEKDAYS"
        :key="weekday"
        class="calendar__weekday"
        :class="{ 'calendar__sunday': isSunday(index) }"
      >
        {{ weekday }}
      </span>

      <span
        v-for="(day, index) in cells"
        :key="index"
        class="calendar__day"
        :class="{
          'calendar__sunday': isSunday(index),
          'calendar__day--today': day === today.getDate(),
        }"
      >
        {{ day }}
      </span>
    </div>
  </section>
</template>

<style scoped>
.calendar {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.calendar__month {
  margin-bottom: var(--space-sm);
  font-size: 1rem;
  text-align: center;
  text-transform: capitalize;
}

.calendar__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.25rem;
  text-align: center;
  font-size: 0.85rem;
}

.calendar__weekday {
  font-weight: 700;
}

.calendar__sunday {
  color: var(--color-error);
}

.calendar__day--today {
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-on-primary);
}
</style>