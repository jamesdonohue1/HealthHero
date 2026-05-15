import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import assert from 'node:assert/strict';

const source = readFileSync(new URL('../src/main.tsx', import.meta.url), 'utf8');

test('persists and restores tab state', () => {
  assert.match(source, /healthcareHeroModule/);
  assert.match(source, /window\.localStorage\.setItem\('healthcareHeroModule'/);
  assert.match(source, /setModule\(savedModule\)/);
});

test('ICD/CPT clear behavior resets compatibility fields', () => {
  const clearFunction = source.match(/function clearCompatibilityCheck\(\) \{[\s\S]*?\n  \}/)?.[0] ?? '';
  assert.match(clearFunction, /setDiagnosisText\(''\)/);
  assert.match(clearFunction, /setIcd10Code\(''\)/);
  assert.match(clearFunction, /setProcedureText\(''\)/);
  assert.match(clearFunction, /setProcedureCode\(''\)/);
  assert.match(clearFunction, /setCompatibility\(null\)/);
});

test('accessibility labels exist for theme and help controls', () => {
  assert.match(source, /aria-label': 'Toggle dark mode'/);
  assert.match(source, /HelpDrawer/);
});

test('platform tools expose clear controls and AI human review status', () => {
  assert.match(source, /clearRepairTool/);
  assert.match(source, /clearHl7ToFhirTool/);
  assert.match(source, /clearAiTool/);
  assert.match(source, /approvalStatus/);
});

test('platform tools support a multi-panel workspace', () => {
  assert.match(source, /type PlatformToolPage/);
  assert.match(source, /visibleToolPages/);
  assert.match(source, /toggleToolPage/);
  assert.match(source, /!visibleToolPages\.includes\('repair'\)/);
  assert.match(source, /!visibleToolPages\.includes\('x12'\)/);
  assert.match(source, /!visibleToolPages\.includes\('ai'\)/);
});

test('AI review requires authentication before calling the API', () => {
  const runAiReview = source.match(/function runAiReview\(\) \{[\s\S]*?\n  \}/)?.[0] ?? '';
  assert.match(runAiReview, /if \(!requireAuth\(\)\)/);
  assert.match(runAiReview, /\/api\/ai\/assist/);
});
