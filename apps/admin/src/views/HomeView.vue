<template>
  <div class="admin-container">
    <MainHeader />

    <div class="grid grid-cols-3 gap-2 p-2">
      <DashboardTotalUsers />
      <DashboardOnlineUsers />
      <DashboardActiveInvites />
      <UsersTable />
      <InviteBlock />
    </div>
  </div>
</template>

<script setup lang="ts">
import DashboardTotalUsers from '@/components/layout/DashboardTotalUsers.vue'
import DashboardOnlineUsers from '@/components/layout/DashboardOnlineUsers.vue'
import DashboardActiveInvites from '@/components/layout/DashboardActiveInvites.vue'
import UsersTable from '@/components/layout/UsersTable.vue'
import InviteBlock from '@/components/layout/InviteBlock.vue'
import MainHeader from '@/components/layout/MainHeader.vue'
</script>

<style scoped></style>
