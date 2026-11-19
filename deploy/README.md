# deploy/

The Compose description of the platform (`compose.yaml`) and the supplied static checks for it.

**No container engine runs in this course.** The checks read `deploy/compose.yaml`,
`api/Dockerfile`, `pipeline/Dockerfile` and, on the Fast track, `.github/workflows/ci.yml` as
text and YAML. Nothing is built, started or pushed, and nothing talks to GitHub.

```sh
cd deploy
npm ci                       # once (installs the pinned YAML parser)
npm test                     # your own tests only (tests/**/*.test.mjs outside tests/project/)
npm test -- standard-w14     # Standard: Compose + Dockerfiles
npm test -- fast-w11         # Fast: Compose + Dockerfiles + CI workflow
```

| Path | Owner |
|---|---|
| `deploy/compose.yaml`, `api/Dockerfile`, `pipeline/Dockerfile` | you (from Standard W14 / Fast W11) |
| `.github/workflows/ci.yml` | you (Fast W11; optional on Standard). Starts inert: manual trigger only |
| `deploy/package.json`, `deploy/package-lock.json`, `deploy/tests/run.mjs`, `deploy/tests/project/**` | supplied |

Rules the checks look for, in short: the `api`, `db` and `pipeline` services; healthchecks and
`depends_on` with `condition: service_healthy`; services reach each other by service name
(`db`, `http://api:8080`), never `localhost`; `PROVIDER_BASE_URL` comes from the environment;
every password, token and signing secret is `${VARIABLE}` indirection with no default; multi-stage
Dockerfiles with pinned images, a slim runtime stage, a non-root `USER`, `EXPOSE` and no secrets.
The payment provider is a stub reached through `PROVIDER_BASE_URL`; no live provider or banking
credential ever appears.
