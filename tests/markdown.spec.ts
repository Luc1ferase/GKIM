import { describe, expect, it } from 'vitest'
import { renderPostBody } from '@/utils/markdown'

describe('renderPostBody', () => {
  it('renders markdown headings and code blocks into html', () => {
    const html = renderPostBody('# Hello\n\n```ts\nconst value = 1\n```')

    expect(html).toContain('<h1>Hello</h1>')
    expect(html).toContain('<code class="language-ts">')
    expect(html).toContain('const value = 1')
  })

  it('wraps note blocks with presentation classes', () => {
    const html = renderPostBody('> blueprint note')

    expect(html).toContain('rich-note')
  })
})
