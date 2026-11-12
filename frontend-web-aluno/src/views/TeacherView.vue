<script setup>
import { computed, ref } from 'vue'
import NoticeList from '@/components/dashboard/NoticeList.vue'
import NoticeForm from '@/components/teacher/NoticeForm.vue'
import { CURRENT_USER } from '@/constants/currentUser'
import { useNotices } from '@/composables/useNotices'
import editIcon from '@/assets/icons/edit.svg'

const { getNoticesByTeacher, addNotice, updateNotice } = useNotices()

const myNotices = computed(() => getNoticesByTeacher(CURRENT_USER.name))
const isFormOpen = ref(false)
const editingNotice = ref(null)

function openCreateForm() {
  editingNotice.value = null
  isFormOpen.value = true
}

function openEditForm(notice) {
  editingNotice.value = notice
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  editingNotice.value = null
}

function handleSubmit(data) {
  if (editingNotice.value) {
    updateNotice(editingNotice.value.id, data)
  } else {
    addNotice({ ...data, teacher: CURRENT_USER.name })
  }
  closeForm()
}
</script>

<template>
  <section class="teacher">
    <header class="teacher__header">
      <h2 class="teacher__title">Cadastrar avisos</h2>
      <button
        type="button"
        class="teacher__add"
        aria-label="Cadastrar novo aviso"
        @click="openCreateForm"
      >
        +
      </button>
    </header>

    <NoticeForm
      v-if="isFormOpen"
      :key="editingNotice?.id ?? 'new'"
      :notice="editingNotice"
      @submit="handleSubmit"
      @cancel="closeForm"
    />

    <h3 class="teacher__subtitle">Avisos cadastrados</h3>

    <p v-if="myNotices.length === 0" class="teacher__empty">Nenhum aviso cadastrado ainda.</p>

    <NoticeList v-else :notices="myNotices" label="Avisos cadastrados">
      <template #actions="{ notice }">
        <button
          type="button"
          class="teacher__edit"
          :aria-label="`Editar aviso ${notice.title}`"
          @click="openEditForm(notice)"
        >
          <img :src="editIcon" alt="" class="teacher__edit-icon" />
        </button>
      </template>
    </NoticeList>
  </section>
</template>

<style scoped>
.teacher {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-sidebar);
}

.teacher__header {
  padding-bottom: var(--space-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 3px solid var(--color-text-muted);
}

.teacher__title {
  flex: 1;
  color: var(--color-text-muted);
  text-align: center;
  text-transform: uppercase;
}

.teacher__add {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--color-text-muted);
  color: var(--color-on-primary);
  font-size: 1.25rem;
}

.teacher__subtitle {
  margin: var(--space-md) 0 var(--space-sm);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.teacher__empty {
  color: var(--color-text-muted);
}

.teacher__edit {
  border: none;
  background: none;
}

.teacher__edit-icon {
  width: 1.25rem;
  height: 1.25rem;
}
</style>