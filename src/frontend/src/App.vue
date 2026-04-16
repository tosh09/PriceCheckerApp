<template>
  <div class="container py-4">
    <h3 class="text-center text-darkgrey fw-bold mb-4">Price Checker</h3>
    <!-- 開発中は簡略化のためRouterを使わずに直接コンポーネントを配置します -->
    <HomeView />
  </div>
</template>

<script setup>
import HomeView from './views/HomeView.vue'
</script>
