import test from 'node:test';
import assert from 'node:assert/strict';
import worker from './worker.js';

test('health endpoint exposes readiness only, never provider keys', async () => {
  const response = await worker.fetch(new Request('https://gateway.example/health'), {});
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.ok, true);
  assert.equal(body.providers.gemini, false);
  assert.equal(body.providers.deepseek, false);
  assert.equal(JSON.stringify(body).includes('API_KEY'), false);
});

test('rejects malformed/empty chat requests', async () => {
  const response = await worker.fetch(new Request('https://gateway.example/v1/chat', {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ provider: 'gemini', prompt: ' ' }),
  }), {});
  assert.equal(response.status, 400);
});

test('Gemini text requests are proxied without returning provider secret', async () => {
  const originalFetch = globalThis.fetch;
  let seenHeader = '';
  globalThis.fetch = async (_url, options) => {
    seenHeader = options.headers['x-goog-api-key'];
    return new Response(JSON.stringify({ steps: [{ type: 'model_output', content: [{ type: 'text', text: 'Halo dari Gemini' }] }] }), { status: 200 });
  };
  try {
    const response = await worker.fetch(new Request('https://gateway.example/v1/chat', {
      method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ provider: 'gemini', prompt: 'Halo' }),
    }), { GEMINI_API_KEY: 'test-secret' });
    assert.equal(response.status, 200);
    assert.equal((await response.json()).answer, 'Halo dari Gemini');
    assert.equal(seenHeader, 'test-secret');
  } finally { globalThis.fetch = originalFetch; }
});

test('DeepSeek screenshot requests are sent as image_url data URLs', async () => {
  const originalFetch = globalThis.fetch;
  let sentBody;
  globalThis.fetch = async (_url, options) => {
    sentBody = JSON.parse(options.body);
    return new Response(JSON.stringify({ choices: [{ message: { content: 'Layar terlihat jelas' } }] }), { status: 200 });
  };
  try {
    const response = await worker.fetch(new Request('https://gateway.example/v1/chat', {
      method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ provider: 'deepseek', prompt: 'Jelaskan', image: { mimeType: 'image/jpeg', data: 'YWJj' } }),
    }), { DEEPSEEK_API_KEY: 'test-secret' });
    assert.equal(response.status, 200);
    assert.equal((await response.json()).answer, 'Layar terlihat jelas');
    assert.equal(sentBody.model, 'deepseek-flash');
    assert.equal(sentBody.messages[1].content[1].image_url.url, 'data:image/jpeg;base64,YWJj');
  } finally { globalThis.fetch = originalFetch; }
});
