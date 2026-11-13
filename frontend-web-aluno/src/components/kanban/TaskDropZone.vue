<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import Sortable from 'sortablejs'
const props = defineProps({
  coluna: { type: String, required: true },
  disabled: Boolean,
})
const emit = defineEmits(['mover'])
const lista = ref(null)
let sortable
let proximoIrmao
onMounted(() => {
  sortable = Sortable.create(lista.value, {
    group: 'tarefas',
    animation: 180,
    disabled: props.disabled,
    draggable: '.tarefa-arrastavel',
    delay: 180,
    delayOnTouchOnly: true,
    touchStartThreshold: 5,
    filter: 'button,input,select',
    preventOnFilter: false,
    ghostClass: 'arrastando',
    onStart(evento) {
      proximoIrmao = evento.item.nextSibling
    },
    onEnd(evento) {
      // Restaura o DOM antes de o Vue aplicar a resposta da API.
      evento.item.remove()
      evento.from.insertBefore(evento.item, proximoIrmao)
      if (evento.from !== evento.to)
        emit('mover', evento.item.dataset.id, evento.to.dataset.coluna)
    },
  })
})
watch(
  () => props.disabled,
  (valor) => sortable?.option('disabled', valor),
)
onBeforeUnmount(() => sortable?.destroy())
</script>
<template>
  <ul ref="lista" :data-coluna="coluna" class="drop-zone">
    <slot />
  </ul>
</template>
<style scoped>
.drop-zone {
  list-style: none;
  min-height: 8rem;
  display: grid;
  align-content: start;
  gap: 0.75rem;
}
.arrastando {
  opacity: 0.35;
}
</style>
