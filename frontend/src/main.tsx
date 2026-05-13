import React from 'react';
import ReactDOM from 'react-dom/client';
import { Alert, Box, Button, Chip, CssBaseline, Divider, FormControl, InputLabel, MenuItem, Select, Tab, Tabs, ThemeProvider, Tooltip, Typography, createTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ClearAllIcon from '@mui/icons-material/ClearAll';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import HomeIcon from '@mui/icons-material/Home';
import HubIcon from '@mui/icons-material/Hub';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import SaveIcon from '@mui/icons-material/Save';
import SearchIcon from '@mui/icons-material/Search';
import Editor from '@monaco-editor/react';
import heroImage from './assets/healthcare-suite-hero.png';
import './styles.css';

type ValidationMode = 'STRICT' | 'STANDARD' | 'LENIENT';
type Severity = 'ERROR' | 'WARNING' | 'INFO';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

type ValidationIssue = {
  severity: Severity;
  segment?: string;
  segmentIndex?: number;
  fieldIndex?: number;
  componentIndex?: number;
  location: string;
  description: string;
  suggestedFix: string;
};

type Hl7Field = {
  index: number;
  name: string;
  value: string;
  required: boolean;
  repeating: boolean;
  datatype: string;
  repetitions: { index: number; name: string; value: string }[][];
};

type Hl7Segment = {
  index: number;
  name: string;
  description: string;
  custom: boolean;
  fields: Hl7Field[];
};

type Hl7Result = {
  metadata: Record<string, string | null>;
  segments: Hl7Segment[];
  issues: ValidationIssue[];
  summary: { errors: number; warnings: number; info: number; valid: boolean };
  normalizedMessage: string;
};

type Icd10Result = {
  code: string;
  shortDescription: string;
  longDescription: string;
  rank: number;
  score: number;
  matchPercentage?: number;
  billable: boolean;
  chapter: string;
  matchReason: string;
};

type Icd10Group = {
  diagnosisText: string;
  needsMoreInformation: boolean;
  clarifyingQuestions: string[];
  refinementSuggestions?: string[];
  results: Icd10Result[];
};

type Icd10Response = {
  originalInput: string;
  normalizedInput: string;
  searchedAt: string;
  disclaimer: string;
  diagnosisGroups: Icd10Group[];
};

type Icd10SelectedCode = {
  code: string;
  description: string;
  longDescription: string;
  billable: boolean;
  chapter: string;
};

const sampleMessage = `MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1
PID|1||12345^^^HOSP^MR||DOE^JANE||19800101|F
OBR|1||ORD001|CBC^Complete Blood Count|||20260101120000
OBX|1|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F
ZVN|alpha^beta|custom~repeat`;

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1f6f78' },
    secondary: { main: '#8a5a44' },
    error: { main: '#b42318' },
    warning: { main: '#b54708' },
    info: { main: '#175cd3' },
    background: { default: '#f7f8f5', paper: '#ffffff' }
  },
  typography: {
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
  },
  shape: { borderRadius: 8 }
});

