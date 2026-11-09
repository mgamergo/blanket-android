import { readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
let html = await readFile(resolve(root, 'docs/comparison-source.html'), 'utf8');
for (const match of [...html.matchAll(/src="([^"]+\.png)"/g)]) {
  const bytes = await readFile(resolve(root, 'docs', match[1]));
  html = html.replace(match[0], `src="data:image/png;base64,${bytes.toString('base64')}"`);
}
await writeFile(resolve(root, 'docs/screenshot-comparison.html'), html);
