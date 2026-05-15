# Feature Flags

Feature flags let you phase releases without changing code. The backend is the source of truth and publishes the current public flags at:

- `GET /api/features`
- `GET /api/v1/features`

When a gated feature is disabled, its UI controls are hidden and its API routes return `404`.

## Available Switches

| Environment variable | Default | Gates |
| --- | --- | --- |
| `FEATURE_DASHBOARD_ENABLED` | `true` | Project dashboard |
| `FEATURE_WORKSPACES_ENABLED` | `true` | Workspace UI and `/api/workspaces` |
| `FEATURE_AI_ASSIST_ENABLED` | `true` local, `false` prod | AI review UI and `/api/ai` |
| `FEATURE_ADMIN_IMPORTS_ENABLED` | `true` local, `false` prod | Code import UI and `/api/admin/imports` |
| `FEATURE_PLATFORM_TOOLS_ENABLED` | `true` | Platform Tools UI and `/api/platform` |
| `FEATURE_DARK_MODE_ENABLED` | `true` | Dark-mode toggle |

## Example Rollout

Start with core tools only:

```bash
FEATURE_DASHBOARD_ENABLED=false \
FEATURE_WORKSPACES_ENABLED=false \
FEATURE_AI_ASSIST_ENABLED=false \
FEATURE_ADMIN_IMPORTS_ENABLED=false \
FEATURE_PLATFORM_TOOLS_ENABLED=false \
java -jar target/hl7-decoder-0.1.0-SNAPSHOT.jar
```

Then enable one cohort at a time by changing the environment values and restarting the service. Production defaults intentionally keep AI assist and admin imports off until explicitly enabled.
