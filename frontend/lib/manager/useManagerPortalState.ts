"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearPersistedAuthTokens } from "../auth/session";
import { useRoleGate } from "../auth/useRoleGate";
import { useT } from "../i18n/useT";
import { useNotify } from "../notify/useNotify";
import { fetchManagerAvatarRequest, fetchManagerInfoRequest } from "./client";
import { clearCachedManagerAvatar, readCachedManagerAvatar, writeCachedManagerAvatar } from "./avatarCache";
import { clearCachedManagerProfile, readCachedManagerProfile, writeCachedManagerProfile } from "./profileCache";
import { ManagerInfo } from "./types";

export function useManagerPortalState(defaultName: string) {
  const tPortal = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const roleGate = useRoleGate("CLUB_MANAGER");
  const [managerInfo, setManagerInfo] = useState<ManagerInfo | null>(() => readCachedManagerProfile());
  const [avatarUrl, setAvatarUrl] = useState<string | null>(() => {
    const cachedProfile = readCachedManagerProfile();
    return cachedProfile?.hasAvatar ? readCachedManagerAvatar(cachedProfile) : null;
  });
  const [logoutSubmitting, setLogoutSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    const loadManagerProfile = async () => {
      if (!roleGate.authorized) {
        return;
      }

      const response = await fetchManagerInfoRequest();
      if (!response) {
        notify.warning(tPortal("auth.loginRequired"));
        router.replace("/login");
        return;
      }

      if (!(await handleAuthStatus(response.status, notify, tPortal, router))) {
        return;
      }

      const result = (await response.json().catch(() => null)) as
        | {
            success?: boolean;
            data?: ManagerInfo;
          }
        | null;

      if (!response.ok || result?.success !== true || !result.data || !active) {
        return;
      }

      setManagerInfo(result.data);
      writeCachedManagerProfile(result.data);
      await loadAvatar(result.data);
    };

    void loadManagerProfile();
    return () => {
      active = false;
    };
  }, [notify, roleGate.authorized, router, tPortal]);

  return {
    ...roleGate,
    managerInfo,
    avatarUrl,
    logoutSubmitting,
    profileName: managerInfo?.displayName || defaultName,
    handleLogout,
    handleAuthStatus: (status: number) => handleAuthStatus(status, notify, tPortal, router)
  };

  async function loadAvatar(profile: ManagerInfo) {
    if (!profile.hasAvatar) {
      clearCachedManagerAvatar(profile.userId);
      setAvatarUrl(null);
      return;
    }

    const cachedAvatar = readCachedManagerAvatar(profile);
    if (cachedAvatar) {
      setAvatarUrl(cachedAvatar);
      return;
    }

    const response = await fetchManagerAvatarRequest(profile.avatarUpdatedAt);
    if (!response) {
      return;
    }
    if (!(await handleAuthStatus(response.status, notify, tPortal, router))) {
      return;
    }
    if (!response.ok) {
      setAvatarUrl(null);
      return;
    }

    const blob = await response.blob();
    setAvatarUrl(await writeCachedManagerAvatar(profile, blob));
  }

  function handleLogout() {
    if (logoutSubmitting) {
      return;
    }

    setLogoutSubmitting(true);
    if (managerInfo?.userId) {
      clearCachedManagerAvatar(managerInfo.userId);
    }
    clearCachedManagerProfile();
    clearPersistedAuthTokens();
    router.replace("/login");
  }
}

async function handleAuthStatus(
  status: number,
  notify: ReturnType<typeof useNotify>,
  tPortal: ReturnType<typeof useT>,
  router: ReturnType<typeof useRouter>
) {
  if (status === 401) {
    clearCachedManagerProfile();
    notify.warning(tPortal("auth.loginRequired"));
    router.replace("/login");
    return false;
  }

  if (status === 403) {
    clearCachedManagerProfile();
    notify.error(tPortal("auth.noPermission"));
    router.replace("/login");
    return false;
  }

  return true;
}
