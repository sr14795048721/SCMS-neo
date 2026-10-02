import nodemailer from "nodemailer";
import type { ApiMessages } from "../i18n/apiMessages";

type FeedbackMailPayload = {
  name?: string;
  email?: string;
  content: string;
  submittedAt: string;
  sourceUrl?: string;
  userAgent?: string;
  forwardedFor?: string;
};

type SmtpConfig = {
  host: string;
  port: number;
  secure: boolean;
  user: string;
  pass: string;
  from: string;
  to: string;
};

type FeedbackMailMessages = ApiMessages["feedback"]["mail"];

export class FeedbackMailerConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "FeedbackMailerConfigError";
  }
}

export async function sendFeedbackEmail(payload: FeedbackMailPayload, messages: FeedbackMailMessages) {
  const config = readSmtpConfig();
  const transporter = nodemailer.createTransport({
    host: config.host,
    port: config.port,
    secure: config.secure,
    auth: {
      user: config.user,
      pass: config.pass
    }
  });

  await transporter.sendMail({
    from: config.from,
    to: config.to,
    subject: buildSubject(payload, messages),
    text: buildText(payload, messages),
    html: buildHtml(payload, messages)
  });
}

function readSmtpConfig(): SmtpConfig {
  const host = readRequired("FEEDBACK_SMTP_HOST");
  const portRaw = readRequired("FEEDBACK_SMTP_PORT");
  const user = readRequired("FEEDBACK_SMTP_USER");
  const pass = readRequired("FEEDBACK_SMTP_PASS");
  const from = readRequired("FEEDBACK_SMTP_FROM");
  const to = readRequired("FEEDBACK_SMTP_TO");
  const secure = parseSecureFlag(process.env.FEEDBACK_SMTP_SECURE, portRaw);
  const port = Number.parseInt(portRaw, 10);

  if (!Number.isFinite(port) || port <= 0) {
    throw new FeedbackMailerConfigError("FEEDBACK_SMTP_PORT is invalid");
  }

  return {
    host,
    port,
    secure,
    user,
    pass,
    from,
    to
  };
}

function buildSubject(payload: FeedbackMailPayload, messages: FeedbackMailMessages) {
  const submitter = payload.name?.trim() || payload.email?.trim() || messages.anonymousName;
  return `${messages.subjectPrefix} - ${submitter}`;
}

function buildText(payload: FeedbackMailPayload, messages: FeedbackMailMessages) {
  return [
    messages.title,
    "",
    `${messages.submittedAtLabel}: ${payload.submittedAt}`,
    `${messages.sourceUrlLabel}: ${payload.sourceUrl || messages.unknownValue}`,
    `${messages.nameLabel}: ${payload.name?.trim() || messages.anonymousName}`,
    `${messages.emailLabel}: ${payload.email?.trim() || messages.emptyEmail}`,
    `${messages.clientIpLabel}: ${payload.forwardedFor || messages.unknownValue}`,
    `${messages.userAgentLabel}: ${payload.userAgent || messages.unknownValue}`,
    "",
    `${messages.contentLabel}:`,
    payload.content.trim()
  ].join("\n");
}

function buildHtml(payload: FeedbackMailPayload, messages: FeedbackMailMessages) {
  return `
    <div style="font-family: Arial, 'Microsoft YaHei', sans-serif; color: #1f2937; line-height: 1.7;">
      <h2 style="margin: 0 0 16px; color: #8b1a1a;">${escapeHtml(messages.title)}</h2>
      <table style="border-collapse: collapse; width: 100%; margin-bottom: 18px;">
        <tbody>
          ${buildRow(messages.submittedAtLabel, payload.submittedAt)}
          ${buildRow(messages.sourceUrlLabel, payload.sourceUrl || messages.unknownValue)}
          ${buildRow(messages.nameLabel, payload.name?.trim() || messages.anonymousName)}
          ${buildRow(messages.emailLabel, payload.email?.trim() || messages.emptyEmail)}
          ${buildRow(messages.clientIpLabel, payload.forwardedFor || messages.unknownValue)}
          ${buildRow(messages.userAgentLabel, payload.userAgent || messages.unknownValue)}
        </tbody>
      </table>
      <div style="margin-bottom: 8px; color: #64748b;">${escapeHtml(messages.contentLabel)}</div>
      <div style="padding: 16px; border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0; white-space: pre-wrap;">
        ${escapeHtml(payload.content.trim())}
      </div>
    </div>
  `;
}

function buildRow(label: string, value: string) {
  return `
    <tr>
      <td style="padding: 8px 10px 8px 0; color: #64748b; vertical-align: top; width: 96px;">${escapeHtml(label)}</td>
      <td style="padding: 8px 0; color: #111827;">${escapeHtml(value)}</td>
    </tr>
  `;
}

function readRequired(name: string) {
  const value = String(process.env[name] || "").trim();
  if (!value) {
    throw new FeedbackMailerConfigError(`${name} is required`);
  }
  return value;
}

function parseSecureFlag(value: string | undefined, portRaw: string) {
  const normalized = String(value || "").trim().toLowerCase();
  if (normalized === "true" || normalized === "1" || normalized === "yes") {
    return true;
  }
  if (normalized === "false" || normalized === "0" || normalized === "no") {
    return false;
  }
  return portRaw === "465";
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}
