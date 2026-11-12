<script setup>
import { reactive, ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import { NOTICE_TYPE_OPTIONS } from '@/constants/notices'
import { validateNoticeForm } from '@/utils/validateNoticeForm'

const props = defineProps({
  notice: { type: Object, default: null },
})

const emit = defineEmits(['submit', 'cancel'])

const form = reactive({
  type: props.notice?.type ?? '',
  title: props.notice?.title ?? '',
  daysLeft: props.notice?.daysLeft ?? '',
})
const errors = ref({})

function handleSubmit() {
  errors.value = validateNoticeForm(form)
  if (Object.keys(errors.value).length > 0) return

  emit('submit', {
    type: form.type,
    title: form.title.trim(),
    daysLeft: Number(form.daysLeft),
  })
}
</script>

<template>
  <form class="notice-form" novalidate @submit.prevent="handleSubmit">
    <h3 class="notice-form__title">{{ notice ? 'Editar aviso' : 'Novo aviso' }}</h3>

    <BaseSelect
      id="notice-type"
      v-model="form.type"
      label="Tipo:"
      :options="NOTICE_TYPE_OPTIONS"
      :error="errors.type"
    />
    <BaseInput id="notice-title" v-model="form.title" label="Título:" :error="errors.title" />
    <BaseInput
      id="notice-days"
      v-model="form.daysLeft"
      label="Dias restantes:"
      type="number"
      :error="errors.daysLeft"
    />

    <div class="notice-form__actions">
      <BaseButton type="submit">Salvar</BaseButton>
      <BaseButton type="button" @click="emit('cancel')">Cancelar</BaseButton>
    </div>
  </form>
</template>

<style scoped>
.notice-form {
  max-width: 24rem;
  margin: var(--space-md) 0;
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.notice-form__title {
  color: var(--color-text-muted);
}

.notice-form__actions {
  display: flex;
  gap: var(--space-sm);
}
</style>