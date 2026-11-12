<script setup>
import NoticeList from '@/components/dashboard/NoticeList.vue'
import { NOTICES } from '@/data/notices'
</script>

<template>
  <NoticeList :notices="NOTICES" />
</template>