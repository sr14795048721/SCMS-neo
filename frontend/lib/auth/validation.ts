export const USERNAME_MIN_LENGTH = 3;
export const USERNAME_MAX_LENGTH = 64;
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const UPPERCASE_PATTERN = /[A-Z]/;
const LOWERCASE_PATTERN = /[a-z]/;
const DIGIT_PATTERN = /\d/;
const SPECIAL_PATTERN = /[^A-Za-z0-9]/;

export type PasswordRuleState = {
  minLength: boolean;
  maxLength: boolean;
  hasUpper: boolean;
  hasLower: boolean;
  hasDigit: boolean;
  hasSpecial: boolean;
};

export function isValidEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value);
}

export function evaluatePasswordRules(password: string): PasswordRuleState {
  return {
    minLength: password.length >= PASSWORD_MIN_LENGTH,
    maxLength: password.length <= PASSWORD_MAX_LENGTH,
    hasUpper: UPPERCASE_PATTERN.test(password),
    hasLower: LOWERCASE_PATTERN.test(password),
    hasDigit: DIGIT_PATTERN.test(password),
    hasSpecial: SPECIAL_PATTERN.test(password)
  };
}

export function isStrongPassword(password: string): boolean {
  const rules = evaluatePasswordRules(password);
  return (
    rules.minLength &&
    rules.maxLength &&
    rules.hasUpper &&
    rules.hasLower &&
    rules.hasDigit &&
    rules.hasSpecial
  );
}
