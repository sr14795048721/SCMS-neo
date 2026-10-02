"use client";

import { ReactNode } from "react";
import styles from "../../styles/teacherShell.module.css";

type TeacherShellProps = {
  title?: string;
  subtitle?: string;
  leading?: ReactNode;
  profileName: string;
  profileHint: string;
  avatarUrl?: string | null;
  profileLoading?: boolean;
  onProfileClick?: () => void;
  logoutLabel: string;
  logoutPendingLabel: string;
  logoutSubmitting?: boolean;
  onLogout?: () => void;
  children: ReactNode;
};

export function TeacherShell({
  title,
  subtitle,
  leading,
  profileName,
  profileHint,
  avatarUrl,
  profileLoading = false,
  onProfileClick,
  logoutLabel,
  logoutPendingLabel,
  logoutSubmitting = false,
  onLogout,
  children
}: TeacherShellProps) {
  const initial = profileName.trim().charAt(0) || "T";

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerLeft}>
            {leading ? <div className={styles.leading}>{leading}</div> : null}
            {title || subtitle ? (
              <div className={styles.brandBlock}>
                {title ? <h1 className={styles.title}>{title}</h1> : null}
                {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
              </div>
            ) : null}
          </div>

          <div className={styles.headerActions}>
            <button
              type="button"
              className={styles.profileButton}
              onClick={onProfileClick}
              disabled={!onProfileClick}
            >
              <span
                className={`${styles.avatarFrame} ${
                  avatarUrl ? styles.avatarFrameImage : profileLoading ? styles.avatarFramePending : styles.avatarFrameFallback
                }`}
                aria-hidden="true"
              >
                {avatarUrl ? (
                  <img src={avatarUrl} alt="" className={styles.avatarImage} />
                ) : profileLoading ? (
                  <span className={styles.avatarSkeleton} />
                ) : (
                  <span className={styles.avatarFallback}>{initial}</span>
                )}
              </span>
              <span className={styles.profileCopy}>
                {profileLoading ? (
                  <>
                    <span className={`${styles.profileLine} ${styles.profileLinePrimary}`} />
                    <span className={`${styles.profileLine} ${styles.profileLineSecondary}`} />
                  </>
                ) : (
                  <>
                    <span className={styles.profileName} title={profileName}>
                      {profileName}
                    </span>
                    <span className={styles.profileHint} title={profileHint}>
                      {profileHint}
                    </span>
                  </>
                )}
              </span>
              {onProfileClick ? <i className={`fas fa-chevron-right ${styles.profileArrow}`} /> : null}
            </button>

            <button
              type="button"
              className={styles.logoutButton}
              onClick={onLogout}
              disabled={logoutSubmitting || !onLogout}
            >
              <i className="fas fa-right-from-bracket" />
              {logoutSubmitting ? logoutPendingLabel : logoutLabel}
            </button>
          </div>
        </div>
      </header>

      <section className={styles.content}>{children}</section>
    </main>
  );
}
