export function isJsonContentType(contentType: string): boolean {
  return contentType.includes('application/json') || contentType.includes('+json');
}
