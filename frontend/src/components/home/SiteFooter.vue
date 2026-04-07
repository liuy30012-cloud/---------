<template>
  <footer v-reveal="{ preset: 'footer', once: true }" class="site-footer">
    <div class="footer-container">
      <div v-reveal="{ preset: 'section', delay: 0.02, once: true }" class="footer-brand">
        <span class="footer-kicker">Archive & Reading Room</span>
        <span class="footer-logo">中国劳动关系学院</span>
        <p class="footer-desc">
          {{ $t('footer.desc') }}
        </p>
      </div>
      <div v-reveal="{ preset: 'section', delay: 0.1, once: true }" class="footer-links">
        <h4 class="footer-heading">{{ $t('footer.resources') }}</h4>
        <ul>
          <li><a href="#">{{ $t('footer.archivalStandards') }}</a></li>
          <li><a href="#">{{ $t('footer.isbnDirectory') }}</a></li>
          <li><a href="#">{{ $t('footer.localizationApis') }}</a></li>
        </ul>
      </div>
      <div v-reveal="{ preset: 'section', delay: 0.18, once: true }" class="footer-info">
        <h4 class="footer-heading">{{ $t('footer.mainBranch') }}</h4>
        <p class="footer-address">
          {{ $t('footer.addressLine1') }}<br />
          {{ $t('footer.addressLine2') }}<br />
          {{ $t('footer.addressLine3') }}
        </p>
        <div class="footer-actions">
          <span class="material-symbols-outlined hover-icon">help</span>
          <span class="material-symbols-outlined hover-icon">logout</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>
