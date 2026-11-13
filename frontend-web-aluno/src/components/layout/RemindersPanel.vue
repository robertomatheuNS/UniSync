<script setup>
import { onMounted, ref } from 'vue'
import {
  listarLembretes,
  criarLembrete,
  atualizarLembrete,
  excluirLembrete,
} from '@/services/lembretesService'
import trashIcon from '@/assets/icons/trash.svg'
const itens = ref([]),
  titulo = ref(''),
  editando = ref(null),
  ocupado = ref(false),
  erro = ref('')
async function carregar() {
  ocupado.value = true
  try {
    itens.value = await listarLembretes()
    erro.value = ''
  } catch (e) {
    erro.value = e.message
  } finally {
    ocupado.value = false
  }
}
async function executar(acao) {
  if (ocupado.value) return
  ocupado.value = true
  erro.value = ''
  try {
    await acao()
  } catch (e) {
    erro.value = e.message
  } finally {
    ocupado.value = false
  }
}
function salvar() {
  const texto = titulo.value.trim()
  if (!texto) return
  executar(async () => {
    if (editando.value) {
      const atualizado = await atualizarLembrete(editando.value, {
        titulo: texto,
      })
      itens.value = itens.value.map((item) =>
        item.id === atualizado.id ? atualizado : item,
      )
    } else itens.value.push(await criarLembrete(texto))
    cancelar()
  })
}
function cancelar() {
  editando.value = null
  titulo.value = ''
}
function editar(item) {
  editando.value = item.id
  titulo.value = item.titulo
}
function concluir(item) {
  executar(async () => {
    const atualizado = await atualizarLembrete(item.id, {
      concluido: !item.concluido,
    })
    itens.value = itens.value.map((valor) =>
      valor.id === item.id ? atualizado : valor,
    )
  })
}
function excluir(item) {
  executar(async () => {
    await excluirLembrete(item.id)
    itens.value = itens.value.filter((valor) => valor.id !== item.id)
    if (editando.value === item.id) cancelar()
  })
}
onMounted(carregar)
</script>
<template>
  <section
    class="lembretes"
    aria-labelledby="lembretes-titulo"
    :aria-busy="ocupado"
  >
    <h2 id="lembretes-titulo">Meus lembretes</h2>
    <p v-if="erro" role="alert">
      {{ erro }}
      <button class="botao" :disabled="ocupado" @click="carregar">
        Tentar novamente
      </button>
    </p>
    <form @submit.prevent="salvar">
      <label for="lembrete-texto">{{
        editando ? 'Editar lembrete' : 'Novo lembrete'
      }}</label>
      <input
        id="lembrete-texto"
        v-model="titulo"
        maxlength="200"
        required
        :disabled="ocupado"
        placeholder="O que você precisa lembrar?"
      />
      <div class="acoes">
        <button
          v-if="editando"
          type="button"
          class="botao"
          :disabled="ocupado"
          @click="cancelar"
        >
          Cancelar</button
        ><button class="botao botao--principal" :disabled="ocupado">
          {{ editando ? 'Salvar' : 'Adicionar' }}
        </button>
      </div>
    </form>
    <p v-if="!itens.length && !ocupado">Seus lembretes aparecerão aqui.</p>
    <TransitionGroup name="lista" tag="ul">
      <li v-for="item in itens" :key="item.id">
        <input
          type="checkbox"
          :checked="item.concluido"
          :disabled="ocupado"
          :aria-label="'Concluir ' + item.titulo"
          @change="concluir(item)"
        />
        <button
          class="lembrete-texto"
          :class="{ concluido: item.concluido }"
          :disabled="ocupado"
          :aria-label="'Editar ' + item.titulo"
          @click="editar(item)"
        >
          {{ item.titulo }}
        </button>
        <button
          class="icone"
          :disabled="ocupado"
          :aria-label="'Excluir ' + item.titulo"
          @click="excluir(item)"
        >
          <img :src="trashIcon" alt="" />
        </button>
      </li>
    </TransitionGroup>
  </section>
</template>
<style scoped>
.lembretes {
  padding: 1rem;
  border-radius: 16px;
  background: var(--color-card);
  display: grid;
  gap: 1rem;
}
h2 {
  font-size: 1.1rem;
}
form {
  display: grid;
  gap: 0.6rem;
}
label,
p {
  font-size: 0.85rem;
}
input:not([type='checkbox']) {
  min-width: 0;
  width: 100%;
  padding: 0.7rem;
  border: 0;
  border-radius: 8px;
  background: var(--color-field);
  font: inherit;
}
ul {
  list-style: none;
  max-height: 24rem;
  overflow: auto;
}
li {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid var(--color-sidebar);
}
.lembrete-texto {
  flex: 1;
  text-align: left;
  background: none;
  border: 0;
  color: var(--color-text);
  overflow-wrap: anywhere;
}
.concluido {
  text-decoration: line-through;
  opacity: 0.65;
}
.icone {
  border: 0;
  background: none;
  padding: 0.4rem;
}
.icone img {
  width: 1.2rem;
  filter: var(--icon-filter);
}
[role='alert'] {
  color: var(--color-error);
}
</style>
