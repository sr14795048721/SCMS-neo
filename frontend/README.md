# SCMS+ Frontend

## Start

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Environment

Copy `.env.example` to `.env.local` for local development or provide the same variables in your deployment environment.

- `BACKEND_BASE_URL`: backend gateway base URL, for example `https://scms.xynova.club`
- `FEEDBACK_SMTP_HOST`: SMTP host for feedback delivery
- `FEEDBACK_SMTP_PORT`: SMTP port such as `465` or `587`
- `FEEDBACK_SMTP_SECURE`: `true` for implicit TLS, `false` otherwise
- `FEEDBACK_SMTP_USER`: SMTP username
- `FEEDBACK_SMTP_PASS`: SMTP password or app password. For QQ Mail, use the SMTP authorization code instead of the mailbox login password.
- `FEEDBACK_SMTP_FROM`: sender address shown in feedback emails
- `FEEDBACK_SMTP_TO`: fixed recipient mailbox for feedback
