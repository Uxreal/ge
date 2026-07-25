import { useCallback, useEffect, useState } from "react";

/** Safari (and older WebKit) still ship the prefixed fullscreen API. */
interface WebkitFullscreenElement extends HTMLElement {
  webkitRequestFullscreen?: () => Promise<void> | void;
}
interface WebkitDocument extends Document {
  webkitFullscreenElement?: Element | null;
  webkitExitFullscreen?: () => Promise<void> | void;
}

export function useFullscreen(ref: React.RefObject<HTMLElement | null>) {
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const sync = () => {
      const doc = document as WebkitDocument;
      setIsFullscreen(Boolean(doc.fullscreenElement ?? doc.webkitFullscreenElement));
    };
    document.addEventListener("fullscreenchange", sync);
    document.addEventListener("webkitfullscreenchange", sync);
    sync();
    return () => {
      document.removeEventListener("fullscreenchange", sync);
      document.removeEventListener("webkitfullscreenchange", sync);
    };
  }, []);

  const toggle = useCallback(async () => {
    const doc = document as WebkitDocument;
    const element = ref.current as WebkitFullscreenElement | null;
    if (!element) return;

    const active = doc.fullscreenElement ?? doc.webkitFullscreenElement;
    try {
      if (active) {
        await (doc.exitFullscreen?.() ?? doc.webkitExitFullscreen?.());
      } else {
        await (element.requestFullscreen?.() ?? element.webkitRequestFullscreen?.());
      }
    } catch {
      // Fullscreen can be refused (iframe without allowfullscreen, iPhone
      // Safari). Nothing to do but leave the player inline.
    }
  }, [ref]);

  return { isFullscreen, toggle };
}

interface PipVideoElement extends HTMLVideoElement {
  /** iOS Safari's equivalent of Picture-in-Picture. */
  webkitSupportsPresentationMode?: (mode: string) => boolean;
  webkitSetPresentationMode?: (mode: string) => void;
}

export function usePictureInPicture(
  videoRef: React.RefObject<HTMLVideoElement | null>,
) {
  const [isPip, setIsPip] = useState(false);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    const enter = () => setIsPip(true);
    const leave = () => setIsPip(false);
    video.addEventListener("enterpictureinpicture", enter);
    video.addEventListener("leavepictureinpicture", leave);
    return () => {
      video.removeEventListener("enterpictureinpicture", enter);
      video.removeEventListener("leavepictureinpicture", leave);
    };
  }, [videoRef]);

  const supported =
    typeof document !== "undefined" &&
    ("pictureInPictureEnabled" in document
      ? document.pictureInPictureEnabled
      : false);

  const toggle = useCallback(async () => {
    const video = videoRef.current as PipVideoElement | null;
    if (!video) return;
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (video.requestPictureInPicture) {
        await video.requestPictureInPicture();
      } else if (video.webkitSupportsPresentationMode?.("picture-in-picture")) {
        video.webkitSetPresentationMode?.(
          isPip ? "inline" : "picture-in-picture",
        );
        setIsPip((v) => !v);
      }
    } catch {
      // Chrome rejects PiP for videos with no video track, and while the
      // element is still loading. Silently staying inline is the right answer.
    }
  }, [videoRef, isPip]);

  return { isPip, supported, toggle };
}
