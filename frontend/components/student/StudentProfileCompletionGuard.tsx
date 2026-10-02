"use client";

import { ReactNode, useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useRoleGate } from "../../lib/auth/useRoleGate";
import { useT } from "../../lib/i18n/useT";
import { useNotify } from "../../lib/notify/useNotify";
import { fetchStudentInfoRequest } from "../../lib/student/client";
import { clearCachedStudentProfile, readCachedStudentProfile, writeCachedStudentProfile } from "../../lib/student/profileCache";
import { isStudentProfileCompleted } from "../../lib/student/profileCompletion";
import { StudentInfo } from "../../lib/student/types";
import shellStyles from "../../styles/teacherShell.module.css";

type StudentInfoResponse = {
  success?: boolean;
  data?: StudentInfo;
};

type StudentProfileCompletionGuardProps = {
  children: ReactNode;
};

export function StudentProfileCompletionGuard({ children }: StudentProfileCompletionGuardProps) {
  const pathname = usePathname();
  const router = useRouter();
  const notify = useNotify();
  const tPortal = useT("portal");
  const tProfile = useT("studentProfile");
  const { authorized, checking, redirecting } = useRoleGate("STUDENT");
  const [ready, setReady] = useState(false);
  const notifiedRef = useRef(false);
  const isProfileRoute = pathname.startsWith("/student/profile");

  useEffect(() => {
    if (!authorized) {
      return;
    }

    if (isProfileRoute) {
      setReady(true);
      return;
    }

    setReady(false);

    const cachedProfile = readCachedStudentProfile();
    if (cachedProfile && !isStudentProfileCompleted(cachedProfile)) {
      redirectToProfile();
      return;
    }

    let active = true;

    const verifyProfile = async () => {
      const response = await fetchStudentInfoRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 401) {
        clearCachedStudentProfile();
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (response.status === 403) {
        clearCachedStudentProfile();
        notify.error(tPortal("auth.noPermission"));
        router.replace("/login");
        return;
      }

      const result = (await response.json().catch(() => null)) as StudentInfoResponse | null;
      if (!response.ok || result?.success !== true || !result.data) {
        redirectToProfile();
        return;
      }

      writeCachedStudentProfile(result.data);
      if (!result.data.profileCompleted) {
        redirectToProfile();
        return;
      }

      if (active) {
        setReady(true);
      }
    };

    void verifyProfile();
    return () => {
      active = false;
    };
  }, [authorized, isProfileRoute, notify, router, tPortal, tProfile]);

  if (checking || (!isProfileRoute && authorized && !ready)) {
    return (
      <main className={shellStyles.page}>
        <section className={shellStyles.loadingCard}>
          <p>{checking ? tPortal("auth.checking") : tProfile("completion.checking")}</p>
        </section>
      </main>
    );
  }

  if (!authorized || redirecting) {
    return null;
  }

  return <>{children}</>;

  function redirectToProfile() {
    if (!notifiedRef.current) {
      notify.warning(tProfile("completion.redirectNotice"));
      notifiedRef.current = true;
    }
    router.replace("/student/profile");
  }
}
