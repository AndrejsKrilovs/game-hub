import { useEffect, type RefObject } from 'react';

export const useAutoScroll = (elementRef: RefObject<HTMLElement | null>, dependency: unknown): void => {
  useEffect(() => {
    const element = elementRef.current;
    if (!element) {
      return;
    }

    element.scrollTop = element.scrollHeight;
  }, [elementRef, dependency]);
};