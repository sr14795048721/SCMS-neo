"use client";

import Link from "next/link";
import { UserRole } from "../../lib/auth/session";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import styles from "../../styles/portal.module.css";

type ProtectedRolePageProps = {
  requiredRole: UserRole;
  titleKey: string;
  descriptionKey: string;
};

export function ProtectedRolePage({
  requiredRole,
  titleKey,
  descriptionKey
}: ProtectedRolePageProps) {
  const t = useT("portal");
  const { authorized, checking, redirecting } = useRoleGate(requiredRole);

  return (
    <main className={styles.page}>
      <section className={styles.card}>
        {checking ? (
          <p>{t("auth.checking")}</p>
        ) : redirecting ? null : authorized ? (
          <>
            <h1>{t(titleKey)}</h1>
            <p>{t(descriptionKey)}</p>
            <div className={styles.actions}>
              <Link href="/">{t("actions.backHome")}</Link>
              <Link href="/login">{t("actions.goLogin")}</Link>
            </div>
          </>
        ) : (
          <p>{t("auth.noPermission")}</p>
        )}
      </section>
    </main>
  );
}
