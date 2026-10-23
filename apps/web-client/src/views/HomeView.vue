<template>
  <div class="h-[100dvh] flex flex-col overflow-hidden">
    <MainHeader />
    <main class="flex-1 overflow-y-auto dark:bg-gray-700">
      <router-view />
    </main>
    <BottomNavigation />
  </div>
</template>

<script setup lang="ts">
import MainHeader from '@/components/layout/MainHeader.vue'
import BottomNavigation from '@/components/layout/BottomNavigation.vue'
</script>
