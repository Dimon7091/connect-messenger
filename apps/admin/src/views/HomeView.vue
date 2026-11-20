<template>
  <div class="admin-container">
    <MainHeader />

    <div class="grid grid-cols-3 gap-2 p-2">
      <!-- Таблица пользователей — 2/3 ширины (статистика внутри тулбара) -->
      <div class="col-span-2">
        <UsersTable />
      </div>
      <!-- Блок приглашений — 1/3 ширины (счётчик + сброс + создание) -->
      <InviteBlock />
    </div>
  </div>
</template>

<script setup lang="ts">
import UsersTable from '@/components/layout/UsersTable.vue'
import InviteBlock from '@/components/layout/InviteBlock.vue'
import MainHeader from '@/components/layout/MainHeader.vue'
</script>

<style scoped></style>
