import { defineConfig, presetAttributify, presetIcons, presetUno, transformerDirectives, transformerVariantGroup } from 'unocss'

export default defineConfig({
  presets: [presetUno(), presetAttributify(), presetIcons()],
  transformers: [transformerDirectives(), transformerVariantGroup()],
  theme: {
    colors: {
      surface: '#091328',
      'surface-light': '#F8FAFC',
      'surface-container-low': '#1a2338',
      'surface-container-high': '#2a3550',
      'surface-container-highest': '#344262',
      'surface-container-lowest': '#050b16',
      primary: '#C3C0FF',
      'primary-container': '#4F46E5',
      'primary-soft': '#8E98FF',
      secondary: '#D5E3FD',
      tertiary: '#FF9DD1',
      'on-surface': '#FFFFFF',
      'on-surface-variant': '#A3AAC4',
      'outline-variant': '#464555',
      success: '#49D39D',
      danger: '#FF6E84',
      warning: '#F5C16C'
    },
    fontFamily: {
      headline: '"Space Grotesk", "PingFang SC", sans-serif',
      body: 'Inter, "PingFang SC", sans-serif',
      mono: '"JetBrains Mono", "SFMono-Regular", monospace'
    },
    borderRadius: {
      sm: '8rpx',
      md: '16rpx',
      lg: '24rpx',
      xl: '32rpx',
      pill: '9999rpx'
    },
    boxShadow: {
      ambient: '0 24rpx 80rpx rgba(9, 19, 40, 0.32)',
      glow: '0 0 48rpx rgba(195, 192, 255, 0.18)'
    }
  },
  shortcuts: {
    'glass-panel': 'bg-[rgba(9,19,40,0.64)] backdrop-blur-2xl',
    'card-shell': 'rounded-xl bg-surface-container-low shadow-ambient',
    'page-shell': 'min-h-screen bg-surface px-8 pt-8 text-on-surface'
  }
})
