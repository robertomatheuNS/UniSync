<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { authUrl } from '@/utils/authUrl'
const route = useRoute()
const perfil = route.query.perfil === 'professor' ? 'professor' : 'aluno'
const destino = authUrl('/cadastro/' + perfil + '/')
onMounted(() => window.location.replace(destino))
</script>
<template>
  <p>Continuar para o <a :href="destino">cadastro do UniSync</a>.</p>
</template>
