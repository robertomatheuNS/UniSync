<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import { validateRegisterForm } from '@/utils/validateRegisterForm'

const SHIFT_OPTIONS = [
  { value: 'manha', label: 'Manhã' },
  { value: 'tarde', label: 'Tarde' },
  { value: 'noite', label: 'Noite' },
]

const route = useRoute()
const router = useRouter()

const role = route.query.perfil === 'professor' ? 'professor' : 'aluno'

const form = reactive({
  name: '',
  email: '',
  password: '',
  course: '',
  shift: '',
})
const errors = ref({})

function handleSubmit() {
  errors.value = validateRegisterForm(form)

  if (Object.keys(errors.value).length > 0) return

  // TODO: enviar os dados para a API quando o back-end estiver pronto
  router.push({ name: 'dashboard' })
}
</script>

<template>
  <main class="register">
    <form class="register__card" novalidate @submit.prevent="handleSubmit">
      <h1 class="register__title">Cadastro de {{ role }}</h1>

      <BaseInput id="name" v-model="form.name" label="Nome Completo:" :error="errors.name" />
      <BaseInput id="email" v-model="form.email" label="E-mail:" type="email" :error="errors.email" />
      <BaseInput id="password" v-model="form.password" label="Senha:" type="password" :error="errors.password" />
      <BaseInput id="course" v-model="form.course" label="Curso:" :error="errors.course" />
      <BaseSelect id="shift" v-model="form.shift" label="Turno:" :options="SHIFT_OPTIONS" :error="errors.shift" />

      <BaseButton type="submit">Criar Cadastro</BaseButton>
    </form>
  </main>
</template>

<style scoped>
.register {
  min-height: 100vh;
  padding: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-landing);
}

.register__card {
  width: 100%;
  max-width: 24rem;
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  border-radius: var(--radius-lg);
  background: var(--color-card);
}

.register__title {
  font-size: 1.25rem;
  font-weight: 400;
  text-transform: capitalize;
}

.register__card > .base-button {
  align-self: center;
}
</style>