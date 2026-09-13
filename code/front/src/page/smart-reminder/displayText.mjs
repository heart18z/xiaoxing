// Presentation only: keep the original timestamp/timezone and do not rewrite stored event facts.
export const displayText = value => String(value ?? '').replace(/(\d{4}-\d{2}-\d{2})T(?=\d{2}:\d{2})/g, '$1 ');
