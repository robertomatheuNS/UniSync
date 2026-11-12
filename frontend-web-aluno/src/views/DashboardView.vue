<script setup>
import NoticeList from '@/components/dashboard/NoticeList.vue'
import { useNotices } from '@/composables/useNotices'

const { notices } = useNotices()
</script>

<template>
  <NoticeList :notices="notices" />
</template>