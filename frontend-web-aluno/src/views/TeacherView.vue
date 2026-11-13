<script setup>
import { computed, onMounted, ref } from 'vue'
import NoticeCard from '@/components/dashboard/NoticeCard.vue'
import BaseModal from '@/components/base/BaseModal.vue'
import OportunidadeForm from '@/components/dashboard/OportunidadeForm.vue'
import {
  listarOportunidades,
  buscarOportunidade,
  criarOportunidade,
  atualizarOportunidade,
  excluirOportunidade,
} from '@/services/oportunidadesService'
import { obterSessao } from '@/utils/sessao'
import { dataOportunidade, formatarData } from '@/utils/dataOportunidade'

const sessao = ref(obterSessao())
const professor = computed(() => sessao.value?.papel === 'Professor')
const oportunidades = ref([])
const carregando = ref(false)
const ocupado = ref(false)
const erro = ref('')
const erroModal = ref('')
const mensagem = ref('')
const modo = ref('')
const selecionada = ref(null)
const tipo = ref('')
const futuros = ref(false)
let consulta = 0

const podeEditar = computed(
  () =>
    professor.value &&
    Boolean(sessao.value.id) &&
    selecionada.value?.professor_id?.toLowerCase() ===
      sessao.value.id.toLowerCase(),
)
const tituloModal = computed(
  () =>
    ({
      detalhes: 'Detalhes da oportunidade',
      criar: 'Criar oportunidade',
      editar: 'Editar oportunidade',
      excluir: 'Excluir oportunidade',
    })[modo.value],
)

function aviso(item) {
  const diferenca = dataOportunidade(item.data_evento) - Date.now()
  return {
    id: item.id,
    title: item.titulo,
    teacher: item.professor,
    type: item.tipo,
    past: diferenca < 0,
    daysLeft: Math.max(0, Math.ceil(diferenca / 86400000)),
  }
}

async function carregar() {
  const atual = ++consulta
  carregando.value = true
  erro.value = ''
  try {
    const resultado = await listarOportunidades({
      tipo: tipo.value,
      futuros: futuros.value,
    })
    if (atual === consulta) oportunidades.value = resultado.slice(0, 15)
  } catch (falha) {
    if (atual === consulta) erro.value = falha.message
  } finally {
    if (atual === consulta) carregando.value = false
  }
}

async function abrir(item) {
  if (ocupado.value) return
  sessao.value = obterSessao()
  ocupado.value = true
  erro.value = ''
  erroModal.value = ''
  try {
    selecionada.value = await buscarOportunidade(item.id)
    modo.value = 'detalhes'
  } catch (falha) {
    erro.value = falha.message
  } finally {
    ocupado.value = false
  }
}

function criar() {
  sessao.value = obterSessao()
  if (!professor.value) return
  selecionada.value = null
  erroModal.value = ''
  modo.value = 'criar'
}

function fechar() {
  if (!ocupado.value) modo.value = ''
}

async function executar(acao, sucesso) {
  if (ocupado.value) return
  ocupado.value = true
  erroModal.value = ''
  mensagem.value = ''
  try {
    sessao.value = obterSessao()
    if (!professor.value)
      throw new Error(
        'Entre novamente com uma conta de professor para continuar.',
      )
    if (modo.value !== 'criar' && !podeEditar.value)
      throw new Error('Somente o autor pode alterar esta oportunidade.')
    await acao(sessao.value.token)
    modo.value = ''
    mensagem.value = sucesso
    await carregar()
  } catch (falha) {
    erroModal.value = falha.message
  } finally {
    ocupado.value = false
  }
}

function salvar(dados) {
  executar(
    (token) =>
      modo.value === 'criar'
        ? criarOportunidade({ ...dados, professor: sessao.value.nome }, token)
        : atualizarOportunidade(selecionada.value.id, dados, token),
    'Oportunidade salva com sucesso.',
  )
}

function excluir() {
  executar(
    (token) => excluirOportunidade(selecionada.value.id, token),
    'Oportunidade excluída com sucesso.',
  )
}

onMounted(carregar)
</script>

