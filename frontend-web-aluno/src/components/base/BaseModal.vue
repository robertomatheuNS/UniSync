<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

defineProps({ titulo: { type: String, required: true }, ocupado: Boolean })
const emit = defineEmits(['fechar'])
const dialogo = ref(null)
let focoAnterior
let rolagemAnterior
onMounted(() => {
  focoAnterior = document.activeElement
  rolagemAnterior = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  dialogo.value.showModal()
})
onBeforeUnmount(() => {
  document.body.style.overflow = rolagemAnterior
  dialogo.value?.close()
  focoAnterior?.focus()
})
</script>

<template>
  <Teleport to="body">
    <dialog
      ref="dialogo"
      class="modal"
      aria-labelledby="modal-titulo"
      :aria-busy="ocupado"
      @cancel.prevent="!ocupado && emit('fechar')"
    >
      <header class="modal__cabecalho">
        <h2 id="modal-titulo">{{ titulo }}</h2>
        <button
          type="button"
          aria-label="Fechar janela"
          :disabled="ocupado"
          @click="emit('fechar')"
        >
          ×
        </button>
      </header>
      <div class="modal__conteudo"><slot /></div>
    </dialog>
  </Teleport>
</template>

<style scoped>
.modal {
  margin: auto;
  width: min(38rem, calc(100vw - 2rem));
  max-height: calc(100dvh - 2rem);
  overflow-y: auto;
  padding: 0;
  border: 1px solid var(--color-card);
  border-radius: 16px;
  background: var(--color-sidebar);
  color: var(--color-text);
  box-shadow: 0 24px 80px #0004;
}
.modal::backdrop {
  background: #0b163080;
  backdrop-filter: blur(3px);
}
.modal__cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--color-header);
}
.modal__cabecalho h2 {
  font-size: 1.2rem;
}
.modal__cabecalho button {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 1.8rem;
  width: 2.5rem;
  height: 2.5rem;
}
.modal__conteudo {
  padding: 1.5rem;
}
</style>
