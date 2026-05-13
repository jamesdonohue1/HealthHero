import React from 'react';
import ReactDOM from 'react-dom/client';
import { Alert, Box, Button, Chip, CssBaseline, Divider, FormControl, InputLabel, MenuItem, Select, Tab, Tabs, ThemeProvider, Tooltip, Typography, createTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
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
let authToken = window.localStorage.getItem('healthcareHeroToken') ?? '';

function setGlobalAuthToken(token: string) {
  authToken = token;
  if (token) {
    window.localStorage.setItem('healthcareHeroToken', token);
  } else {
    window.localStorage.removeItem('healthcareHeroToken');
  }
}

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
  queryTerm?: string;
  fallbackMatch?: boolean;
  source?: string;
};

type Icd10Group = {
  diagnosisText: string;
  needsMoreInformation: boolean;
  clarifyingQuestions: string[];
  refinementSuggestions?: string[];
  queryTerms?: string[];
  exactMatchCount?: number;
  fallbackMatchCount?: number;
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

type Icd10AutocompleteSuggestion = {
  code: string | null;
  description: string;
};

type Icd10RefineResponse = {
  inputText: string;
  normalizedInput: string;
  diagnosisConcepts: string[];
  clarifyingQuestions: string[];
};

type ModuleName = 'landing' | 'hl7' | 'icd10' | 'cpt' | 'tools';
type AuthView = 'login' | 'register' | 'reset' | 'account' | '';

type AuthUser = {
  id: string;
  email: string;
  displayName: string;
  organizationId: string;
  organizationName: string;
  roles: string[];
};

type AuthResponse = {
  token: string;
  expiresAt: string;
  user: AuthUser;
  capabilities: string[];
};

type ApiKeyResponse = {
  id: string;
  name: string;
  apiKey: string;
};

type Hl7RepairResponse = {
  originalMessage: string;
  repairedMessage: string;
  changed: boolean;
  repairs: string[];
};

type FhirConversionResponse = {
  sourceType: string;
  targetType: string;
  bundle: Record<string, unknown>;
  mappingNotes: string[];
};

type SyntheticDataResponse = {
  hl7Messages: string[];
  fhirBundles: Record<string, unknown>[];
  x12Claims: string[];
  patients: string[];
};

type X12DecodeResponse = {
  transactionType: string;
  segments: { index: number; segmentId: string; description: string; loop: string; elements: string[] }[];
  issues: string[];
};

type MedicalNecessityResponse = {
  cptCode: string;
  icd10Codes: string[];
  likelyCovered: boolean;
  riskLevel: string;
  matchedRules: string[];
  recommendations: string[];
};

type GenericPlatformResponse = Record<string, unknown>;

type ProcedureSearchResult = {
  code: string;
  type: string;
  description: string;
  longDescription: string;
  category: string;
  confidence: number;
  active: boolean;
  effectiveDate: string | null;
  terminationDate: string | null;
  source: string;
  matchReason: string;
};

type ProcedureSearchResponse = {
  query: string;
  searchedAt: string;
  licensingNotice: string;
  results: ProcedureSearchResult[];
};

type IcdCptMatchResult = {
  diagnosisText: string;
  diagnosisCode: string;
  procedureText: string;
  procedureCode: string;
  payer: string;
  status: string;
  confidence: number;
  reason: string;
  warnings: string[];
  recommendations: string[];
  modifierSuggestions: { modifier: string; reason: string; required: boolean }[];
};

const sampleMessage = `MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1
PID|1||12345^^^HOSP^MR||DOE^JANE||19800101|F
OBR|1||ORD001|CBC^Complete Blood Count|||20260101120000
OBX|1|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F
ZVN|alpha^beta|custom~repeat`;

const sampleX12 = `ISA*00*          *00*          *ZZ*HEALTHHERO    *ZZ*PAYER          *260101*1230*^*00501*000000001*0*T*:~
GS*HC*HEALTHHERO*PAYER*20260101*1230*1*X*005010X222A1~
ST*837*0001*005010X222A1~
BHT*0019*00*HH0001*20260101*1230*CH~
NM1*IL*1*DOE*JANE****MI*SYN000001~
CLM*HH0001*125.00***11:B:1*Y*A*Y*I~
HI*ABK:M25.562~
SV1*HC:99213*125.00*UN*1***1~
SE*8*0001~`;

const sampleFhir = `{
  "resourceType": "Bundle",
  "type": "collection",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "P1",
        "identifier": [{ "value": "12345" }],
        "name": [{ "family": "Doe", "given": ["Jane"] }],
        "birthDate": "1980-01-01",
        "gender": "female"
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "code": { "text": "White Blood Cells" },
        "valueString": "7.0"
      }
    }
  ]
}`;

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
  const [module, setModule] = React.useState<ModuleName>('landing');
  const [currentUser, setCurrentUser] = React.useState<AuthUser | null>(null);
  const [authView, setAuthView] = React.useState<AuthView>('');
  const [authError, setAuthError] = React.useState('');
  const [apiKey, setApiKey] = React.useState('');
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

  React.useEffect(() => {
    if (!authToken) {
      return;
    }
    getJson<AuthResponse>('/api/auth/me')
      .then((response) => setCurrentUser(response.user))
      .catch(() => {
        setGlobalAuthToken('');
        setCurrentUser(null);
      });
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
    if (!requireAuth()) {
      return;
    }
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
        headers: jsonHeaders(),
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

  function requireAuth() {
    if (currentUser) {
      return true;
    }
    setAuthError('Please log in to save work or view account data.');
    setAuthView('login');
    return false;
  }

  function applyAuth(response: AuthResponse) {
    setGlobalAuthToken(response.token);
    setCurrentUser(response.user);
    setAuthView('');
    setAuthError('');
  }

  async function logout() {
    try {
      await postJson('/api/auth/logout', {});
    } catch {
      // Local logout still clears the bearer token.
    }
    setGlobalAuthToken('');
    setCurrentUser(null);
    setApiKey('');
    setAuthView('');
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {module === 'landing' ? <LandingPage onSelect={(next) => setModule(next)} /> : (
      <Box className="app-shell">
        <Box component="header" className="topbar">
          <Box>
            <Typography variant="h5" fontWeight={800}>Healthcare Hero</Typography>
            <Typography variant="body2" color="text.secondary">Healthcare integration debugging suite</Typography>
          </Box>
          <Tabs value={module} onChange={(_, next) => setModule(next)}>
            <Tab value="hl7" label="HL7" />
            <Tab value="icd10" label="ICD-10" />
            <Tab value="cpt" label="CPT/HCPCS" />
            <Tab value="tools" label="Tools" />
          </Tabs>
          <Button variant="outlined" startIcon={<HomeIcon />} onClick={() => setModule('landing')}>Solutions</Button>
          <Box className="auth-actions">
            {currentUser ? (
              <>
                <Chip size="small" label={`${currentUser.displayName} | ${currentUser.organizationName}`} />
                <Button variant="outlined" onClick={() => setAuthView('account')}>Account</Button>
                <Button variant="outlined" onClick={logout}>Logout</Button>
              </>
            ) : (
              <>
                <Button variant="outlined" onClick={() => setAuthView('login')}>Login</Button>
                <Button variant="contained" onClick={() => setAuthView('register')}>Create account</Button>
              </>
            )}
          </Box>
        </Box>

        <Alert severity="warning" className="phi-alert">Do not submit real PHI unless authorized to do so.</Alert>
        {authView && (
          <AuthPanel
            view={authView}
            setView={setAuthView}
            currentUser={currentUser}
            error={authError}
            setError={setAuthError}
            onAuthenticated={applyAuth}
            onUserUpdate={(user) => setCurrentUser(user)}
            apiKey={apiKey}
            setApiKey={setApiKey}
          />
        )}

        <Box className={`module-view ${module === 'hl7' ? 'active' : ''}`}>
        <Box className="hl7-module-head">
          <Box>
            <Typography variant="h6" fontWeight={800}>HL7 Decoder</Typography>
            {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
          </Box>
          <Box className="toolbar">
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
          </Box>
        </Box>
        <Box className="workspace">
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
        </Box>
        </Box>
        <Box className={`module-view ${module === 'icd10' ? 'active' : ''}`}>
          <Icd10Module requireAuth={requireAuth} />
        </Box>
        <Box className={`module-view ${module === 'cpt' ? 'active' : ''}`}>
          <CptModule />
        </Box>
        <Box className={`module-view ${module === 'tools' ? 'active' : ''}`}>
          <PlatformToolsModule />
        </Box>
      </Box>
      )}
    </ThemeProvider>
  );
}

function LandingPage({ onSelect }: { onSelect: (module: Exclude<ModuleName, 'landing'>) => void }) {
  return (
    <Box className="landing-page">
      <Box className="landing-hero" sx={{ backgroundImage: `linear-gradient(90deg, rgba(9, 28, 31, 0.86), rgba(9, 28, 31, 0.48), rgba(9, 28, 31, 0.16)), url(${heroImage})` }}>
        <Box className="landing-nav">
          <Typography variant="h6" fontWeight={900}>Healthcare Hero</Typography>
          <Box className="landing-nav-actions">
            <Button color="inherit" onClick={() => onSelect('hl7')}>HL7</Button>
            <Button color="inherit" onClick={() => onSelect('icd10')}>ICD-10</Button>
            <Button color="inherit" onClick={() => onSelect('cpt')}>CPT</Button>
            <Button color="inherit" onClick={() => onSelect('tools')}>Tools</Button>
          </Box>
        </Box>
        <Box className="landing-copy">
          <Typography variant="h2" component="h1" fontWeight={900}>Healthcare Hero</Typography>
          <Typography variant="h6">Choose the workflow you need: validate HL7 messages, search ICD-10-CM code suggestions, or run interoperability and revenue cycle tools.</Typography>
          <Box className="landing-actions">
            <Button size="large" variant="contained" startIcon={<HubIcon />} onClick={() => onSelect('hl7')}>Open HL7 Decoder</Button>
            <Button size="large" variant="outlined" color="inherit" startIcon={<LocalOfferIcon />} onClick={() => onSelect('icd10')}>Open ICD-10 Search</Button>
            <Button size="large" variant="outlined" color="inherit" startIcon={<LocalOfferIcon />} onClick={() => onSelect('cpt')}>Open CPT Search</Button>
            <Button size="large" variant="outlined" color="inherit" startIcon={<SearchIcon />} onClick={() => onSelect('tools')}>Open Platform Tools</Button>
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
          <button type="button" className="solution-card" onClick={() => onSelect('tools')}>
            <span className="solution-icon"><SearchIcon /></span>
            <span>
              <strong>Platform Tools</strong>
              <small>Repair HL7, convert FHIR, generate test data, decode X12, and check medical necessity.</small>
            </span>
          </button>
          <button type="button" className="solution-card" onClick={() => onSelect('cpt')}>
            <span className="solution-icon"><LocalOfferIcon /></span>
            <span>
              <strong>CPT/HCPCS Search</strong>
              <small>Find procedure-code candidates and cross-check ICD/CPT claim readiness.</small>
            </span>
          </button>
        </Box>
      </Box>
    </Box>
  );
}

function AuthPanel({
  view,
  setView,
  currentUser,
  error,
  setError,
  onAuthenticated,
  onUserUpdate,
  apiKey,
  setApiKey
}: {
  view: AuthView;
  setView: (view: AuthView) => void;
  currentUser: AuthUser | null;
  error: string;
  setError: (error: string) => void;
  onAuthenticated: (response: AuthResponse) => void;
  onUserUpdate: (user: AuthUser) => void;
  apiKey: string;
  setApiKey: (key: string) => void;
}) {
  const [email, setEmail] = React.useState(currentUser?.email ?? '');
  const [password, setPassword] = React.useState('');
  const [displayName, setDisplayName] = React.useState(currentUser?.displayName ?? '');
  const [organizationName, setOrganizationName] = React.useState(currentUser?.organizationName ?? 'Healthcare Hero');
  const [apiKeyName, setApiKeyName] = React.useState('Default API key');
  const [status, setStatus] = React.useState('');

  React.useEffect(() => {
    setEmail(currentUser?.email ?? email);
    setDisplayName(currentUser?.displayName ?? displayName);
    setOrganizationName(currentUser?.organizationName ?? organizationName);
  }, [currentUser]);

  async function submitAuth(path: string, body: unknown) {
    setStatus('Working...');
    setError('');
    try {
      onAuthenticated(await postJson<AuthResponse>(path, body));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication request failed.');
    } finally {
      setStatus('');
    }
  }

  async function updateAccount() {
    setStatus('Saving account...');
    setError('');
    try {
      const response = await patchJson<AuthResponse>('/api/auth/me', { displayName });
      onUserUpdate(response.user);
      setStatus('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Account update failed.');
      setStatus('');
    }
  }

  async function createApiKey() {
    setStatus('Creating API key...');
    setError('');
    try {
      const response = await postJson<ApiKeyResponse>('/api/auth/api-keys', { name: apiKeyName });
      setApiKey(response.apiKey);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'API key creation failed.');
    } finally {
      setStatus('');
    }
  }

  return (
    <Box className="auth-panel">
      <Box className="pane-head">
        <Box>
          <Typography variant="subtitle1" fontWeight={800}>
            {view === 'login' && 'Login'}
            {view === 'register' && 'Create Account'}
            {view === 'reset' && 'Password Reset'}
            {view === 'account' && 'Account Settings'}
          </Typography>
          {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
        </Box>
        <Button size="small" variant="outlined" onClick={() => { setView(''); setError(''); }}>Close</Button>
      </Box>
      <Box className="auth-panel-body">
        {error && <Alert severity="error">{error}</Alert>}
        {view !== 'account' && (
          <>
            <label>Email <input value={email} onChange={(event) => setEmail(event.target.value)} /></label>
            <label>Password <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          </>
        )}
        {view === 'register' && (
          <>
            <label>Name <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
            <label>Organization <input value={organizationName} onChange={(event) => setOrganizationName(event.target.value)} /></label>
          </>
        )}
        {view === 'account' && currentUser && (
          <>
            <Alert severity="info">Roles: {currentUser.roles.join(', ')}</Alert>
            <label>Name <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
            <label>Email <input value={currentUser.email} disabled /></label>
            <label>Organization <input value={currentUser.organizationName} disabled /></label>
            <Box className="tool-button-row">
              <Button variant="contained" onClick={updateAccount}>Save account</Button>
            </Box>
            <Divider />
            <label>API key name <input value={apiKeyName} onChange={(event) => setApiKeyName(event.target.value)} /></label>
            <Box className="tool-button-row">
              <Button variant="outlined" onClick={createApiKey}>Create API key</Button>
            </Box>
            {apiKey && <Alert severity="warning">Copy this API key now: <code>{apiKey}</code></Alert>}
          </>
        )}
        {view === 'login' && (
          <Box className="tool-button-row">
            <Button variant="contained" onClick={() => submitAuth('/api/auth/login', { email, password })}>Login</Button>
            <Button variant="outlined" onClick={() => setView('reset')}>Forgot password</Button>
            <Button variant="outlined" onClick={() => setView('register')}>Create account</Button>
          </Box>
        )}
        {view === 'register' && (
          <Box className="tool-button-row">
            <Button variant="contained" onClick={() => submitAuth('/api/auth/register', { email, password, displayName, organizationName })}>Create account</Button>
            <Button variant="outlined" onClick={() => setView('login')}>Login</Button>
          </Box>
        )}
        {view === 'reset' && (
          <>
            <Alert severity="warning">MVP reset updates the password directly after email entry. Add email-token verification before production use.</Alert>
            <Box className="tool-button-row">
              <Button variant="contained" onClick={() => submitAuth('/api/auth/password-reset', { email, newPassword: password })}>Reset password</Button>
              <Button variant="outlined" onClick={() => setView('login')}>Back to login</Button>
            </Box>
          </>
        )}
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

function Icd10Module({ requireAuth }: { requireAuth: () => boolean }) {
  const [inputText, setInputText] = React.useState('patient has chronic left knee pain and shortness of breath');
  const [result, setResult] = React.useState<Icd10Response | null>(null);
  const [status, setStatus] = React.useState('');
  const [error, setError] = React.useState('');
  const [selectedCodes, setSelectedCodes] = React.useState<Icd10SelectedCode[]>([]);
  const [autocompleteSuggestions, setAutocompleteSuggestions] = React.useState<Icd10AutocompleteSuggestion[]>([]);
  const [refineQuestions, setRefineQuestions] = React.useState<string[]>([]);
  const [copiedText, setCopiedText] = React.useState('');

  React.useEffect(() => {
    const normalized = inputText.trim();
    if (normalized.length < 3) {
      setAutocompleteSuggestions([]);
      return;
    }
    const timeout = window.setTimeout(async () => {
      try {
        const response = await postJson<{ suggestions: Icd10AutocompleteSuggestion[] }>('/api/icd10/autocomplete', {
          inputText: normalized,
          resultLimit: 5
        });
        setAutocompleteSuggestions(response.suggestions);
      } catch {
        setAutocompleteSuggestions([]);
      }
    }, 350);
    return () => window.clearTimeout(timeout);
  }, [inputText]);

  async function search() {
    await runSearch(inputText);
  }

  async function runSearch(nextInputText: string) {
    setStatus('Searching ICD-10-CM...');
    setError('');
    setRefineQuestions([]);
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
    if (!requireAuth()) {
      return;
    }
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

  async function refineInput() {
    setStatus('Checking specificity...');
    setError('');
    try {
      const response = await postJson<Icd10RefineResponse>('/api/icd10/refine', {
        inputText,
        resultLimit: 10,
        includeClarifyingQuestions: true,
        includeAiRefinement: true
      });
      setRefineQuestions(response.clarifyingQuestions);
      if (response.diagnosisConcepts.length > 0) {
        setInputText(response.diagnosisConcepts.join('\n'));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Refine request failed.');
    } finally {
      setStatus('');
    }
  }

  async function exportIcd10(format: 'json' | 'csv' | 'pdf' | 'text', selectedOnly: boolean) {
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/icd10/export/${format}`, {
        method: 'POST',
        headers: jsonHeaders(),
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
    setCopiedText(text);
    window.setTimeout(() => {
      setCopiedText((current) => current === text ? '' : current);
    }, 1400);
  }

  function matchLabel(item: Icd10Result) {
    return `Match ${item.matchPercentage ?? Math.round(item.score * 100)}%`;
  }

  function selectedCodesText(includeDescriptions: boolean) {
    return selectedCodes
      .map((code) => includeDescriptions ? `${code.code} - ${code.description}` : code.code)
      .join('\n');
  }

  function selectedCodesCsv() {
    return selectedCodes.map((code) => code.code).join(', ');
  }

  function updateInputText(nextInputText: string) {
    setInputText(nextInputText);
    setRefineQuestions([]);
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
              <Button variant="outlined" onClick={refineInput} startIcon={<SearchIcon />}>Refine</Button>
              <Button variant="outlined" onClick={() => { updateInputText(''); setResult(null); setAutocompleteSuggestions([]); }} startIcon={<ClearAllIcon />}>Clear</Button>
              <Button variant="outlined" onClick={saveSearch} startIcon={<SaveIcon />}>Save</Button>
            </Box>
          </Box>
          <textarea
            className="icd10-textarea"
            value={inputText}
            onChange={(event) => updateInputText(event.target.value)}
            placeholder="Enter diagnosis text, clinical note snippets, or multiple diagnoses on separate lines"
          />
          <Box className="sample-row">
            {['chest pain', 'diabetes with kidney disease', 'left ankle sprain initial encounter'].map((sample) => (
              <Button key={sample} size="small" variant="outlined" onClick={() => updateInputText(sample)}>{sample}</Button>
            ))}
          </Box>
          {autocompleteSuggestions.length > 0 && (
            <Box className="autocomplete-row">
              <Typography variant="caption" color="text.secondary" className="suggestions-label">Suggestions</Typography>
              {autocompleteSuggestions.map((suggestion) => (
                <Chip
                  key={`${suggestion.code ?? 'phrase'}-${suggestion.description}`}
                  size="small"
                  className="autocomplete-chip"
                  label={suggestion.code ? `${suggestion.code} ${suggestion.description}` : suggestion.description}
                  clickable
                  onClick={() => updateInputText(suggestion.description)}
                />
              ))}
            </Box>
          )}
        </Box>

        {error && <Alert severity="error">{error}</Alert>}
        {refineQuestions.length > 0 && (
          <Alert severity="info">
            <ul className="refine-question-list">
              {refineQuestions.map((question) => <li key={question}>{question}</li>)}
            </ul>
          </Alert>
        )}
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
                {group.fallbackMatchCount ? (
                  <Alert severity="info" className="fallback-box">
                    No exact ICD-10-CM description match was required. Showing {group.fallbackMatchCount} fallback match{group.fallbackMatchCount === 1 ? '' : 'es'} from {group.queryTerms?.join(', ') || 'broader search terms'}.
                  </Alert>
                ) : null}
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
                            startIcon={copiedText === item.code ? <CheckIcon /> : <ContentCopyIcon />}
                            onClick={(event) => {
                              event.preventDefault();
                              event.stopPropagation();
                              copyText(item.code);
                            }}
                          >
                            {copiedText === item.code ? 'Copied' : 'Copy'}
                          </Button>
                        </Tooltip>
                      </span>
                      <span className="result-description">{item.shortDescription}</span>
                      <Box className="result-badges">
                        <Chip size="small" color="info" variant="outlined" label={matchLabel(item)} />
                        <Chip size="small" variant="outlined" label={item.billable ? 'Billable' : 'Non-billable'} />
                      </Box>
                    </summary>
                    <Box className="result-detail">
                      <Typography variant="body2"><strong>Long description:</strong> {item.longDescription}</Typography>
                      <Typography variant="body2"><strong>Chapter/category:</strong> {item.chapter}</Typography>
                      <Typography variant="body2"><strong>Lookup source:</strong> {item.source || 'NLM Clinical Tables ICD-10-CM'}</Typography>
                      <Typography variant="body2"><strong>Query term:</strong> {item.queryTerm || group.diagnosisText}{item.fallbackMatch ? ' (fallback)' : ''}</Typography>
                      <Typography variant="body2"><strong>Match reason:</strong> {item.matchReason}</Typography>
                      <Box className="result-actions">
                        <Button size="small" variant="outlined" startIcon={<AddIcon />} onClick={() => addCode(item)}>Select</Button>
                        <Button size="small" variant="outlined" startIcon={copiedText === item.code ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(item.code)}>
                          {copiedText === item.code ? 'Copied' : 'Copy code'}
                        </Button>
                        <Button size="small" variant="outlined" startIcon={copiedText === item.longDescription ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(item.longDescription)}>
                          {copiedText === item.longDescription ? 'Copied' : 'Copy description'}
                        </Button>
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
          <Button size="small" variant="outlined" startIcon={copiedText === selectedCodesText(false) ? <CheckIcon /> : <ContentCopyIcon />} disabled={selectedCodes.length === 0} onClick={() => copyText(selectedCodesText(false))}>
            Copy codes
          </Button>
          <Button size="small" variant="outlined" startIcon={copiedText === selectedCodesCsv() ? <CheckIcon /> : <ContentCopyIcon />} disabled={selectedCodes.length === 0} onClick={() => copyText(selectedCodesCsv())}>
            Copy CSV
          </Button>
          <Button size="small" variant="outlined" startIcon={copiedText === selectedCodesText(true) ? <CheckIcon /> : <ContentCopyIcon />} disabled={selectedCodes.length === 0} onClick={() => copyText(selectedCodesText(true))}>
            Copy details
          </Button>
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

function CptModule() {
  const [procedureQuery, setProcedureQuery] = React.useState('chest x-ray 2 views');
  const [searchResult, setSearchResult] = React.useState<ProcedureSearchResponse | null>(null);
  const [diagnosisText, setDiagnosisText] = React.useState('cough');
  const [icd10Code, setIcd10Code] = React.useState('R05.9');
  const [procedureText, setProcedureText] = React.useState('chest x-ray 2 views');
  const [procedureCode, setProcedureCode] = React.useState('71046');
  const [payer, setPayer] = React.useState('Medicare');
  const [compatibility, setCompatibility] = React.useState<IcdCptMatchResult | null>(null);
  const [status, setStatus] = React.useState('');
  const [error, setError] = React.useState('');
  const [copiedText, setCopiedText] = React.useState('');

  React.useEffect(() => {
    const query = procedureQuery.trim();
    if (query.length < 3) {
      setSearchResult(null);
      return;
    }
    const timeout = window.setTimeout(() => {
      void search(query);
    }, 550);
    return () => window.clearTimeout(timeout);
  }, [procedureQuery]);

  async function search(query = procedureQuery) {
    setStatus('Searching CPT/HCPCS...');
    setError('');
    try {
      const params = new URLSearchParams({ q: query, limit: '10' });
      setSearchResult(await getJson<ProcedureSearchResponse>(`/api/cpt/search?${params.toString()}`));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'CPT/HCPCS search failed.');
    } finally {
      setStatus('');
    }
  }

  async function checkCompatibility() {
    setStatus('Checking ICD/CPT compatibility...');
    setError('');
    try {
      setCompatibility(await postJson<IcdCptMatchResult>('/api/coding/compatibility', {
        diagnosisText,
        icd10Code,
        procedureText,
        procedureCode,
        payer
      }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Compatibility check failed.');
    } finally {
      setStatus('');
    }
  }

  async function copyText(text: string) {
    await navigator.clipboard.writeText(text);
    setCopiedText(text);
    window.setTimeout(() => {
      setCopiedText((current) => current === text ? '' : current);
    }, 1400);
  }

  function useProcedure(result: ProcedureSearchResult) {
    setProcedureCode(result.code);
    setProcedureText(result.description);
  }

  function clearCompatibilityCheck() {
    setDiagnosisText('');
    setIcd10Code('');
    setProcedureText('');
    setProcedureCode('');
    setPayer('');
    setCompatibility(null);
    setError('');
    setStatus('');
  }

  return (
    <Box className="cpt-workspace">
      <Box className="cpt-main">
        <Box className="icd10-input-panel">
          <Box className="pane-head">
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>CPT/HCPCS Procedure Search</Typography>
              {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
            </Box>
            <Box className="toolbar">
              <Button variant="contained" onClick={() => search()} startIcon={<SearchIcon />}>Search</Button>
              <Button variant="outlined" onClick={() => { setProcedureQuery(''); setSearchResult(null); }} startIcon={<ClearAllIcon />}>Clear</Button>
            </Box>
          </Box>
          <textarea
            className="icd10-textarea"
            value={procedureQuery}
            onChange={(event) => setProcedureQuery(event.target.value)}
            placeholder="Enter procedure text, HCPCS, CPT, modifier, or range such as 71045-71046"
          />
          <Box className="sample-row">
            {['chest x-ray 2 views', 'knee xray 3 views', 'A1c', '93000', '71045-71046'].map((sample) => (
              <Button key={sample} size="small" variant="outlined" onClick={() => setProcedureQuery(sample)}>{sample}</Button>
            ))}
          </Box>
        </Box>

        {error && <Alert severity="error">{error}</Alert>}
        {searchResult && <Alert severity="warning">{searchResult.licensingNotice}</Alert>}
        {searchResult && (
          <Box className="icd10-results">
            {searchResult.results.length === 0 ? <Alert severity="warning">No CPT/HCPCS matches found.</Alert> : searchResult.results.map((item) => (
              <details className="icd10-result" key={item.code}>
                <summary>
                  <span className="result-code-cell">
                    <span className="result-code">{item.code}</span>
                    <Tooltip title={`Copy ${item.code}`}>
                      <Button size="small" variant="outlined" className="copy-code-button" startIcon={copiedText === item.code ? <CheckIcon /> : <ContentCopyIcon />} onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        copyText(item.code);
                      }}>
                        {copiedText === item.code ? 'Copied' : 'Copy'}
                      </Button>
                    </Tooltip>
                  </span>
                  <span className="result-description">{item.description}</span>
                  <Box className="result-badges">
                    <Chip size="small" color="info" variant="outlined" label={`${item.confidence}%`} />
                    <Chip size="small" variant="outlined" label={item.type} />
                    <Chip size="small" variant="outlined" label={item.active ? 'Active' : 'Retired/deleted'} />
                  </Box>
                </summary>
                <Box className="result-detail">
                  <Typography variant="body2"><strong>Long description:</strong> {item.longDescription}</Typography>
                  <Typography variant="body2"><strong>Category:</strong> {item.category}</Typography>
                  <Typography variant="body2"><strong>Effective:</strong> {item.effectiveDate || 'n/a'} {item.terminationDate ? `through ${item.terminationDate}` : ''}</Typography>
                  <Typography variant="body2"><strong>Source:</strong> {item.source}</Typography>
                  <Typography variant="body2"><strong>Match reason:</strong> {item.matchReason}</Typography>
                  <Box className="result-actions">
                    <Button size="small" variant="outlined" startIcon={<AddIcon />} onClick={() => useProcedure(item)}>Use in check</Button>
                    <Button size="small" variant="outlined" startIcon={copiedText === item.longDescription ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(item.longDescription)}>
                      {copiedText === item.longDescription ? 'Copied' : 'Copy description'}
                    </Button>
                  </Box>
                </Box>
              </details>
            ))}
          </Box>
        )}
      </Box>

      <Box className="selected-panel cpt-check-panel">
        <Box className="pane-head">
          <Typography variant="subtitle1" fontWeight={700}>ICD/CPT Check</Typography>
          <Box className="tool-button-row">
            <Button size="small" variant="outlined" onClick={clearCompatibilityCheck} startIcon={<ClearAllIcon />}>Clear</Button>
            <Button size="small" variant="contained" onClick={checkCompatibility}>Check</Button>
          </Box>
        </Box>
        <Box className="tool-form-row cpt-check-form">
          <label>Diagnosis text <input value={diagnosisText} onChange={(event) => setDiagnosisText(event.target.value)} /></label>
          <label>ICD-10 <input value={icd10Code} onChange={(event) => setIcd10Code(event.target.value)} /></label>
          <label>Procedure text <input value={procedureText} onChange={(event) => setProcedureText(event.target.value)} /></label>
          <label>CPT/HCPCS <input value={procedureCode} onChange={(event) => setProcedureCode(event.target.value)} /></label>
          <label>Payer <input value={payer} onChange={(event) => setPayer(event.target.value)} /></label>
        </Box>
        {compatibility && (
          <Box className="tool-output">
            <Box className="tool-output-head">
              <Chip color={compatibility.status === 'SUPPORTED' ? 'success' : compatibility.status === 'LIKELY_DENIAL' ? 'error' : 'warning'} label={compatibility.status.replace(/_/g, ' ')} />
              <Chip label={`${Math.round(compatibility.confidence * 100)}% confidence`} />
            </Box>
            <Typography variant="body2">{compatibility.reason}</Typography>
            {[...compatibility.warnings, ...compatibility.recommendations].length > 0 && (
              <ul className="compact-list">
                {[...compatibility.warnings, ...compatibility.recommendations].map((item) => <li key={item}>{item}</li>)}
              </ul>
            )}
            {compatibility.modifierSuggestions.length > 0 && (
              <Box className="result-badges">
                {compatibility.modifierSuggestions.map((modifier) => (
                  <Chip key={modifier.modifier} label={`${modifier.modifier}: ${modifier.required ? 'Required' : 'Consider'}`} variant="outlined" />
                ))}
              </Box>
            )}
          </Box>
        )}
      </Box>
    </Box>
  );
}

function PlatformToolsModule() {
  const [repairInput, setRepairInput] = React.useState(sampleMessage);
  const [repairResult, setRepairResult] = React.useState<Hl7RepairResponse | null>(null);
  const [profileResult, setProfileResult] = React.useState<GenericPlatformResponse | null>(null);
  const [fhirInput, setFhirInput] = React.useState(sampleMessage);
  const [fhirResult, setFhirResult] = React.useState<FhirConversionResponse | null>(null);
  const [fhirToHl7Input, setFhirToHl7Input] = React.useState(sampleFhir);
  const [fhirToHl7Result, setFhirToHl7Result] = React.useState<GenericPlatformResponse | null>(null);
  const [syntheticCount, setSyntheticCount] = React.useState(3);
  const [syntheticDiagnosis, setSyntheticDiagnosis] = React.useState('M25.562');
  const [syntheticResult, setSyntheticResult] = React.useState<SyntheticDataResponse | null>(null);
  const [syntheticManifest, setSyntheticManifest] = React.useState<GenericPlatformResponse | null>(null);
  const [x12Input, setX12Input] = React.useState(sampleX12);
  const [x12Result, setX12Result] = React.useState<X12DecodeResponse | null>(null);
  const [cptCode, setCptCode] = React.useState('83036');
  const [icdCodes, setIcdCodes] = React.useState('E11.9');
  const [payer, setPayer] = React.useState('Medicare');
  const [necessityResult, setNecessityResult] = React.useState<MedicalNecessityResponse | null>(null);
  const [roadmapText, setRoadmapText] = React.useState('Left knee pain MRI denied for medical necessity. Patient DOE^JANE has member ABC123.');
  const [roadmapResult, setRoadmapResult] = React.useState<GenericPlatformResponse | null>(null);
  const [roadmapEngine, setRoadmapEngine] = React.useState('prior-auth');
  const [status, setStatus] = React.useState('');
  const [error, setError] = React.useState('');
  const [copiedText, setCopiedText] = React.useState('');

  async function run<T>(label: string, action: () => Promise<T>, onSuccess: (result: T) => void) {
    setStatus(label);
    setError('');
    try {
      onSuccess(await action());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Platform tool request failed.');
    } finally {
      setStatus('');
    }
  }

  async function copyText(text: string) {
    await navigator.clipboard.writeText(text);
    setCopiedText(text);
    window.setTimeout(() => {
      setCopiedText((current) => current === text ? '' : current);
    }, 1400);
  }

  const repairedMessage = repairResult?.repairedMessage ?? '';
  const fhirJson = fhirResult ? JSON.stringify(fhirResult.bundle, null, 2) : '';
  const fhirToHl7Message = typeof fhirToHl7Result?.message === 'string' ? fhirToHl7Result.message : '';
  const syntheticJson = syntheticResult ? JSON.stringify(syntheticResult, null, 2) : '';
  const roadmapJson = roadmapResult ? JSON.stringify(roadmapResult, null, 2) : '';
  const profileJson = profileResult ? JSON.stringify(profileResult, null, 2) : '';
  const manifestJson = syntheticManifest ? JSON.stringify(syntheticManifest, null, 2) : '';

  const roadmapEngines: { id: string; label: string; path: string }[] = [
    { id: 'prior-auth', label: 'Prior Auth', path: '/api/platform/prior-auth/analyze' },
    { id: 'denials', label: 'Denials', path: '/api/platform/denials/analyze' },
    { id: 'cdi', label: 'CDI', path: '/api/platform/cdi/analyze' },
    { id: 'terminology', label: 'Terminology', path: '/api/platform/terminology/normalize' },
    { id: 'labs', label: 'Labs', path: '/api/platform/labs/interpret' },
    { id: 'monitoring', label: 'Monitoring', path: '/api/platform/monitoring/snapshot' },
    { id: 'coding', label: 'AI Coding', path: '/api/platform/coding/assist' },
    { id: 'sandbox', label: 'API Sandbox', path: '/api/platform/sandbox/plan' },
    { id: 'eligibility', label: 'Eligibility', path: '/api/platform/eligibility/analyze' },
    { id: 'compliance', label: 'Compliance', path: '/api/platform/compliance/scan' },
    { id: 'search', label: 'Global Search', path: '/api/platform/search' }
  ];

  function selectedRoadmapEngine() {
    return roadmapEngines.find((engine) => engine.id === roadmapEngine) ?? roadmapEngines[0];
  }

  return (
    <Box className="tools-workspace">
      <Box className="tools-status-row">
        <Box>
          <Typography variant="h6" fontWeight={800}>Platform Tools</Typography>
          {status && <Typography variant="caption" color="text.secondary">{status}</Typography>}
        </Box>
        <Chip label="MVP engines" variant="outlined" />
      </Box>
      {error && <Alert severity="error">{error}</Alert>}

      <Box className="tool-grid">
        <Box className="tool-panel">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>HL7 Repair</Typography>
            <Box className="tool-button-row">
              <Button variant="outlined" onClick={() => run('Running profile validation...', () => postJson<GenericPlatformResponse>('/api/platform/hl7/profile-validate', { message: repairInput, mode: 'STANDARD' }), setProfileResult)}>Profile</Button>
              <Button variant="contained" onClick={() => run('Repairing HL7...', () => postJson<Hl7RepairResponse>('/api/hl7/repair', { message: repairInput, mode: 'STANDARD' }), setRepairResult)}>Repair</Button>
            </Box>
          </Box>
          <textarea className="tool-textarea" value={repairInput} onChange={(event) => setRepairInput(event.target.value)} />
          {profileResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label="Advanced profile validation" />
                <Button size="small" variant="outlined" startIcon={copiedText === profileJson ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(profileJson)}>
                  {copiedText === profileJson ? 'Copied' : 'Copy JSON'}
                </Button>
              </Box>
              <pre className="json-view">{profileJson}</pre>
            </Box>
          )}
          {repairResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip color={repairResult.changed ? 'warning' : 'success'} label={repairResult.changed ? 'Changed' : 'No changes'} />
                <Button size="small" variant="outlined" startIcon={copiedText === repairedMessage ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(repairedMessage)}>
                  {copiedText === repairedMessage ? 'Copied' : 'Copy repaired'}
                </Button>
              </Box>
              <ul className="compact-list">{repairResult.repairs.map((repair) => <li key={repair}>{repair}</li>)}</ul>
              <pre className="json-view">{repairResult.repairedMessage}</pre>
            </Box>
          )}
        </Box>

        <Box className="tool-panel">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>HL7 to FHIR</Typography>
            <Button variant="contained" onClick={() => run('Converting HL7 to FHIR...', () => postJson<FhirConversionResponse>('/api/platform/fhir/hl7-to-fhir', { text: fhirInput }), setFhirResult)}>Convert</Button>
          </Box>
          <textarea className="tool-textarea" value={fhirInput} onChange={(event) => setFhirInput(event.target.value)} />
          {fhirResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label={`${fhirResult.sourceType} to ${fhirResult.targetType}`} />
                <Button size="small" variant="outlined" startIcon={copiedText === fhirJson ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(fhirJson)}>
                  {copiedText === fhirJson ? 'Copied' : 'Copy JSON'}
                </Button>
              </Box>
              <ul className="compact-list">{fhirResult.mappingNotes.map((note) => <li key={note}>{note}</li>)}</ul>
              <pre className="json-view">{fhirJson}</pre>
            </Box>
          )}
        </Box>

        <Box className="tool-panel">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>FHIR to HL7</Typography>
            <Button variant="contained" onClick={() => run('Converting FHIR to HL7...', () => postJson<GenericPlatformResponse>('/api/platform/fhir/fhir-to-hl7', { text: fhirToHl7Input }), setFhirToHl7Result)}>Convert</Button>
          </Box>
          <textarea className="tool-textarea" value={fhirToHl7Input} onChange={(event) => setFhirToHl7Input(event.target.value)} />
          {fhirToHl7Result && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label={`${fhirToHl7Result.sourceType ?? 'FHIR'} to ${fhirToHl7Result.targetType ?? 'HL7'}`} />
                <Button size="small" variant="outlined" startIcon={copiedText === fhirToHl7Message ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(fhirToHl7Message)}>
                  {copiedText === fhirToHl7Message ? 'Copied' : 'Copy HL7'}
                </Button>
              </Box>
              <pre className="json-view">{fhirToHl7Message}</pre>
            </Box>
          )}
        </Box>

        <Box className="tool-panel">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>Synthetic Data</Typography>
            <Box className="tool-button-row">
              <Button variant="outlined" onClick={() => run('Loading export manifest...', () => postJson<GenericPlatformResponse>('/api/platform/synthetic/export-manifest', {}), setSyntheticManifest)}>Exports</Button>
              <Button variant="contained" onClick={() => run('Generating synthetic data...', () => postJson<SyntheticDataResponse>('/api/platform/synthetic/generate', { count: syntheticCount, minAge: 18, maxAge: 90, diagnosis: syntheticDiagnosis }), setSyntheticResult)}>Generate</Button>
            </Box>
          </Box>
          <Box className="tool-form-row">
            <label>Count <input type="number" min={1} max={25} value={syntheticCount} onChange={(event) => setSyntheticCount(Number(event.target.value))} /></label>
            <label>Diagnosis <input value={syntheticDiagnosis} onChange={(event) => setSyntheticDiagnosis(event.target.value)} /></label>
          </Box>
          {syntheticResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label={`${syntheticResult.patients.length} patients`} />
                <Button size="small" variant="outlined" startIcon={copiedText === syntheticJson ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(syntheticJson)}>
                  {copiedText === syntheticJson ? 'Copied' : 'Copy payload'}
                </Button>
              </Box>
              <ul className="compact-list">{syntheticResult.patients.map((patient) => <li key={patient}>{patient}</li>)}</ul>
              <pre className="json-view">{syntheticJson}</pre>
            </Box>
          )}
          {syntheticManifest && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label="Export manifest" />
                <Button size="small" variant="outlined" startIcon={copiedText === manifestJson ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(manifestJson)}>
                  {copiedText === manifestJson ? 'Copied' : 'Copy JSON'}
                </Button>
              </Box>
              <pre className="json-view">{manifestJson}</pre>
            </Box>
          )}
        </Box>

        <Box className="tool-panel">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>X12 Decoder</Typography>
            <Button variant="contained" onClick={() => run('Decoding X12...', () => postJson<X12DecodeResponse>('/api/platform/x12/decode', { text: x12Input }), setX12Result)}>Decode</Button>
          </Box>
          <textarea className="tool-textarea" value={x12Input} onChange={(event) => setX12Input(event.target.value)} />
          {x12Result && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label={x12Result.transactionType} />
                <Chip color={x12Result.issues.length ? 'warning' : 'success'} label={`${x12Result.issues.length} issues`} />
              </Box>
              {x12Result.issues.length > 0 && <ul className="compact-list">{x12Result.issues.map((issue) => <li key={issue}>{issue}</li>)}</ul>}
              <table className="grid-table">
                <thead><tr><th>#</th><th>ID</th><th>Description</th><th>Loop</th><th>Elements</th></tr></thead>
                <tbody>
                  {x12Result.segments.map((segment) => (
                    <tr key={`${segment.index}-${segment.segmentId}`}>
                      <td>{segment.index}</td>
                      <td><code>{segment.segmentId}</code></td>
                      <td>{segment.description}</td>
                      <td>{segment.loop}</td>
                      <td><code>{segment.elements.join(' | ')}</code></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Box>
          )}
        </Box>

        <Box className="tool-panel tool-panel-wide">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>Medical Necessity</Typography>
            <Button variant="contained" onClick={() => run('Checking medical necessity...', () => postJson<MedicalNecessityResponse>('/api/platform/necessity/check', {
              cptCode,
              icd10Codes: icdCodes.split(/[\n, ]+/).filter(Boolean),
              payer
            }), setNecessityResult)}>Check</Button>
          </Box>
          <Box className="tool-form-row">
            <label>CPT <input value={cptCode} onChange={(event) => setCptCode(event.target.value)} /></label>
            <label>ICD-10 <input value={icdCodes} onChange={(event) => setIcdCodes(event.target.value)} /></label>
            <label>Payer <input value={payer} onChange={(event) => setPayer(event.target.value)} /></label>
          </Box>
          {necessityResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip color={necessityResult.likelyCovered ? 'success' : 'warning'} label={necessityResult.likelyCovered ? 'Likely covered' : 'Review needed'} />
                <Chip label={`Risk ${necessityResult.riskLevel}`} />
              </Box>
              <Typography variant="body2"><strong>CPT:</strong> {necessityResult.cptCode}</Typography>
              <Typography variant="body2"><strong>ICD-10:</strong> {necessityResult.icd10Codes.join(', ') || 'n/a'}</Typography>
              <ul className="compact-list">
                {[...necessityResult.matchedRules, ...necessityResult.recommendations].map((item) => <li key={item}>{item}</li>)}
              </ul>
            </Box>
          )}
        </Box>

        <Box className="tool-panel tool-panel-wide">
          <Box className="pane-head">
            <Typography variant="subtitle1" fontWeight={700}>Roadmap Engines</Typography>
            <Button variant="contained" onClick={() => {
              const engine = selectedRoadmapEngine();
              run(`Running ${engine.label}...`, () => postJson<GenericPlatformResponse>(engine.path, { text: roadmapText }), setRoadmapResult);
            }}>Run</Button>
          </Box>
          <Box className="roadmap-engine-row">
            {roadmapEngines.map((engine) => (
              <Chip
                key={engine.id}
                color={roadmapEngine === engine.id ? 'primary' : 'default'}
                variant={roadmapEngine === engine.id ? 'filled' : 'outlined'}
                label={engine.label}
                clickable
                onClick={() => setRoadmapEngine(engine.id)}
              />
            ))}
          </Box>
          <textarea className="tool-textarea" value={roadmapText} onChange={(event) => setRoadmapText(event.target.value)} />
          {roadmapResult && (
            <Box className="tool-output">
              <Box className="tool-output-head">
                <Chip label={selectedRoadmapEngine().label} />
                <Button size="small" variant="outlined" startIcon={copiedText === roadmapJson ? <CheckIcon /> : <ContentCopyIcon />} onClick={() => copyText(roadmapJson)}>
                  {copiedText === roadmapJson ? 'Copied' : 'Copy JSON'}
                </Button>
              </Box>
              <pre className="json-view">{roadmapJson}</pre>
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: jsonHeaders(),
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

async function patchJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'PATCH',
    headers: jsonHeaders(),
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  return response.json() as Promise<T>;
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, { headers: authHeaders() });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    throw new Error(`Expected JSON from ${path}, but received ${contentType || 'no content type'}.`);
  }
  return response.json() as Promise<T>;
}

function authHeaders(): Record<string, string> {
  return authToken ? { Authorization: `Bearer ${authToken}` } : {};
}

function jsonHeaders(): Record<string, string> {
  return { 'Content-Type': 'application/json', ...authHeaders() };
}

async function errorMessage(response: Response) {
  const text = await response.text();
  return `${response.status} ${response.statusText}${text ? `: ${text.slice(0, 240)}` : ''}`;
}

function locationDomId(location: string) {
  return `loc-${location.replace(/[^a-zA-Z0-9_-]/g, '-')}`;
}
