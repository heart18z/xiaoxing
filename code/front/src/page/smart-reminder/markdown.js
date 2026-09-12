import DOMPurify from 'dompurify';
import { marked } from 'marked';

marked.setOptions({
  gfm: true,
  breaks: true,
  headerIds: false,
  mangle: false,
});

export const renderMarkdown = value =>
  DOMPurify.sanitize(marked.parse(String(value || '')), {
    USE_PROFILES: { html: true },
  });
