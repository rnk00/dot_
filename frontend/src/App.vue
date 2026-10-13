<template>
  <div id="app">
    <RouterView />
    <AppModal />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
