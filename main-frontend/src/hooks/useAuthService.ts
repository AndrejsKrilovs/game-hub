import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { eventBus } from "shared-frontend";
import { useInactivityTimer } from "./useInactivityTimer";

interface UserResponse {
  username: string;
  inactivityTimeoutMs: number;
}

interface AuthState {
  username: string | null;
  inactivityTimeoutMs: number;
}

const TOAST_STORAGE_KEY = "session_expired_toast";
const showToast = (message: string, type: "info" | "success" | "error" = "error") => {
  eventBus.emit("TOAST", { message, type });
};
const redirectAfterLoginIfNeeded = () => {
  const redirect = new URLSearchParams(window.location.search).get("redirect");
  if (!redirect || !redirect.startsWith("/") || redirect.startsWith("//")) {
    return null;
  }
  if (redirect) {
    window.location.replace(redirect);
  }
};
export const useAuthService = () => {
  const [authState, setAuthState] = useState<AuthState>({
    username: null,
    inactivityTimeoutMs: 0,
  });

  const [isAuthChecking, setIsAuthChecking] = useState(true);

  const setAuthenticatedUser = useCallback((user: UserResponse) => {
    setAuthState({ username: user.username,  inactivityTimeoutMs: user.inactivityTimeoutMs ?? 0 });
  }, []);

  const clearAuthenticatedUser = useCallback(() => {
    setAuthState({ username: null, inactivityTimeoutMs: 0 });
  }, []);

  useEffect(() => {
    const pendingMessage = sessionStorage.getItem(TOAST_STORAGE_KEY);
    if (!pendingMessage) {
      return;
    }

    sessionStorage.removeItem(TOAST_STORAGE_KEY);
    const timerId = window.setTimeout(() => showToast(pendingMessage), 100);
    return () => window.clearTimeout(timerId);
  }, []);

  const handleSessionExpired = useCallback(
    (message: string) => {
      clearAuthenticatedUser();
      if (window.location.pathname !== "/") {
        sessionStorage.setItem(TOAST_STORAGE_KEY, message);
        window.location.replace("/");
        return;
      }

      showToast(message);
    },
    [clearAuthenticatedUser]
  );

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        const url = error.config?.url;
        if (
          axios.isAxiosError(error) &&
          error.response?.status === 401 &&
          url !== "/api/me" &&
          url !== "/api/login"
        ) {
          handleSessionExpired("Сессия истекла.");
        }
        return Promise.reject(error);
      }
    );

    axios
      .get<UserResponse>("/api/me")
      .then((res) => {
        setAuthenticatedUser(res.data);
        redirectAfterLoginIfNeeded();
      })
      .catch(clearAuthenticatedUser)
      .finally(() => setIsAuthChecking(false));

    return () => axios.interceptors.response.eject(interceptor);
  }, [handleSessionExpired, setAuthenticatedUser, clearAuthenticatedUser]);

  const handleInactivityTimeout = useCallback(() => {
    axios.post("/api/logout").finally(() => {
      handleSessionExpired("Сессия завершена из-за неактивности.");
    });
  }, [handleSessionExpired]);

  useInactivityTimer(authState.inactivityTimeoutMs, handleInactivityTimeout, Boolean(authState.username));

  const login = async (name: string) => {
    const params = new URLSearchParams({ username: name, password: "" });
    await axios.post("/api/login", params);

    const { data } = await axios.get<UserResponse>("/api/me");
    setAuthenticatedUser(data);
    redirectAfterLoginIfNeeded();
  };

  const logout = async () => {
    axios.post("/api/logout").finally(() => {
      clearAuthenticatedUser();
      if (window.location.pathname !== "/") {
        window.location.replace("/");
      }
    });
  };

  return { username: authState.username, isAuthChecking, login, logout };
};