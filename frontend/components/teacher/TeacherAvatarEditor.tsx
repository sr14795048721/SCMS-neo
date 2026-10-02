"use client";

import { ChangeEvent, PointerEvent, WheelEvent, useEffect, useRef, useState } from "react";
import { useT } from "../../lib/i18n/useT";
import styles from "../../styles/teacherAvatarEditor.module.css";

type Point = { x: number; y: number };

type TeacherAvatarEditorProps = {
  file: File;
  previewUrl: string;
  submitting: boolean;
  onCancel: () => void;
  onChangeFile: (file: File) => void;
  onSubmit: (blob: Blob) => Promise<void> | void;
};

type RenderOptions = {
  image: HTMLImageElement;
  canvas: HTMLCanvasElement;
  size: number;
  naturalWidth: number;
  naturalHeight: number;
  baseScale: number;
  zoom: number;
  rotation: number;
  position: Point;
};

const STAGE_SIZE = 320;
const EXPORT_SIZE = 720;
const PREVIEW_SIZE = 180;
const MIN_ZOOM = 0.2;
const MAX_ZOOM = 6;
const ZOOM_STEP = 0.12;
const MIN_ROTATION = -180;
const MAX_ROTATION = 180;
const ROTATION_STEP = 15;

export function TeacherAvatarEditor({
  file,
  previewUrl,
  submitting,
  onCancel,
  onChangeFile,
  onSubmit
}: TeacherAvatarEditorProps) {
  const t = useT("teacherProfile");
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const dragRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    origin: Point;
  } | null>(null);

  const [zoom, setZoom] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [position, setPosition] = useState<Point>({ x: 0, y: 0 });
  const [naturalSize, setNaturalSize] = useState({ width: STAGE_SIZE, height: STAGE_SIZE });
  const [baseScale, setBaseScale] = useState(1);
  const [sourceImage, setSourceImage] = useState<HTMLImageElement | null>(null);
  const [previewDataUrl, setPreviewDataUrl] = useState<string>("");

  useEffect(() => {
    const previousBodyOverflow = document.body.style.overflow;
    const previousBodyOverscroll = document.body.style.overscrollBehavior;
    const previousHtmlOverflow = document.documentElement.style.overflow;

    document.body.style.overflow = "hidden";
    document.body.style.overscrollBehavior = "contain";
    document.documentElement.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.body.style.overscrollBehavior = previousBodyOverscroll;
      document.documentElement.style.overflow = previousHtmlOverflow;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const image = new Image();
    image.onload = () => {
      if (!active) {
        return;
      }
      const width = image.naturalWidth || STAGE_SIZE;
      const height = image.naturalHeight || STAGE_SIZE;
      setSourceImage(image);
      setNaturalSize({ width, height });
      setBaseScale(Math.max(STAGE_SIZE / width, STAGE_SIZE / height));
      setZoom(1);
      setRotation(0);
      setPosition({ x: 0, y: 0 });
    };
    image.src = previewUrl;

    return () => {
      active = false;
    };
  }, [previewUrl]);

  useEffect(() => {
    if (!sourceImage) {
      return;
    }

    const canvas = document.createElement("canvas");
    renderAvatarToCanvas({
      image: sourceImage,
      canvas,
      size: PREVIEW_SIZE,
      naturalWidth: naturalSize.width,
      naturalHeight: naturalSize.height,
      baseScale,
      zoom,
      rotation,
      position
    });
    setPreviewDataUrl(canvas.toDataURL("image/png"));
  }, [baseScale, naturalSize.height, naturalSize.width, position, rotation, sourceImage, zoom]);

  const renderScale = baseScale * zoom;

  const handlePointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (submitting) {
      return;
    }

    event.preventDefault();
    dragRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      origin: position
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current || dragRef.current.pointerId !== event.pointerId) {
      return;
    }

    setPosition({
      x: dragRef.current.origin.x + (event.clientX - dragRef.current.startX),
      y: dragRef.current.origin.y + (event.clientY - dragRef.current.startY)
    });
  };

  const handlePointerEnd = (event: PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current || dragRef.current.pointerId !== event.pointerId) {
      return;
    }

    dragRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  };

  const handleWheel = (event: WheelEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    const next = zoom + (event.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP);
    setZoom(clamp(next, MIN_ZOOM, MAX_ZOOM));
  };

  const handleReplaceFile = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFile = event.target.files?.[0];
    event.target.value = "";
    if (!nextFile) {
      return;
    }
    onChangeFile(nextFile);
  };

  const handleReset = () => {
    setZoom(1);
    setRotation(0);
    setPosition({ x: 0, y: 0 });
  };

  const handleSubmit = async () => {
    if (!sourceImage) {
      return;
    }

    const canvas = document.createElement("canvas");
    renderAvatarToCanvas({
      image: sourceImage,
      canvas,
      size: EXPORT_SIZE,
      naturalWidth: naturalSize.width,
      naturalHeight: naturalSize.height,
      baseScale,
      zoom,
      rotation,
      position
    });

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (nextBlob) => {
          if (!nextBlob) {
            reject(new Error("avatar export failed"));
            return;
          }
          resolve(nextBlob);
        },
        "image/png",
        0.92
      );
    });

    await onSubmit(blob);
  };

  return (
    <div className={styles.layer} role="presentation" onClick={onCancel}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="teacher-avatar-editor-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.header}>
          <div className={styles.headerCopy}>
            <h2 id="teacher-avatar-editor-title" className={styles.title}>
              {t("avatarEditor.title")}
            </h2>
            <p className={styles.subtitle}>{t("avatarEditor.subtitle")}</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onCancel} disabled={submitting}>
            <i className="fas fa-xmark" />
          </button>
        </div>

        <div className={styles.body}>
          <section className={styles.canvasColumn}>
            <div className={styles.stageCard}>
              <div
                className={styles.stageViewport}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerEnd}
                onPointerCancel={handlePointerEnd}
                onPointerLeave={handlePointerEnd}
                onWheel={handleWheel}
              >
                {sourceImage ? (
                  <img
                    src={previewUrl}
                    alt={file.name}
                    className={styles.stageImage}
                    draggable={false}
                    style={{
                      width: `${naturalSize.width}px`,
                      height: `${naturalSize.height}px`,
                      transform: [
                        "translate(-50%, -50%)",
                        `translate(${position.x}px, ${position.y}px)`,
                        `rotate(${rotation}deg)`,
                        `scale(${renderScale})`
                      ].join(" ")
                    }}
                  />
                ) : (
                  <div className={styles.emptyState}>{t("avatarEditor.empty")}</div>
                )}
                <div className={`${styles.cropOverlay} ${styles.circleOverlay}`} />
              </div>
            </div>

            <div className={styles.stageTip}>
              <i className="fas fa-up-down-left-right" />
              <span>{t("avatarEditor.dragHint")}</span>
            </div>
          </section>

          <aside className={styles.sidePanel}>
            <div className={styles.previewCard}>
              <div className={styles.previewHeader}>
                <span className={styles.previewLabel}>{t("avatarEditor.preview")}</span>
                <span className={styles.previewMeta}>{t("avatarEditor.circle")}</span>
              </div>
              <div className={`${styles.previewFrame} ${styles.circlePreview}`}>
                {previewDataUrl ? <img src={previewDataUrl} alt="" className={styles.previewImage} draggable={false} /> : null}
              </div>

              <div className={styles.controlSection}>
                <div className={styles.controlHeader}>
                  <span>{t("avatarEditor.zoom")}</span>
                  <strong>{Math.round(zoom * 100)}%</strong>
                </div>
                <div className={styles.rangeRow}>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => setZoom((value) => clamp(value - ZOOM_STEP, MIN_ZOOM, MAX_ZOOM))}
                    disabled={submitting}
                  >
                    <i className="fas fa-magnifying-glass-minus" />
                  </button>
                  <input
                    type="range"
                    min={MIN_ZOOM}
                    max={MAX_ZOOM}
                    step="0.01"
                    value={zoom}
                    className={styles.range}
                    onChange={(event) => setZoom(Number(event.target.value))}
                    disabled={submitting}
                  />
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => setZoom((value) => clamp(value + ZOOM_STEP, MIN_ZOOM, MAX_ZOOM))}
                    disabled={submitting}
                  >
                    <i className="fas fa-magnifying-glass-plus" />
                  </button>
                </div>
              </div>

            </div>

            <div className={styles.controlsCard}>
              <div className={styles.toolbar}>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => fileInputRef.current?.click()}
                  disabled={submitting}
                >
                  <i className="fas fa-image" />
                  {t("avatarEditor.replace")}
                </button>
                <button type="button" className={styles.secondaryButton} onClick={handleReset} disabled={submitting}>
                  <i className="fas fa-rotate-left" />
                  {t("avatarEditor.reset")}
                </button>
              </div>

              <div className={styles.controlSection}>
                <div className={styles.controlHeader}>
                  <span>{t("avatarEditor.rotate")}</span>
                  <strong>{rotation}deg</strong>
                </div>
                <input
                  type="range"
                  min={MIN_ROTATION}
                  max={MAX_ROTATION}
                  step="1"
                  value={rotation}
                  className={styles.range}
                  onChange={(event) => setRotation(clamp(Number(event.target.value), MIN_ROTATION, MAX_ROTATION))}
                  disabled={submitting}
                />
                <div className={styles.rotationActions}>
                  <button
                    type="button"
                    className={`${styles.secondaryButton} ${styles.compactActionButton}`}
                    onClick={() => setRotation((value) => clamp(value - ROTATION_STEP, MIN_ROTATION, MAX_ROTATION))}
                    disabled={submitting}
                  >
                    <i className="fas fa-rotate-left" />
                    {t("avatarEditor.rotateLeft")}
                  </button>
                  <button
                    type="button"
                    className={`${styles.secondaryButton} ${styles.compactActionButton}`}
                    onClick={() => setRotation(0)}
                    disabled={submitting}
                  >
                    <i className="fas fa-compass" />
                    {t("avatarEditor.rotateReset")}
                  </button>
                  <button
                    type="button"
                    className={`${styles.secondaryButton} ${styles.compactActionButton}`}
                    onClick={() => setRotation((value) => clamp(value + ROTATION_STEP, MIN_ROTATION, MAX_ROTATION))}
                    disabled={submitting}
                  >
                    <i className="fas fa-rotate-right" />
                    {t("avatarEditor.rotateRight")}
                  </button>
                </div>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/jpg"
                hidden
                onChange={handleReplaceFile}
              />
            </div>
          </aside>
        </div>

        <div className={styles.footer}>
          <button type="button" className={styles.cancelButton} onClick={onCancel} disabled={submitting}>
            {t("avatarEditor.cancel")}
          </button>
          <button type="button" className={styles.submitButton} onClick={() => void handleSubmit()} disabled={submitting}>
            <i className="fas fa-cloud-arrow-up" />
            {submitting ? t("avatarEditor.submitting") : t("avatarEditor.submit")}
          </button>
        </div>
      </div>
    </div>
  );
}

function renderAvatarToCanvas({
  image,
  canvas,
  size,
  naturalWidth,
  naturalHeight,
  baseScale,
  zoom,
  rotation,
  position
}: RenderOptions) {
  canvas.width = size;
  canvas.height = size;

  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("avatar render context unavailable");
  }

  const ratio = size / STAGE_SIZE;
  context.clearRect(0, 0, size, size);

  context.save();
  context.beginPath();
  context.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
  context.closePath();
  context.clip();

  context.translate(size / 2 + position.x * ratio, size / 2 + position.y * ratio);
  context.rotate((rotation * Math.PI) / 180);
  context.scale(baseScale * zoom * ratio, baseScale * zoom * ratio);
  context.drawImage(image, -naturalWidth / 2, -naturalHeight / 2, naturalWidth, naturalHeight);

  context.restore();
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}
