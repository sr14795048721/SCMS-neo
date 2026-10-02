# SCMS+ Agent Rules

## Encoding
- All new or modified files must use UTF-8 without BOM.
- Before commit, run encoding checks and fix any mojibake-like text.

## i18n
- All user-facing copy must be stored in `frontend/locales/<locale>/*.json`.
- Do not hardcode Chinese copy in `frontend/app`, `frontend/components`, or `frontend/lib`.
- Workflow for new copy:
  1. Add locale key in `frontend/locales/zh-CN/*.json`.
  2. Read it in UI or route code via i18n runtime (`useT` or locale loader).

## Validation Gate
- Before commit, run:
  - `cd frontend`
  - `npm run check:encoding`
  - `npm run check:i18n`
  - `npm run build`