function App() {
  const [module, setModule] = React.useState<'landing' | 'hl7' | 'icd10'>('landing');
  const [message, setMessage] = React.useState(sampleMessage);
  const [mode, setMode] = React.useState<ValidationMode>('STANDARD');
  const [result, setResult] = React.useState<Hl7Result | null>(null);
  const [tab, setTab] = React.useState(0);
  const [query, setQuery] = React.useState('');
  const [status, setStatus] = React.useState('');
  const [error, setError] = React.useState('');
  const [selectedLocation, setSelectedLocation] = React.useState('');

  React.useEffect(() => {
    if (!selectedLocation) {
      return;
    }
    window.setTimeout(() => {
      document.getElementById(locationDomId(selectedLocation))?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    }, 80);
  }, [selectedLocation, tab, result]);

  React.useEffect(() => {
    void parse();
  }, []);

  async function parse() {
    setStatus('Validating...');
    setError('');
    try {
      const parsed = await postJson<Hl7Result>('/api/hl7/parse', { message, mode });
      setResult(parsed);
      setSelectedLocation('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Validation request failed.');
    } finally {
      setStatus('');
    }
  }

  async function saveValidation() {
    setStatus('Saving...');
    setError('');
    try {
      const saved = await postJson<{ expiresAt: string }>('/api/validations', { message, mode });
      setStatus(`Saved until ${new Date(saved.expiresAt).toLocaleString()}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save request failed.');
      setStatus('');
    }
  }

  async function download(format: 'json' | 'xml' | 'pdf' | 'hl7' | 'csv') {
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/exports/${format}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, mode })
      });
      if (!response.ok) {
        throw new Error(await errorMessage(response));
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `hl7-validation.${format === 'hl7' ? 'hl7' : format}`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export request failed.');
    }
  }

  const filteredSegments = result?.segments.filter((segment) => {
    const haystack = `${segment.name} ${segment.description} ${segment.fields.map((field) => `${field.name} ${field.value}`).join(' ')}`.toLowerCase();
    return haystack.includes(query.toLowerCase());
  }) ?? [];

  function focusIssue(issue: ValidationIssue) {
    setSelectedLocation(issue.location);
    setQuery('');
    if (issue.segment && issue.fieldIndex) {
      setTab(0);
    }
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {module === 'landing' ? <LandingPage onSelect={(next) => setModule(next)} /> : (
      <Box className="app-shell">
        <Box component="header" className="topbar">
          <Box>
            <Typography variant="h5" fontWeight={800}>Healthcare Decoder</Typography>
            <Typography variant="body2" color="text.secondary">Healthcare integration debugging suite</Typography>
          </Box>
          <Tabs value={module} onChange={(_, next) => setModule(next)}>
            <Tab value="hl7" label="HL7" />
            <Tab value="icd10" label="ICD-10" />
          </Tabs>
          <Button variant="outlined" startIcon={<HomeIcon />} onClick={() => setModule('landing')}>Solutions</Button>
          {module === 'hl7' && <Box className="toolbar">
            <FormControl size="small">
              <InputLabel>Mode</InputLabel>
              <Select label="Mode" value={mode} onChange={(event) => setMode(event.target.value as ValidationMode)}>
                <MenuItem value="STRICT">Strict</MenuItem>
                <MenuItem value="STANDARD">Standard</MenuItem>
                <MenuItem value="LENIENT">Lenient</MenuItem>
              </Select>
            </FormControl>
            <Button variant="contained" onClick={parse}>Validate</Button>
            <Tooltip title="Save this validation for 24 hours">
              <Button variant="outlined" onClick={saveValidation} startIcon={<SaveIcon />}>Save</Button>
            </Tooltip>
          </Box>}
        </Box>

        <Alert severity="warning" className="phi-alert">Do not submit real PHI unless authorized to do so.</Alert>

        {module === 'icd10' ? <Icd10Module /> : <Box className="workspace">
          <Box className="editor-pane">
            <Box className="pane-head">
              <Typography variant="subtitle1" fontWeight={700}>Raw HL7</Typography>
              <Box className="editor-actions">
                <Chip size="small" label={`${message.length.toLocaleString()} chars`} />
                <Tooltip title="Clear raw HL7">
                  <Button size="small" variant="outlined" startIcon={<ClearAllIcon />} onClick={() => setMessage('')}>Clear</Button>
                </Tooltip>
              </Box>
            </Box>
            <Editor
              height="100%"
              defaultLanguage="text"
              value={message}
              options={{ minimap: { enabled: false }, wordWrap: 'on', fontSize: 13, lineNumbers: 'on' }}
              onChange={(value) => setMessage(value ?? '')}
            />
          </Box>

          <Box className="decoded-pane">
            <Box className="pane-head">
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>Decoded Output</Typography>
                {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
              </Box>
              <Box className="export-row">
                {(['json', 'xml', 'pdf', 'hl7', 'csv'] as const).map((format) => (
                  <Tooltip key={format} title={`Export ${format.toUpperCase()}`}>
                    <Button size="small" variant="outlined" onClick={() => download(format)} startIcon={<DownloadIcon />}>{format.toUpperCase()}</Button>
                  </Tooltip>
                ))}
              </Box>
            </Box>

            {result && <Metadata result={result} />}
            {error && <Alert severity="error" className="inline-error">{error}</Alert>}

            <Tabs value={tab} onChange={(_, next) => setTab(next)}>
              <Tab label="Tree" />
              <Tab label="Grid" />
              <Tab label="JSON" />
              <Tab label={`Issues ${result ? result.issues.length : 0}`} />
            </Tabs>
            <Divider />

            <Box className="search-row">
              <SearchIcon fontSize="small" />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search segments and fields" />
            </Box>

            <Box className="result-body">
              {tab === 0 && <SegmentTree segments={filteredSegments} selectedLocation={selectedLocation} />}
              {tab === 1 && <SegmentGrid segments={filteredSegments} selectedLocation={selectedLocation} />}
              {tab === 2 && <pre className="json-view">{JSON.stringify(result, null, 2)}</pre>}
              {tab === 3 && result && <IssueList issues={result.issues} onSelect={focusIssue} selectedLocation={selectedLocation} />}
            </Box>
          </Box>
        </Box>}
      </Box>
      )}
    </ThemeProvider>
  );
}

function LandingPage({ onSelect }: { onSelect: (module: 'hl7' | 'icd10') => void }) {
  return (
    <Box className="landing-page">
      <Box className="landing-hero" sx={{ backgroundImage: `linear-gradient(90deg, rgba(9, 28, 31, 0.86), rgba(9, 28, 31, 0.48), rgba(9, 28, 31, 0.16)), url(${heroImage})` }}>
        <Box className="landing-nav">
          <Typography variant="h6" fontWeight={900}>Healthcare Decoder</Typography>
          <Box className="landing-nav-actions">
            <Button color="inherit" onClick={() => onSelect('hl7')}>HL7</Button>
            <Button color="inherit" onClick={() => onSelect('icd10')}>ICD-10</Button>
          </Box>
        </Box>
        <Box className="landing-copy">
          <Typography variant="h2" component="h1" fontWeight={900}>Healthcare Decoder</Typography>
          <Typography variant="h6">Choose the workflow you need: validate HL7 messages or search ICD-10-CM diagnosis code suggestions from a backend-mediated government source.</Typography>
          <Box className="landing-actions">
            <Button size="large" variant="contained" startIcon={<HubIcon />} onClick={() => onSelect('hl7')}>Open HL7 Decoder</Button>
            <Button size="large" variant="outlined" color="inherit" startIcon={<LocalOfferIcon />} onClick={() => onSelect('icd10')}>Open ICD-10 Search</Button>
          </Box>
        </Box>
      </Box>

      <Box className="solution-band">
        <Box className="solution-grid">
          <button type="button" className="solution-card" onClick={() => onSelect('hl7')}>
            <span className="solution-icon"><HubIcon /></span>
            <span>
              <strong>HL7 Decoder</strong>
              <small>Parse, validate, inspect, save, and export HL7 messages.</small>
            </span>
          </button>
          <button type="button" className="solution-card" onClick={() => onSelect('icd10')}>
            <span className="solution-icon"><LocalOfferIcon /></span>
            <span>
              <strong>ICD-10 Search</strong>
              <small>Convert diagnosis text into grouped ICD-10-CM code suggestions.</small>
            </span>
          </button>
        </Box>
      </Box>
    </Box>
  );
}

function Metadata({ result }: { result: Hl7Result }) {
  const meta = result.metadata;
  const chips = [
    ['Version', meta.hl7Version],
    ['Type', `${meta.messageType ?? ''}${meta.triggerEvent ? `^${meta.triggerEvent}` : ''}`],
    ['Control', meta.controlId],
    ['Processing', meta.processingId],
    ['Sender', meta.sendingApplication],
    ['Receiver', meta.receivingApplication]
  ];
  return (
    <Box className="metadata-strip">
      <Chip color={result.summary.valid ? 'success' : 'error'} label={result.summary.valid ? 'Valid' : 'Invalid'} />
      <Chip color="error" variant="outlined" label={`${result.summary.errors} errors`} />
      <Chip color="warning" variant="outlined" label={`${result.summary.warnings} warnings`} />
      {chips.map(([label, value]) => <Chip key={label} label={`${label}: ${value || 'n/a'}`} />)}
    </Box>
  );
}

function SegmentTree({ segments, selectedLocation }: { segments: Hl7Segment[]; selectedLocation: string }) {
  return (
    <Box className="segment-tree">
      {segments.map((segment) => (
        <details key={`${segment.index}-${segment.name}`} open id={locationDomId(segment.name)}>
          <summary>
            <strong>{segment.name}</strong>
            <span>{segment.description}</span>
            {segment.custom && <Chip size="small" label="Z-segment" />}
          </summary>
          {segment.fields.map((field) => (
            <Tooltip key={field.index} title={`${field.datatype} | ${field.required ? 'Required' : 'Optional'} | ${field.repeating ? 'Repeating' : 'Single'}`} placement="left">
              <Box id={locationDomId(`${segment.name}-${field.index}`)} className={`field-row ${selectedLocation === `${segment.name}-${field.index}` ? 'selected-row' : ''}`}>
                <span>{segment.name}-{field.index}</span>
                <span>{field.name}</span>
                <code>{field.value || '<empty>'}</code>
              </Box>
            </Tooltip>
          ))}
        </details>
      ))}
    </Box>
  );
}

function SegmentGrid({ segments, selectedLocation }: { segments: Hl7Segment[]; selectedLocation: string }) {
  return (
    <table className="grid-table">
      <thead>
        <tr><th>Location</th><th>Name</th><th>Datatype</th><th>Cardinality</th><th>Value</th></tr>
      </thead>
      <tbody>
        {segments.flatMap((segment) => segment.fields.map((field) => (
          <tr key={`${segment.index}-${field.index}`} id={locationDomId(`grid-${segment.name}-${field.index}`)} className={selectedLocation === `${segment.name}-${field.index}` ? 'selected-row' : ''}>
            <td>{segment.name}-{field.index}</td>
            <td>{field.name}</td>
            <td>{field.datatype}</td>
            <td>{field.required ? 'Required' : 'Optional'} / {field.repeating ? 'Repeats' : 'Single'}</td>
            <td><code>{field.value}</code></td>
          </tr>
        )))}
      </tbody>
    </table>
  );
}

function IssueList({ issues, onSelect, selectedLocation }: { issues: ValidationIssue[]; onSelect: (issue: ValidationIssue) => void; selectedLocation: string }) {
  if (issues.length === 0) {
    return <Alert severity="success">No validation issues detected.</Alert>;
  }
  return (
    <Box className="issue-list">
      {issues.map((issue, index) => (
        <button
          className={`issue-button ${selectedLocation === issue.location ? 'selected-issue' : ''}`}
          key={`${issue.location}-${index}`}
          onClick={() => onSelect(issue)}
          type="button"
        >
          <Alert severity={issue.severity.toLowerCase() as 'error' | 'warning' | 'info'}>
            <strong>{issue.location}</strong> {issue.description}
            <span className="issue-fix">Suggested fix: {issue.suggestedFix}</span>
          </Alert>
        </button>
      ))}
    </Box>
  );
}

function Icd10Module() {
  const [inputText, setInputText] = React.useState('patient has chronic left knee pain and shortness of breath');
  const [result, setResult] = React.useState<Icd10Response | null>(null);
  const [status, setStatus] = React.useState('');
  const [error, setError] = React.useState('');
  const [selectedCodes, setSelectedCodes] = React.useState<Icd10SelectedCode[]>([]);

  async function search() {
    await runSearch(inputText);
  }

  async function runSearch(nextInputText: string) {
    setStatus('Searching ICD-10-CM...');
    setError('');
    try {
      const next = await postJson<Icd10Response>('/api/icd10/search', {
        inputText: nextInputText,
        resultLimit: 10,
        includeClarifyingQuestions: true,
        includeAiRefinement: true
      });
      setInputText(nextInputText);
      setResult(next);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ICD-10 search failed.');
    } finally {
      setStatus('');
    }
  }

  async function saveSearch() {
    setStatus('Saving ICD-10 search...');
    setError('');
    try {
      const saved = await postJson<{ expiresAt: string }>('/api/icd10/save', {
        inputText,
        resultLimit: 10,
        includeClarifyingQuestions: true,
        includeAiRefinement: true
      });
      setStatus(`Saved until ${new Date(saved.expiresAt).toLocaleString()}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save request failed.');
      setStatus('');
    }
  }

  async function exportIcd10(format: 'json' | 'csv' | 'pdf' | 'text', selectedOnly: boolean) {
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/icd10/export/${format}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ inputText, resultLimit: 10, selectedCodes, selectedOnly })
      });
      if (!response.ok) {
        throw new Error(await errorMessage(response));
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `icd10-search.${format === 'text' ? 'txt' : format}`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export request failed.');
    }
  }

  function addCode(result: Icd10Result) {
    setSelectedCodes((current) => {
      if (current.some((item) => item.code === result.code)) {
        return current;
      }
      return [...current, {
        code: result.code,
        description: result.shortDescription,
        longDescription: result.longDescription,
        billable: result.billable,
        chapter: result.chapter
      }];
    });
  }

  function removeCode(code: string) {
    setSelectedCodes((current) => current.filter((item) => item.code !== code));
  }

  async function copyText(text: string) {
    await navigator.clipboard.writeText(text);
  }

  function matchLabel(item: Icd10Result) {
    return `Match ${item.matchPercentage ?? Math.round(item.score * 100)}%`;
  }

  return (
    <Box className="icd10-workspace">
      <Box className="icd10-main">
        <Box className="icd10-input-panel">
          <Box className="pane-head">
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>ICD-10 Diagnosis Search</Typography>
              {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
            </Box>
            <Box className="toolbar">
              <Button variant="contained" onClick={search} startIcon={<SearchIcon />}>Search</Button>
              <Button variant="outlined" onClick={() => { setInputText(''); setResult(null); }} startIcon={<ClearAllIcon />}>Clear</Button>
              <Button variant="outlined" onClick={saveSearch} startIcon={<SaveIcon />}>Save</Button>
            </Box>
          </Box>
          <textarea
            className="icd10-textarea"
            value={inputText}
            onChange={(event) => setInputText(event.target.value)}
            placeholder="Enter diagnosis text, clinical note snippets, or multiple diagnoses on separate lines"
          />
          <Box className="sample-row">
            {['chest pain', 'diabetes with kidney disease', 'left ankle sprain initial encounter'].map((sample) => (
              <Button key={sample} size="small" variant="outlined" onClick={() => setInputText(sample)}>{sample}</Button>
            ))}
          </Box>
        </Box>

        {error && <Alert severity="error">{error}</Alert>}
        {!result && !error && <Alert severity="info">Enter plain-English diagnosis text to receive grouped ICD-10-CM suggestions.</Alert>}
        {result && (
          <Box className="icd10-results">
            <Alert severity="warning">{result.disclaimer}</Alert>
            <Box className="metadata-strip">
              <Chip label={`Normalized: ${result.normalizedInput || 'n/a'}`} />
              <Chip label={`Groups: ${result.diagnosisGroups.length}`} />
              {(['json', 'csv', 'pdf', 'text'] as const).map((format) => (
                <Button key={format} size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={() => exportIcd10(format, false)}>
                  Export all {format.toUpperCase()}
                </Button>
              ))}
            </Box>
            {result.diagnosisGroups.map((group) => (
              <Box className="diagnosis-group" key={group.diagnosisText}>
                <Box className="diagnosis-head">
                  <Typography variant="subtitle1" fontWeight={800}>{group.diagnosisText}</Typography>
                  {group.needsMoreInformation && <Chip color="warning" size="small" label="Needs specificity" />}
                </Box>
                {group.needsMoreInformation && (
                  <Alert severity="info" className="clarify-box">
                    Not enough information to confidently suggest a specific code.
                    <ul>
                      {group.clarifyingQuestions.map((question) => <li key={question}>{question}</li>)}
                    </ul>
                    {group.refinementSuggestions && group.refinementSuggestions.length > 0 && (
                      <Box className="refinement-list">
                        {group.refinementSuggestions.map((suggestion) => (
                          <Chip
                            key={suggestion}
                            size="small"
                            label={suggestion}
                            clickable
                            onClick={() => runSearch(suggestion)}
                          />
                        ))}
                      </Box>
                    )}
                  </Alert>
                )}
                {group.results.length === 0 ? (
                  <Alert severity="warning">No government ICD-10-CM matches returned for this diagnosis text.</Alert>
                ) : group.results.map((item) => (
                  <details className="icd10-result" key={item.code}>
                    <summary>
                      <span className="result-code-cell">
                        <span className="result-code">{item.code}</span>
                        <Tooltip title={`Copy ${item.code}`}>
                          <Button
                            size="small"
                            variant="outlined"
                            className="copy-code-button"
                            startIcon={<ContentCopyIcon />}
                            onClick={(event) => {
                              event.preventDefault();
                              event.stopPropagation();
                              copyText(item.code);
                            }}
                          >
                            Copy
                          </Button>
                        </Tooltip>
                      </span>
                      <span>{item.shortDescription}</span>
                      <Chip size="small" color="info" variant="outlined" label={matchLabel(item)} />
                      <Chip size="small" variant="outlined" label={item.billable ? 'Billable' : 'Non-billable'} />
                    </summary>
                    <Box className="result-detail">
                      <Typography variant="body2"><strong>Long description:</strong> {item.longDescription}</Typography>
                      <Typography variant="body2"><strong>Chapter/category:</strong> {item.chapter}</Typography>
                      <Typography variant="body2"><strong>Match reason:</strong> {item.matchReason}</Typography>
                      <Box className="result-actions">
                        <Button size="small" variant="outlined" startIcon={<AddIcon />} onClick={() => addCode(item)}>Select</Button>
                        <Button size="small" variant="outlined" startIcon={<ContentCopyIcon />} onClick={() => copyText(item.code)}>Copy code</Button>
                        <Button size="small" variant="outlined" startIcon={<ContentCopyIcon />} onClick={() => copyText(item.longDescription)}>Copy description</Button>
                      </Box>
                    </Box>
                  </details>
                ))}
              </Box>
            ))}
          </Box>
        )}
      </Box>

      <Box className="selected-panel">
        <Box className="pane-head">
          <Typography variant="subtitle1" fontWeight={700}>Selected Codes</Typography>
          <Chip size="small" label={selectedCodes.length} />
        </Box>
        <Box className="selected-body">
          {selectedCodes.length === 0 ? <Typography color="text.secondary">No ICD-10 codes selected.</Typography> : selectedCodes.map((code) => (
            <Box className="selected-code" key={code.code}>
              <Box>
                <Typography fontWeight={800}>{code.code}</Typography>
                <Typography variant="body2">{code.description}</Typography>
              </Box>
              <Button size="small" variant="outlined" startIcon={<DeleteIcon />} onClick={() => removeCode(code.code)}>Remove</Button>
            </Box>
          ))}
        </Box>
        <Divider />
        <Box className="selected-actions">
          <Button size="small" variant="outlined" onClick={() => setSelectedCodes([])}>Clear selected</Button>
          {(['json', 'csv', 'pdf', 'text'] as const).map((format) => (
            <Button key={format} size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={() => exportIcd10(format, true)}>
              {format.toUpperCase()}
            </Button>
          ))}
        </Box>
      </Box>
    </Box>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    throw new Error(`Expected JSON from ${path}, but received ${contentType || 'no content type'}. Check that the API is running and the frontend proxy is configured.`);
  }
  return response.json() as Promise<T>;
}

async function errorMessage(response: Response) {
  const text = await response.text();
  return `${response.status} ${response.statusText}${text ? `: ${text.slice(0, 240)}` : ''}`;
}

function locationDomId(location: string) {
  return `loc-${location.replace(/[^a-zA-Z0-9_-]/g, '-')}`;
}
