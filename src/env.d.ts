/// <reference types="vite/client" />
/// <reference types="@dcloudio/types" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, any>
  export default component
}

type UniRouteOptions = {
  url: string
}

type UniStorageValue = string | number | boolean | object | null | undefined

interface UniLike {
  navigateTo(options: UniRouteOptions): void
  navigateBack(options?: { delta?: number }): void
  switchTab(options: UniRouteOptions): void
  showToast(options?: { title?: string; icon?: string }): void
  chooseImage(options?: unknown): void
  chooseVideo(options?: unknown): void
  getStorageSync?(key: string): UniStorageValue
  setStorageSync?(key: string, value: string): void
  removeStorageSync?(key: string): void
}

declare const uni: UniLike

declare global {
  var uni: UniLike | undefined
}
