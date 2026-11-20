<template>
  <span class="inline-flex flex-col items-center text-xs leading-none">
    <!-- Стрелка вверх (▲) -->
    <span
      :class="[
        'transition-colors duration-200',
        sortBy === field && sortOrder === 'asc' ? 'text-blue-600' : 'text-gray-400',
      ]"
    >
      ▲
    </span>
    <!-- Стрелка вниз (▼) -->
    <span
      :class="[
        'transition-colors duration-200',
        sortBy === field && sortOrder === 'desc' ? 'text-blue-600' : 'text-gray-400',
      ]"
    >
      ▼
    </span>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  field: string
  sortBy: string | null
  sortOrder: 'asc' | 'desc' | null
}>()
</script>
