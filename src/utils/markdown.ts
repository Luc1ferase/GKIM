import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
})

const defaultBlockquote = markdown.renderer.rules.blockquote_open
markdown.renderer.rules.blockquote_open = (...args) => {
  const rendered = defaultBlockquote ? defaultBlockquote(...args) : '<blockquote>'
  return rendered.replace('<blockquote>', '<blockquote class="rich-note">')
}

export function renderPostBody(source: string) {
  return markdown.render(source)
}
