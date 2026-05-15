# Product UI and Workflow Improvements

Phase 8 adds workflow polish across the React app.

## Added

- Authenticated project dashboard with recent and pinned workspace records.
- Local pinned-work persistence.
- Dark mode toggle persisted in local storage.
- Keyboard shortcuts for module switching and help.
- Right-side help drawer with synthetic examples and use disclaimers.
- CSV/JSON file loading for code imports.
- Filtered/sorted workspace and import result lists.
- Workspace copy/export presets.
- In-app notification banner for pin/copy/export actions.
- Onboarding sample entry points for core modules.
- Responsive dense-table handling for mobile layouts.
- Dedicated Platform Tools pages for HL7 Repair, HL7/FHIR conversion, Synthetic Data, X12, Medical Necessity, Roadmap Engines, and AI Review.

Frontend regression checks live in `frontend/scripts/ui-regression.test.mjs` and run with:

```bash
npm test
```
