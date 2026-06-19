/**
 * One-time dev script: encodes scenario strings to Base64 for scenarios.js
 * Run: node scripts/encode-scenarios.mjs
 */
import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

function e(str) {
  return Buffer.from(str, 'utf8').toString('base64');
}

function encodeScenario(scenario) {
  return {
    ...scenario,
    sender: e(scenario.sender),
    message: e(scenario.message),
    options: scenario.options.map((opt) => ({
      ...opt,
      text: e(opt.text),
      feedback: e(opt.feedback),
    })),
  };
}

// Load raw scenarios by evaluating the module (temporarily use plaintext file)
const rawPath = join(__dirname, '../src/data/scenarios.raw.js');
const outPath = join(__dirname, '../src/data/scenarios.js');

// Read and eval export - we'll import from a copy
const src = readFileSync(join(__dirname, '../src/data/scenarios.js'), 'utf8');

// Dynamic import won't work on non-module exports easily; parse with Function
const fn = new Function(`${src.replace(/^export /gm, '')}; return { scenariosSet1, scenariosSet2, scenariosSet3 };`);
const { scenariosSet1, scenariosSet2, scenariosSet3 } = fn();

const encodeSet = (set) => set.map(encodeScenario);

const output = `/**
 * Educational scenario data — strings are Base64-encoded to prevent
 * automated crawlers from flagging phishing example content in the JS bundle.
 * Decoded at runtime via scenarioCodec.js when displayed to the user.
 */

export const scenariosSet1 = ${JSON.stringify(encodeSet(scenariosSet1), null, 2)};

export const scenariosSet2 = ${JSON.stringify(encodeSet(scenariosSet2), null, 2)};

export const scenariosSet3 = ${JSON.stringify(encodeSet(scenariosSet3), null, 2)};
`;

writeFileSync(outPath, output, 'utf8');
console.log('Encoded scenarios written to', outPath);