<template>
  <section
    class="professor"
    aria-labelledby="professor-titulo"
    :aria-busy="carregando"
  >
    <header class="professor__cabecalho">
      <div>
        <h1 id="professor-titulo">Oportunidades</h1>
        <p>Comunicados e atividades para seus alunos</p>
      </div>
      <button
        v-if="professor"
        class="botao botao--principal"
        :disabled="ocupado"
        @click="criar"
      >
        <span aria-hidden="true">＋</span> Criar oportunidade
      </button>
    </header>
    <p v-if="!sessao" class="professor__sessao">
      Acesse esta página pelo login do UniSync para gerenciar suas
      oportunidades.
    </p>
    <p v-else-if="!professor" class="professor__sessao">
      A criação de oportunidades está disponível para professores.
    </p>
    <form class="professor__filtros" @submit.prevent="carregar">
      <label
        >Tipo<input
          v-model="tipo"
          placeholder="Todos os tipos"
          aria-label="Filtrar por tipo"
      /></label>
      <label class="professor__checkbox"
        ><input v-model="futuros" type="checkbox" /> Somente futuras</label
      >
      <button class="botao" :disabled="carregando">Filtrar</button>
    </form>
    <p v-if="mensagem" class="professor__mensagem" role="status">
      {{ mensagem }}
    </p>
    <div v-if="erro" class="professor__erro" role="alert">
      {{ erro }}
      <button class="botao" :disabled="carregando" @click="carregar">
        Atualizar lista
      </button>
    </div>
    <p v-if="carregando" class="professor__vazio" role="status">
      Carregando oportunidades…
    </p>
    <template v-else-if="!erro">
      <p class="professor__contagem">
        {{ oportunidades.length }} oportunidade(s) · Até 15 por consulta, em
        ordem de data
      </p>
      <ul v-if="oportunidades.length" class="professor__grade">
        <li v-for="item in oportunidades" :key="item.id">
          <button
            class="professor__cartao"
            :disabled="ocupado"
            :aria-label="'Ver detalhes de ' + item.titulo"
            @click="abrir(item)"
          >
            <NoticeCard :notice="aviso(item)" />
            <span class="professor__data">{{
              formatarData(item.data_evento)
            }}</span>
          </button>
        </li>
      </ul>
      <p v-else class="professor__vazio">
        Nenhuma oportunidade encontrada para estes filtros.
      </p>
    </template>
  </section>

  <BaseModal
    v-if="modo"
    :titulo="tituloModal"
    :ocupado="ocupado"
    @fechar="fechar"
  >
    <p v-if="erroModal" class="professor__erro" role="alert">{{ erroModal }}</p>
    <OportunidadeForm
      v-if="modo === 'criar' || modo === 'editar'"
      :oportunidade="selecionada"
      :ocupado="ocupado"
      @salvar="salvar"
      @cancelar="fechar"
    />
    <div v-else-if="modo === 'detalhes'" class="detalhes">
      <span class="detalhes__tipo">{{ selecionada.tipo }}</span>
      <h3>{{ selecionada.titulo }}</h3>
      <dl>
        <dt>Disciplina</dt>
        <dd>{{ selecionada.materia }}</dd>
        <dt>Professor</dt>
        <dd>{{ selecionada.professor }}</dd>
        <dt>Data e horário</dt>
        <dd>{{ formatarData(selecionada.data_evento) }}</dd>
      </dl>
      <div>
        <h4>Descrição</h4>
        <p class="detalhes__descricao">
          {{ selecionada.descricao || 'Nenhuma descrição informada.' }}
        </p>
      </div>
      <footer class="acoes">
        <button
          v-if="podeEditar"
          class="botao botao--perigo"
          @click="modo = 'excluir'"
        >
          Excluir
        </button>
        <button class="botao" @click="fechar">Fechar</button>
        <button
          v-if="podeEditar"
          class="botao botao--principal"
          @click="modo = 'editar'"
        >
          Editar oportunidade
        </button>
      </footer>
    </div>
    <div v-else class="detalhes">
      <p>
        Deseja excluir <strong>{{ selecionada.titulo }}</strong
        >? Esta ação é definitiva.
      </p>
      <footer class="acoes">
        <button class="botao" :disabled="ocupado" @click="modo = 'detalhes'">
          Cancelar</button
        ><button
          class="botao botao--perigo"
          :disabled="ocupado"
          @click="excluir"
        >
          {{ ocupado ? 'Excluindo…' : 'Confirmar exclusão' }}
        </button>
      </footer>
    </div>
  </BaseModal>
</template>

<style scoped>
.professor {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  background: var(--color-sidebar);
  min-height: 26rem;
}
.professor__cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding-bottom: 1.25rem;
  border-bottom: 2px solid var(--color-card);
}
.professor__cabecalho h1 {
  font-size: 1.4rem;
}
.professor__cabecalho p,
.professor__contagem {
  margin-top: 0.4rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
.professor__filtros {
  display: flex;
  gap: 0.75rem;
  align-items: end;
  flex-wrap: wrap;
  padding: 1.25rem 0;
}
.professor__filtros label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.85rem;
}
.professor__filtros input:not([type='checkbox']) {
  background: var(--color-field);
  border: 1px solid var(--color-card);
  padding: 0.65rem;
  border-radius: 8px;
  max-width: 100%;
  font: inherit;
}
.professor__filtros .professor__checkbox {
  display: flex;
  align-items: center;
  min-height: 2.5rem;
}
.professor__grade {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 12rem), 1fr));
  gap: 1rem;
  list-style: none;
  margin-top: 1rem;
}
.professor__grade li {
  min-width: 0;
}
.professor__cartao {
  width: 100%;
  height: 100%;
  text-align: left;
  background: var(--color-card);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
  transition:
    transform 0.15s,
    border-color 0.15s;
}
.professor__cartao:hover {
  transform: translateY(-2px);
  border-color: var(--color-highlight);
}
.professor__data {
  display: block;
  color: var(--color-text);
  font-size: 0.75rem;
  padding: 0.5rem;
  border-top: 1px solid var(--color-sidebar);
}
.professor__vazio {
  padding: 3rem 0;
  text-align: center;
  color: var(--color-text-muted);
}
.professor__sessao,
.professor__mensagem {
  padding: 1rem 0;
}
.professor__erro {
  color: var(--color-error);
  margin-bottom: 1rem;
  overflow-wrap: anywhere;
}
.detalhes {
  display: grid;
  gap: 1.25rem;
  overflow-wrap: anywhere;
}
.detalhes h3 {
  font-size: 1.4rem;
}
.detalhes__tipo {
  justify-self: start;
  padding: 0.4rem 0.75rem;
  border-radius: 20px;
  background: var(--color-card);
  font-size: 0.85rem;
}
.detalhes dl {
  display: grid;
  grid-template-columns: 7rem minmax(0, 1fr);
  gap: 0.75rem;
}
.detalhes dt {
  color: var(--color-text-muted);
}
.detalhes__descricao {
  white-space: pre-wrap;
  line-height: 1.6;
  margin-top: 0.5rem;
}
@media (prefers-reduced-motion: reduce) {
  .professor__cartao {
    transition: none;
  }
}
</style>
