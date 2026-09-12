import { copyFileSync, existsSync } from 'node:fs';
if (!existsSync('dist-native/native.html')) throw new Error('Native build is missing');
copyFileSync('dist-native/native.html', 'dist-native/index.html');
