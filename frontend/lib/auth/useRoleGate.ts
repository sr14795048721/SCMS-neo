"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useNotify } from "../notify/useNotify";
import { useT } from "../i18n/useT";
import { AuthRequestError, getValidAccessToken } from "./client";
import { extractRoleFromAccessToken, getPersistedAuthTokens, UserRole } from "./session";

type RoleGateState = {
  authorized: boolean;
  checking: boolean;
  redirecting: boolean;
};

const initialState: RoleGateState = {
  authorized: false,
  checking: true,
  redirecting: false
};

type RoleRequirement = UserRole | ReadonlyArray<UserRole>;

export function useRoleGate(requiredRole: RoleRequirement): RoleGateState {
  const t = useT("portal");
  const notify = useNotify();
  const router = useRouter();
  const checkedRef = useRef(false);
  const [state, setState] = useState<RoleGateState>(initialState);

  useEffect(() => {
    if (checkedRef.current) {
      return;
    }
    checkedRef.current = true;

    let active = true;

    const verifyRole = async () => {
      const tokens = getPersistedAuthTokens();
      const fallbackRole = extractRoleFromAccessToken(tokens?.accessToken);

      try {
        const accessToken = await getValidAccessToken();
        if (!active) {
          return;
        }

        const role = extractRoleFromAccessToken(accessToken);
        if (!accessToken || !role) {
          notify.warning(t("auth.loginRequired"));
          setState({
            authorized: false,
            checking: false,
            redirecting: true
          });
          router.replace("/login");
          return;
        }

        if (!matchesRoleRequirement(role, requiredRole)) {
          notify.error(t("auth.noPermission"));
          setState({
            authorized: false,
            checking: false,
            redirecting: true
          });
          router.replace("/login");
          return;
        }

        setState({
          authorized: true,
          checking: false,
          redirecting: false
        });
      } catch (error) {
        if (!active) {
          return;
        }

        if (
          error instanceof AuthRequestError &&
          error.isRetryable &&
          fallbackRole &&
          matchesRoleRequirement(fallbackRole, requiredRole)
        ) {
          setState({
            authorized: true,
            checking: false,
            redirecting: false
          });
          return;
        }

        notify.warning(t("auth.loginRequired"));
        setState({
          authorized: false,
          checking: false,
          redirecting: true
        });
        router.replace("/login");
      }
    };

    void verifyRole();

    return () => {
      active = false;
    };
  }, [notify, requiredRole, router, t]);

  return state;
}

function matchesRoleRequirement(role: UserRole, requiredRole: RoleRequirement): boolean {
  const requiredRoles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
  return requiredRoles.some((candidate) => {
    if (candidate === role) {
      return true;
    }
    return candidate === "ADMIN" && role === "SUPER_ADMIN";
  });
}
