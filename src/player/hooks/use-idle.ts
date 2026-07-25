import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Tracks "is the user doing anything?" so the chrome can fade away during
 * playback. Any pointer or key activity wakes it back up.
 */
export function useIdle(delay = 2800, enabled = true) {
  const [isIdle, setIsIdle] = useState(false);
  const timer = useRef<number | null>(null);
  /** While true (menu open, scrubbing), the controls never hide. */
  const held = useRef(false);

  const clear = () => {
    if (timer.current !== null) window.clearTimeout(timer.current);
    timer.current = null;
  };

  const wake = useCallback(() => {
    setIsIdle(false);
    clear();
    if (!enabled || held.current) return;
    timer.current = window.setTimeout(() => setIsIdle(true), delay);
  }, [delay, enabled]);

  const hold = useCallback(
    (value: boolean) => {
      held.current = value;
      wake();
    },
    [wake],
  );

  useEffect(() => {
    if (!enabled) {
      setIsIdle(false);
      clear();
      return;
    }
    wake();
    return clear;
  }, [enabled, wake]);

  return { isIdle: isIdle && enabled, wake, hold };
}
