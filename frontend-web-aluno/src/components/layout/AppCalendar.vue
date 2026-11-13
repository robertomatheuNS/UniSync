<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { buildMonthCells } from '@/utils/calendar'
const hoje = ref(new Date()),
  mes = ref(new Date(hoje.value.getFullYear(), hoje.value.getMonth(), 1)),
  selecionada = ref(new Date())
const dias = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
const cells = computed(() => buildMonthCells(mes.value))
const rotulo = computed(() =>
  mes.value.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }),
)
const igual = (a, b) => a.toDateString() === b.toDateString()
const data = (dia) =>
  new Date(mes.value.getFullYear(), mes.value.getMonth(), dia)
function mover(delta) {
  mes.value = new Date(mes.value.getFullYear(), mes.value.getMonth() + delta, 1)
}
function voltarHoje() {
  hoje.value = new Date()
  mes.value = new Date(hoje.value.getFullYear(), hoje.value.getMonth(), 1)
  selecionada.value = hoje.value
}
let relogio
onMounted(() => {
  relogio = setInterval(() => {
    hoje.value = new Date()
  }, 60000)
})
onBeforeUnmount(() => clearInterval(relogio))
</script>
<template>
  <section class="calendar" aria-label="Calendário">
    <header>
      <button aria-label="Mês anterior" @click="mover(-1)">‹</button>
      <h2 aria-live="polite">{{ rotulo }}</h2>
      <button aria-label="Próximo mês" @click="mover(1)">›</button>
    </header>
    <div class="calendar-grid">
      <span v-for="dia in dias" :key="dia">{{ dia }}</span>
      <template v-for="(dia, index) in cells" :key="index">
        <button
          v-if="dia"
          :aria-label="
            data(dia).toLocaleDateString('pt-BR', { dateStyle: 'full' })
          "
          :aria-current="igual(data(dia), hoje) ? 'date' : undefined"
          :aria-pressed="igual(data(dia), selecionada)"
          @click="selecionada = data(dia)"
        >
          {{ dia }}
        </button>
        <span v-else />
      </template>
    </div>
    <footer>
      <button class="botao" @click="voltarHoje">Hoje</button
      ><time :datetime="selecionada.toLocaleDateString('sv-SE')">{{
        selecionada.toLocaleDateString('pt-BR')
      }}</time>
    </footer>
  </section>
</template>
<style scoped>
.calendar {
  padding: 1rem;
  background: var(--color-calendar);
  border-radius: 16px;
  color: var(--color-text);
}
header,
footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.4rem;
}
h2 {
  font-size: 0.9rem;
  text-transform: capitalize;
  text-align: center;
}
button {
  background: none;
  border: 0;
  color: inherit;
  border-radius: 8px;
  min-height: 2rem;
  min-width: 1.6rem;
}
header button {
  font-size: 1.8rem;
}
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  text-align: center;
  gap: 0.2rem;
  margin: 1rem 0;
  font-size: 0.75rem;
}
.calendar-grid button:hover {
  background: var(--color-header);
}
[aria-current='date'] {
  font-weight: 800;
  border: 1px solid var(--color-highlight);
}
[aria-pressed='true'] {
  background: var(--color-header);
}
footer {
  font-size: 0.8rem;
}
</style>
