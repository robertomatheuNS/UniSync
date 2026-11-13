<script setup>
import { computed, onMounted, ref } from 'vue'
import { obterSessao } from '@/utils/sessao'
import { useKanban } from '@/composables/useKanban'
import { KANBAN_COLUMNS } from '@/constants/kanban'
import TaskDropZone from '@/components/kanban/TaskDropZone.vue'
import BaseModal from '@/components/base/BaseModal.vue'
import trashIcon from '@/assets/icons/trash.svg'
const aluno = computed(() => obterSessao()?.papel === 'Aluno')
const {
  tasks,
  loading,
  error,
  loadTasks,
  addTask,
  editTask,
  moveTask,
  deleteTask,
  getTasksByColumn,
} = useKanban()
const titulo = ref(''),
  editando = ref(null),
  excluindo = ref(null),
  novoTitulo = ref('')
async function adicionar() {
  if (await addTask(titulo.value)) titulo.value = ''
}
function abrirEdicao(tarefa) {
  editando.value = tarefa
  novoTitulo.value = tarefa.title
}
async function salvar() {
  if (await editTask(editando.value.id, novoTitulo.value)) editando.value = null
}
function mover(id, coluna) {
  if (coluna === 'trash') excluindo.value = tasks.value.find((t) => t.id === id)
  else moveTask(id, coluna)
}
async function excluir() {
  if (await deleteTask(excluindo.value.id)) excluindo.value = null
}
onMounted(() => {
  if (aluno.value) loadTasks()
})
</script>
<template>
  <section class="kanban">
    <header>
      <div>
        <h1>Meu Kanban</h1>
        <p>Organize seus estudos e acompanhe seu progresso.</p>
      </div>
      <span
        >{{ tasks.filter((t) => t.columnId === 'done').length }} /
        {{ tasks.length }} concluídas</span
      >
    </header>
    <p v-if="!aluno">O Kanban está disponível para contas de aluno.</p>
    <template v-else>
      <p v-if="error" role="alert">
        {{ error }}
        <button class="botao" :disabled="loading" @click="loadTasks">
          Atualizar
        </button>
      </p>
      <form class="nova-tarefa" @submit.prevent="adicionar">
        <label for="nova-tarefa">Nova tarefa</label
        ><input
          id="nova-tarefa"
          v-model="titulo"
          required
          maxlength="100"
          pattern=".*\S.*"
          :disabled="loading"
          placeholder="Ex.: Revisar a aula de hoje"
        /><button class="botao botao--principal" :disabled="loading">
          Adicionar
        </button>
      </form>
      <p v-if="loading" role="status">Salvando ou carregando tarefas…</p>
      <div class="kanban-grid">
        <section
          v-for="coluna in KANBAN_COLUMNS"
          :key="coluna.id"
          class="coluna"
        >
          <h2>
            {{ coluna.title }}
            <span>{{ getTasksByColumn(coluna.id).length }}</span>
          </h2>
          <TaskDropZone :coluna="coluna.id" :disabled="loading" @mover="mover">
            <li
              v-for="tarefa in getTasksByColumn(coluna.id)"
              :key="tarefa.id"
              :data-id="tarefa.id"
              class="tarefa-arrastavel"
            >
              <p>{{ tarefa.title }}</p>
              <div class="tarefa-acoes">
                <button
                  :disabled="loading"
                  :aria-label="'Editar ' + tarefa.title"
                  @click="abrirEdicao(tarefa)"
                >
                  Editar</button
                ><button
                  :disabled="loading"
                  :aria-label="'Excluir ' + tarefa.title"
                  @click="excluindo = tarefa"
                >
                  <img :src="trashIcon" alt="" />
                </button>
              </div>
              <select
                :value="tarefa.columnId"
                :disabled="loading"
                :aria-label="'Status de ' + tarefa.title"
                @change="mover(tarefa.id, $event.target.value)"
              >
                <option
                  v-for="opcao in KANBAN_COLUMNS"
                  :key="opcao.id"
                  :value="opcao.id"
                >
                  {{ opcao.title }}
                </option>
              </select>
            </li>
          </TaskDropZone>
          <p v-if="!getTasksByColumn(coluna.id).length" class="vazio">
            Arraste uma tarefa para cá
          </p>
        </section>
      </div>
      <div class="lixeira">
        <img :src="trashIcon" alt="" /><span>Arraste aqui para excluir</span
        ><TaskDropZone coluna="trash" :disabled="loading" @mover="mover" />
      </div>
    </template>
    <BaseModal
      v-if="editando"
      titulo="Editar tarefa"
      :ocupado="loading"
      @fechar="editando = null"
      ><form class="editar" @submit.prevent="salvar">
        <label for="editar-tarefa">Título</label
        ><input
          id="editar-tarefa"
          v-model="novoTitulo"
          required
          maxlength="100"
          pattern=".*\S.*"
          :disabled="loading"
        />
        <p v-if="error" role="alert">{{ error }}</p>
        <button class="botao botao--principal" :disabled="loading">
          Salvar
        </button>
      </form></BaseModal
    >
    <BaseModal
      v-if="excluindo"
      titulo="Excluir tarefa"
      :ocupado="loading"
      @fechar="excluindo = null"
      ><p>Excluir “{{ excluindo.title }}” definitivamente?</p>
      <p v-if="error" role="alert">{{ error }}</p>
      <footer class="acoes">
        <button class="botao" :disabled="loading" @click="excluindo = null">
          Cancelar</button
        ><button
          class="botao botao--perigo"
          :disabled="loading"
          @click="excluir"
        >
          Excluir tarefa
        </button>
      </footer></BaseModal
    >
  </section>
</template>
<style scoped>
.kanban {
  padding: 1.25rem;
  background: var(--color-sidebar);
  border-radius: 16px;
}
header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}
h1 {
  font-size: 1.5rem;
}
header p,
header span,
.vazio {
  color: var(--color-text-muted);
  font-size: 0.85rem;
  margin-top: 0.5rem;
}
.nova-tarefa {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}
input,
select {
  padding: 0.7rem;
  background: var(--color-field);
  border: 1px solid var(--color-card);
  border-radius: 8px;
  font: inherit;
  min-width: 0;
  color: var(--color-text);
}
.nova-tarefa input {
  flex: 1;
}
.kanban-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}
.coluna {
  padding: 1rem;
  background: var(--color-card);
  border-radius: 12px;
}
h2 {
  font-size: 1rem;
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.tarefa-arrastavel {
  padding: 1rem;
  background: var(--color-field);
  border-radius: 10px;
  box-shadow: 0 3px 10px #0001;
  cursor: grab;
  overflow-wrap: anywhere;
}
.tarefa-acoes {
  display: flex;
  justify-content: space-between;
  margin: 0.75rem 0;
}
.tarefa-acoes button {
  border: 0;
  background: none;
  color: var(--color-text);
  padding: 0.3rem;
}
img {
  width: 1.3rem;
  filter: var(--icon-filter);
}
select {
  width: 100%;
  font-size: 0.8rem;
}
.lixeira {
  position: relative;
  border: 2px dashed var(--color-text-muted);
  border-radius: 12px;
  padding: 1rem;
  margin-top: 1rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}
.lixeira :deep(.drop-zone) {
  position: absolute;
  inset: 0;
  min-height: 0;
}
.editar {
  display: grid;
  gap: 1rem;
}
.acoes {
  margin-top: 1rem;
}
[role='alert'] {
  color: var(--color-error);
}
@media (max-width: 1100px) {
  .kanban-grid {
    grid-template-columns: 1fr;
  }
}
</style>
