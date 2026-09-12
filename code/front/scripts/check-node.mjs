import process from 'node:process';

const required = 18;
const current = process.versions.node;
const major = Number.parseInt(current.split('.')[0], 10);

if (Number.isNaN(major) || major < required) {
  console.error(
    `\n[front] 当前 Node.js 版本为 v${current}，Vite 5 需要 Node.js >= ${required}。\n` +
      `请先切换版本，例如：\n` +
      `  nvm use\n` +
      `  或 nvm install 18 && nvm use 18\n`
  );
  process.exit(1);
}
