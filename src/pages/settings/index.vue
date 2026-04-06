<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import ProviderCard from '@/components/ProviderCard.vue'
import { useAigcStore } from '@/stores/aigc'

const aigcStore = useAigcStore()

type UniInputLikeEvent = Event & {
  detail?: {
    value?: string | number
  }
}

function updateField(field: 'baseUrl' | 'apiKey' | 'model', value: string) {
  aigcStore.updateCustomProvider({ [field]: value })
}

function readInputValue(event: Event): string {
  const value = (event as UniInputLikeEvent).detail?.value
  return typeof value === 'string' ? value : value == null ? '' : String(value)
}

function handleBaseUrlInput(event: Event) {
  updateField('baseUrl', readInputValue(event))
}

function handleModelInput(event: Event) {
  updateField('model', readInputValue(event))
}

function handleApiKeyInput(event: Event) {
  updateField('apiKey', readInputValue(event))
}

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Infrastructure"
      title="AI Settings"
      description="Switch between preset AI providers or wire a custom OpenAI-compatible gateway for AIGC requests."
      show-back
      @back="goBack"
    />

    <view class="section-stack gap-4">
      <ProviderCard
        v-for="provider in aigcStore.providers"
        :key="provider.id"
        :provider="provider"
        :active="provider.id === aigcStore.activeProviderId"
        @select="aigcStore.setActiveProvider"
      />
    </view>

    <view class="card-shell px-5 py-5 section-stack gap-4">
      <text class="text-[20rpx] uppercase tracking-[0.2em] text-tertiary">Custom endpoint</text>
      <input class="rounded-xl bg-surface-container-lowest/80 px-4 py-4 text-[24rpx] text-on-surface" :value="aigcStore.customProvider.baseUrl" placeholder="Base URL" @input="handleBaseUrlInput" />
      <input class="rounded-xl bg-surface-container-lowest/80 px-4 py-4 text-[24rpx] text-on-surface" :value="aigcStore.customProvider.model" placeholder="Model" @input="handleModelInput" />
      <input class="rounded-xl bg-surface-container-lowest/80 px-4 py-4 text-[24rpx] text-on-surface" password :value="aigcStore.customProvider.apiKey" placeholder="API key" @input="handleApiKeyInput" />
    </view>

    <view class="card-shell px-5 py-5 section-stack gap-3">
      <text class="text-[20rpx] uppercase tracking-[0.2em] text-primary">Local database note</text>
      <text class="text-[24rpx] leading-[1.7] text-on-surface-variant">The Postgres connection is stored in `.env.local`, and the CA certificate is mirrored at `certs/aiven-postgres-ca.pem` for future server-side tooling. It is not wired into the mobile client runtime.</text>
    </view>
  </view>
</template>
