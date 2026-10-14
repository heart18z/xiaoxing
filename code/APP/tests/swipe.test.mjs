import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {HorizontalSwipe}=await uts('core/swipe.uts');
test('horizontal swipes in either direction cross the threshold',()=>{
 const swipe=new HorizontalSwipe();swipe.start(200,100);swipe.move(170,103);assert.equal(swipe.end(100,108),-1);
 swipe.start(100,100);assert.equal(swipe.end(180,105),1);
});
test('vertical and diagonal scrolling never turn into sideways navigation',()=>{
 const swipe=new HorizontalSwipe();swipe.start(100,100);swipe.move(108,140);assert.equal(swipe.end(240,150),0);
 swipe.start(100,100);assert.equal(swipe.end(180,180),0);
});
test('taps, short drags and cancelled multi-touch leave the page unchanged',()=>{
 const swipe=new HorizontalSwipe();swipe.start(100,100);assert.equal(swipe.end(105,100),0);
 swipe.start(100,100);assert.equal(swipe.end(155,100),0);
 swipe.start(100,100);swipe.cancel();assert.equal(swipe.end(200,100),0);
});
