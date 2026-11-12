import { computed, ref } from 'vue'
import { NOTICES } from '@/data/notices'

// Fica FORA da função de propósito: assim existe uma única lista,
// compartilhada por todas as telas que chamarem useNotices().
const notices = ref([...NOTICES])

export function useNotices() {
  const sortedNotices = computed(() =>
    [...notices.value].sort((a, b) => a.daysLeft - b.daysLeft),
  )

  function getNoticesByTeacher(teacher) {
    return sortedNotices.value.filter((notice) => notice.teacher === teacher)
  }

  function addNotice(data) {
    notices.value.push({ id: crypto.randomUUID(), ...data })
  }

  function updateNotice(id, data) {
    const notice = notices.value.find((item) => item.id === id)
    if (notice) Object.assign(notice, data)
  }

  return { notices: sortedNotices, getNoticesByTeacher, addNotice, updateNotice }
}