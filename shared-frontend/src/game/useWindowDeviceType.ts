import { useEffect, useState } from 'react';
import { distinctUntilChanged, fromEvent, map, startWith } from 'rxjs';

export type DeviceType = 'desktop' | 'ipad' | 'mobile';

const DEVICE_WIDTH = {
  mobile: 600,
  ipad: 900,
} as const;

const getDeviceType = (): DeviceType => {
  const width = window.innerWidth;
  if (width <= DEVICE_WIDTH.mobile) {
    return 'mobile';
  }
  if (width <= DEVICE_WIDTH.ipad) {
    return 'ipad';
  }

  return 'desktop';
};

export const useWindowDeviceType = (): DeviceType => {
  const [deviceType, setDeviceType] = useState<DeviceType>(() => getDeviceType());

  useEffect(() => {
    const subscription = fromEvent(window, 'resize')
      .pipe(
        startWith(null),
        map(() => getDeviceType()),
        distinctUntilChanged()
      )
      .subscribe(setDeviceType);

    return () => subscription.unsubscribe();
  }, []);

  return deviceType;
};