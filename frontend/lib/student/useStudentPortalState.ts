"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearPersistedAuthTokens } from "../auth/session";
import { useRoleGate } from "../auth/useRoleGate";
import { useT } from "../i18n/useT";
import { useNotify } from "../notify/useNotify";
import { fetchStudentAvatarRequest, fetchStudentInfoRequest } from "./client";
import { clearCachedStudentAvatar, readCachedStudentAvatar, writeCachedStudentAvatar } from "./avatarCache";
import { clearCachedStudentProfile, readCachedStudentProfile, writeCachedStudentProfile } from "./profileCache";
import { StudentInfo } from "./types";

export function useStudentPortalState(defaultName: string) {
  const tPortal = useT("portal");
  const tProfile = useT("studentProfile");
  const notify = useNotify();
  const router = useRouter();
  const roleGate = useRoleGate("STUDENT");
  const [studentInfo, setStudentInfo] = useState<StudentInfo | null>(() => readCachedStudentProfile());
  const [avatarUrl, setAvatarUrl] = useState<string | null>(() => {
    const cachedProfile = readCachedStudentProfile();
    return cachedProfile?.hasAvatar ? readCachedStudentAvatar(cachedProfile) : null;
  });
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    const loadStudentProfile = async () => {
      if (!roleGate.authorized) {
        return;
      }

      const response = await fetchStudentInfoRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status, notify, tPortal, tProfile, router))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | {
            success?: boolean;
            data?: StudentInfo;
          }
        | null;

      if (!response.ok || result?.success !== true || !result.data || !active) {
        return;
      }

      setStudentInfo(result.data);
      writeCachedStudentProfile(result.data);
      await loadAvatar(result.data);
    };

    void loadStudentProfile();
    return () => {
      active = false;
    };
  }, [notify, roleGate.authorized, router, tPortal, tProfile]);

  return {
    ...roleGate,
    studentInfo,
    avatarUrl,
    logoutSubmitting,
    profileName: studentInfo?.displayName || defaultName,
    handleLogout,
    handleAuthStatus: (status: number) => handleAuthStatus(status, notify, tPortal, tProfile, router)
  };

  async function loadAvatar(profile: StudentInfo) {
    if (!profile.hasAvatar) {
      clearCachedStudentAvatar(profile.userId);
      setAvatarUrl(null);
      return;
    }

    const cachedAvatar = readCachedStudentAvatar(profile);
    if (cachedAvatar) {
      setAvatarUrl(cachedAvatar);
      return;
    }

    const response = await fetchStudentAvatarRequest(profile.avatarUpdatedAt);
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status, notify, tPortal, tProfile, router))) {
      return;
    }
    if (!response.ok) {
      setAvatarUrl(null);
      return;
    }

    const blob = await response.blob();
    setAvatarUrl(await writeCachedStudentAvatar(profile, blob));
  }

  function handleLogout() {
    if (logoutSubmitting) {
      return;
    }

    setLogoutSubmitting(true);
    if (studentInfo?.userId) {
      clearCachedStudentAvatar(studentInfo.userId);
    }
    clearCachedStudentProfile();
    clearPersistedAuthTokens();
    router.replace("/login");
  }
}

async function handleAuthStatus(
  status: number,
  notify: ReturnType<typeof useNotify>,
  tPortal: ReturnType<typeof useT>,
  tProfile: ReturnType<typeof useT>,
  router: ReturnType<typeof useRouter>
) {
  if (status === 401) {
    clearCachedStudentProfile();
    notify.warning(tPortal("auth.loginRequired"));
    router.replace("/login");
    return false;
  }

  if (status === 403) {
    clearCachedStudentProfile();
    notify.error(tPortal("auth.noPermission"));
    router.replace("/login");
    return false;
  }

  if (status === 428) {
    notify.warning(tProfile("completion.redirectNotice"));
    router.replace("/student/profile");
    return false;
  }

  return true;
}
