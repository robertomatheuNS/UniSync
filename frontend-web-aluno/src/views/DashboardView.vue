<script setup>
import { onMounted, ref } from 'vue'
import NoticeList from '@/components/dashboard/NoticeList.vue'

const apiUrl = import.meta.env.VITE_PYTHON_API_URL || 'http://localhost:8000'
const avisos = ref([])
const carregando = ref(true)
const erro = ref('')

onMounted(async () => {
  try {
    const resposta = await fetch(
      `${apiUrl}/oportunidades/listar?futuros=true&ordenar=true&limite=8`,
    )

    if (!resposta.ok) {
      throw new Error(`HTTP ${resposta.status}`)
    }

    const oportunidades = await resposta.json()
    const agora = Date.now()

    avisos.value = oportunidades.map((oportunidade) => {
      const data = oportunidade.data_evento
      // O FastAPI pode devolver uma data UTC sem o sufixo de fuso.
      const dataComFuso = /(?:Z|[+-]\d{2}:\d{2})$/i.test(data) ? data : `${data}Z`
      const diasAteEvento = Math.ceil((Date.parse(dataComFuso) - agora) / 86_400_000)

      return {
        id: oportunidade.id,
        type: oportunidade.tipo,
        title: oportunidade.titulo,
        teacher: oportunidade.professor,
        daysLeft: Math.max(0, diasAteEvento),
      }
    })
  } catch {
    erro.value = 'Não foi possível carregar as oportunidades.'
  } finally {
    carregando.value = false
  }
})
</script>

<template>
  <p v-if="carregando" role="status">Carregando oportunidades…</p>
  <p v-else-if="erro" role="alert">{{ erro }}</p>
  <p v-else-if="avisos.length === 0">Nenhuma oportunidade futura disponível.</p>
  <NoticeList v-else :notices="avisos" />
</template>