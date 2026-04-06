<script setup lang="ts">
import { onHide, onLaunch, onShow } from '@dcloudio/uni-app'

onLaunch(() => {
  console.info('UniAIGC IM launched')
})

onShow(() => {
  console.info('UniAIGC IM visible')
})

onHide(() => {
  console.info('UniAIGC IM hidden')
})
</script>

<style lang="scss">
@import '@/styles/app.scss';
@import '@/styles/rich-content.scss';
</style>
