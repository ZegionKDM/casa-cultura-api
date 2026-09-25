<script setup>
defineProps({
  title: String,
  value: [String, Number],
  subtitle: String,
  icon: [Object, Function],
  color: String
})
</script>

<template>
  <article class="stat-card">
    <div
      class="icon-box"
      :style="{ backgroundColor: color }"
    >
      <component
        :is="icon"
        :size="30"
      />
    </div>

    <div class="stat-info">
      <p>{{ title }}</p>
      <h2>{{ value }}</h2>
      <span>{{ subtitle }}</span>
    </div>
  </article>
</template>

<style scoped>
.stat-card {
  background: white;
  border: 1px solid #e7e9ef;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.07);
}

.icon-box {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  min-width: 0;
}

.stat-info p {
  margin: 0;
  color: #6d7480;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-info h2 {
  margin: 5px 0;
  font-size: 26px;
  color: #1c2434;
  font-weight: 700;
  line-height: 1.2;
}

.stat-info span {
  font-size: 12px;
  color: #8a92a3;
}
</style>
