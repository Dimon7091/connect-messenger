<template>
  <div id="app" class="w-full">
    <main class="w-7xl mx-auto">
      <router-view />
    </main>
    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import ToastContainer from '@/components/ui/ToastContainer.vue';
</script>
