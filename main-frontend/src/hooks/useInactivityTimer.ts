import { useEffect } from "react";
import { fromEvent, merge, Subscription, timer } from "rxjs";
import { startWith, switchMap } from "rxjs/operators";

const DEFAULT_EVENTS = ["mousemove", "keydown", "mousedown", "scroll", "touchstart"];

export const useInactivityTimer = (
  timeoutMs: number,
  onTimeout: () => void,
  enabled: boolean = true
) => {
  useEffect(() => {
    if (!enabled || !timeoutMs) return;

    const activity$ = merge(...DEFAULT_EVENTS.map((event) => fromEvent(window, event)));
    const subscription: Subscription = activity$
      .pipe(
        startWith(null),
        switchMap(() => timer(timeoutMs))
      )
      .subscribe(onTimeout);

    return () => subscription.unsubscribe();
  }, [timeoutMs, onTimeout, enabled]);
};