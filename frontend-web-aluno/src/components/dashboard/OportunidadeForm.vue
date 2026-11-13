<script setup>
import { reactive } from 'vue'
import { dataParaFormulario } from '@/utils/dataOportunidade'
const props = defineProps({
  oportunidade: { type: Object, default: null },
  ocupado: Boolean,
})
const emit = defineEmits(['salvar', 'cancelar'])
const dados = reactive({
  titulo: props.oportunidade?.titulo ?? '',
  materia: props.oportunidade?.materia ?? '',
  tipo: props.oportunidade?.tipo ?? '',
  descricao: props.oportunidade?.descricao ?? '',
  data_evento: props.oportunidade
    ? dataParaFormulario(props.oportunidade.data_evento)
    : '',
})
function salvar() {
  emit('salvar', {
    titulo: dados.titulo.trim(),
    materia: dados.materia.trim(),
    tipo: dados.tipo.trim(),
    descricao: dados.descricao.trim() || null,
    data_evento: new Date(dados.data_evento).toISOString(),
  })
}
</script>
<template>
  <form @submit.prevent="salvar">
    <fieldset :disabled="ocupado" class="formulario">
      <label
        >Título<input
          v-model="dados.titulo"
          autofocus
          required
          minlength="5"
          maxlength="100"
          pattern=".*\S.*"
          placeholder="Ex.: Avaliação de programação"
      /></label>
      <div class="formulario__linha">
        <label
          >Disciplina<input
            v-model="dados.materia"
            required
            pattern=".*\S.*"
            placeholder="Ex.: Programação"
        /></label>
        <label
          >Tipo<input
            v-model="dados.tipo"
            required
            pattern=".*\S.*"
            list="tipos-oportunidade"
            placeholder="Ex.: PROVA"
        /></label>
      </div>
      <datalist id="tipos-oportunidade">
        <option value="PROVA" />
        <option value="TRABALHO" />
        <option value="EVENTO" />
      </datalist>
      <label
        >Data e horário<input
          v-model="dados.data_evento"
          type="datetime-local"
          required
        /><small>Horário local do seu dispositivo.</small></label
      >
      <label
        >Descrição <span>(opcional)</span
        ><textarea
          v-model="dados.descricao"
          rows="5"
          placeholder="Inclua orientações, local e informações para os alunos."
        />
      </label>
      <p class="formulario__autoria">A autoria será vinculada à sua conta.</p>
      <footer class="acoes">
        <button type="button" class="botao" @click="emit('cancelar')">
          Cancelar</button
        ><button type="submit" class="botao botao--principal">
          {{ ocupado ? 'Salvando…' : 'Salvar oportunidade' }}
        </button>
      </footer>
    </fieldset>
  </form>
</template>
<style scoped>
.formulario {
  border: 0;
  display: grid;
  gap: 1.2rem;
  min-width: 0;
}
.formulario label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  min-width: 0;
}
.formulario input,
.formulario textarea {
  width: 100%;
  border: 1px solid var(--color-card);
  border-radius: 8px;
  background: var(--color-field);
  color: var(--color-text);
  padding: 0.75rem;
  font: inherit;
  color-scheme: var(--color-scheme);
}
.formulario textarea {
  resize: vertical;
  min-height: 7rem;
}
.formulario__linha {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.formulario small,
.formulario span,
.formulario__autoria {
  font-size: 0.85rem;
  font-weight: 400;
  color: var(--color-text-muted);
}
@media (max-width: 480px) {
  .formulario__linha {
    grid-template-columns: 1fr;
  }
}
</style>
