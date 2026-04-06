<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppHeader from '@/components/AppHeader.vue'
import { useChatStore } from '@/stores/chat'
import { useAigcStore } from '@/stores/aigc'
import { useAIGC } from '@/composables/useAIGC'
import { formatChatTimestamp } from '@/utils/time'

const chatStore = useChatStore()
const aigcStore = useAigcStore()
const aigc = useAIGC()
const promptInput = ref(aigcStore.draftRequest.prompt || 'Create an editorial portrait with indigo rim light and polished glass surfaces.')

onLoad((options) => {
  if (options?.id && typeof options.id === 'string') {
    chatStore.openConversation(options.id)
    return
  }

  chatStore.ensureStudioRoom()
})

const activeConversation = computed(() => chatStore.activeConversation)
const latestTask = computed(() => aigcStore.history[0])

function goBack() {
  uni.navigateBack()
}

function openWorkshop() {
  uni.navigateTo({ url: '/pages/workshop/index' })
}

function sendMessage() {
  if (!promptInput.value.trim()) {
    return
  }

  chatStore.appendOutgoingMessage(promptInput.value)
}

async function runTextToImage() {
  await aigc.generateTextToImage(promptInput.value)
}

async function runImageToImage() {
  await aigc.generateImageToImage(promptInput.value)
}

async function runVideoToVideo() {
  await aigc.generateVideoToVideo(promptInput.value)
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Active Room"
      :title="activeConversation?.contactName || 'Chat'"
      :description="activeConversation?.contactTitle || 'AIGC-enabled conversation surface.'"
      show-back
      action-label="Workshop"
      @back="goBack"
      @action="openWorkshop"
    />

    <view v-if="activeConversation" class="section-stack gap-4">
      <view
        v-for="message in activeConversation.messages"
        :key="message.id"
        class="max-w-[88%] rounded-xl px-5 py-4"
        :class="message.direction === 'outgoing' ? 'ml-auto bg-gradient-to-br from-primary to-primary-container text-surface' : message.direction === 'system' ? 'bg-surface-container-highest text-on-surface' : 'bg-surface-container-low text-on-surface'"
      >
        <view class="section-stack gap-3">
          <text class="text-[24rpx] leading-[1.7]">{{ message.body }}</text>
          <image v-if="message.attachment" class="h-56 w-full rounded-lg" :src="message.attachment.preview" mode="aspectFill" />
          <view v-if="message.chips?.length" class="flex flex-wrap gap-2">
            <view v-for="chip in message.chips" :key="chip" class="rounded-pill bg-surface-container-high px-3 py-2">
              <text class="text-[18rpx] uppercase tracking-[0.16em] text-on-surface-variant">{{ chip }}</text>
            </view>
          </view>
          <text class="text-[18rpx] opacity-70">{{ formatChatTimestamp(message.createdAt) }}</text>
        </view>
      </view>
    </view>

    <view class="glass-card rounded-xl px-5 py-5 section-stack gap-4">
      <view class="section-stack gap-2">
        <text class="text-[20rpx] uppercase tracking-[0.2em] text-primary">AIGC Actions</text>
        <text class="text-[24rpx] leading-[1.7] text-on-surface-variant">Use the same prompt across text, image, and video flows. Provider: {{ aigcStore.activeProvider?.label }}</text>
      </view>
      <textarea v-model="promptInput" class="min-h-28 rounded-xl bg-surface-container-lowest/80 px-4 py-4 text-[24rpx] text-on-surface" maxlength="500" />
      <view class="grid grid-cols-3 gap-3">
        <button class="rounded-xl bg-surface-container-high px-4 py-4 text-[22rpx] text-on-surface" :disabled="aigcStore.isLoading" @tap="runTextToImage">Text to image</button>
        <button class="rounded-xl bg-surface-container-high px-4 py-4 text-[22rpx] text-on-surface" :disabled="aigcStore.isLoading" @tap="runImageToImage">Image to image</button>
        <button class="rounded-xl bg-surface-container-high px-4 py-4 text-[22rpx] text-on-surface" :disabled="aigcStore.isLoading" @tap="runVideoToVideo">Video to video</button>
      </view>
      <button class="rounded-xl bg-gradient-to-br from-primary to-primary-container px-4 py-4 text-[24rpx] font-semibold text-surface" @tap="sendMessage">Send text</button>
    </view>

    <view v-if="latestTask" class="card-shell px-5 py-5 section-stack gap-3">
      <text class="text-[20rpx] uppercase tracking-[0.2em] text-tertiary">Latest generation</text>
      <image class="h-56 w-full rounded-xl" :src="latestTask.outputPreview" mode="aspectFill" />
      <text class="text-[22rpx] leading-[1.7] text-on-surface-variant">{{ latestTask.mode }} · {{ latestTask.prompt }}</text>
    </view>
  </view>
</template>
