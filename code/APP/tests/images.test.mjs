import test from 'node:test';
import assert from 'node:assert/strict';
import { dependency, root } from './loader.mjs';

const result = await dependency('esbuild').build({
  entryPoints: [root + 'services/images.uts'], bundle: true, write: false,
  platform: 'node', format: 'esm', loader: {'.uts': 'ts'},
  plugins: [{name: 'ui-fixture', setup(build) {
    build.onResolve({filter: /core\/ui\.uts$/}, () => ({path: 'ui', namespace: 'fixture'}));
    build.onLoad({filter: /.*/, namespace: 'fixture'}, () => ({contents: 'export const toast = text => globalThis.notices.push(text);'}));
  }}],
});
const {pickImages} = await import('data:text/javascript;base64,' + Buffer.from(result.outputFiles[0].text).toString('base64'));

test('canceling source or album leaves the draft/attachment consumer untouched', () => {
  globalThis.notices = [];
  let source, picker, calls = 0, hidden = false;
  globalThis.uni = {hideKeyboard: () => {hidden = true;}, showActionSheet: value => {source = value;}, chooseImage: value => {picker = value;}};
  pickImages(4, () => {calls++;});
  assert.equal(hidden, true);
  source.fail({errMsg: 'showActionSheet:fail cancel'});
  assert.equal(picker, undefined);
  assert.equal(calls, 0);
  source.success({tapIndex: 0});
  assert.equal(picker.albumMode, 'system');
  assert.deepEqual(picker.sourceType, ['album']);
  assert.equal(picker.count, 4);
  picker.fail({errMsg: 'chooseImage:fail cancel'});
  picker.success({tempFilePaths: [], tempFiles: []});
  assert.equal(calls, 0);
  assert.deepEqual(notices, []);
  picker.success({tempFilePaths: ['/photo.jpg'], tempFiles: [{path:'/photo.jpg', size:42}]});
  assert.equal(calls, 1);
  source.success({tapIndex: 1});
  assert.equal(picker.count, 1);
  assert.deepEqual(picker.sourceType, ['camera']);
});
