<template>
  <aside class="sidebar">
    <div class="filter-group">
      <h3 class="filter-heading">{{ $t('filters.status') }}</h3>
      <div class="filter-options">
        <label class="checkbox-label">
          <input type="checkbox" :checked="statusFilters.includes('inLibrary')" @change="onToggle('status', 'inLibrary')" class="custom-checkbox" />
          <span>{{ $t('filters.statusInLibrary') }}</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="statusFilters.includes('checkedOut')" @change="onToggle('status', 'checkedOut')" class="custom-checkbox" />
          <span>{{ $t('filters.statusCheckedOut') }}</span>
        </label>
      </div>
    </div>

    <div class="filter-group">
      <h3 class="filter-heading">{{ $t('filters.language') }}</h3>
      <div class="filter-options">
        <label class="checkbox-label">
          <input type="checkbox" :checked="langFilters.includes('English')" @change="onToggle('lang', 'English')" class="custom-checkbox" />
          <span>English</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="langFilters.includes('Latin')" @change="onToggle('lang', 'Latin')" class="custom-checkbox" />
          <span>Latin</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="langFilters.includes('中文 (Chinese)')" @change="onToggle('lang', '中文 (Chinese)')" class="custom-checkbox" />
          <span>中文 (Chinese)</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="langFilters.includes('Deutsch')" @change="onToggle('lang', 'Deutsch')" class="custom-checkbox" />
          <span>Deutsch</span>
        </label>
      </div>
    </div>

    <div class="filter-group">
      <h3 class="filter-heading">{{ $t('filters.category') }}</h3>
      <div class="filter-options">
        <label class="checkbox-label">
          <input type="checkbox" :checked="categoryFilters.includes('Literature')" @change="onToggle('category', 'Literature')" class="custom-checkbox" />
          <span>{{ $t('filters.catLit') }}</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="categoryFilters.includes('Technology')" @change="onToggle('category', 'Technology')" class="custom-checkbox" />
          <span>{{ $t('filters.catTech') }}</span>
        </label>
        <label class="checkbox-label">
          <input type="checkbox" :checked="categoryFilters.includes('Art')" @change="onToggle('category', 'Art')" class="custom-checkbox" />
          <span>{{ $t('filters.catArt') }}</span>
        </label>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
defineProps<{
  statusFilters: string[]
  langFilters: string[]
  categoryFilters: string[]
}>()

const emit = defineEmits<{
  (e: 'toggle', type: string, value: string): void
}>()

function onToggle(type: string, value: string) {
  emit('toggle', type, value)
}
</script>
