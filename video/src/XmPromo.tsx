import React from "react";
import {
  AbsoluteFill,
  Easing,
  Img,
  Sequence,
  interpolate,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Background, C, Monogram, Waves, fontFamily, gradient } from "./brand";

/** Screenshots are 1206 x 2622 (iPhone 17 Pro); the phone shows them at this width. */
const SHOT_W = 1206;
const SHOT_H = 2622;
const PHONE_W = 620;
const SCALE = PHONE_W / SHOT_W;
const PHONE_H = SHOT_H * SCALE;

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** Fades a scene in and out over its own duration. */
const useSceneFade = (duration: number, edge = 10) => {
  const frame = useCurrentFrame();
  return interpolate(frame, [0, edge, duration - edge, duration], [0, 1, 1, 0], clamp);
};

const Phone = ({ children, enter = 0 }: { children: React.ReactNode; enter?: number }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const rise = spring({ frame: frame - enter, fps, config: { damping: 200 } });
  return (
    <div
      style={{
        position: "absolute",
        left: (1080 - PHONE_W) / 2,
        top: 560 + (1 - rise) * 260,
        width: PHONE_W,
        height: PHONE_H,
        borderRadius: 78,
        overflow: "hidden",
        opacity: rise,
        boxShadow: `0 0 0 10px #1C1A29, 0 0 0 12px ${C.outline}, 0 40px 120px rgba(109,91,255,0.35)`,
        background: C.night,
      }}
    >
      {children}
    </div>
  );
};

const Shot = ({ name, opacity = 1 }: { name: string; opacity?: number }) => (
  <Img src={staticFile(`screens/${name}.png`)} style={{ position: "absolute", width: PHONE_W, height: PHONE_H, opacity }} />
);

/** Draws on top of a screenshot, in the screenshot's own pixel coordinates. */
const Overlay = ({ x, y, w, h, children, style }: { x: number; y: number; w: number; h: number; children?: React.ReactNode; style?: React.CSSProperties }) => (
  <div style={{ position: "absolute", left: x * SCALE, top: y * SCALE, width: w * SCALE, height: h * SCALE, ...style }}>{children}</div>
);

const Caption = ({ kicker, title, sub, delay = 0 }: { kicker?: string; title: string; sub?: string; delay?: number }) => {
  const frame = useCurrentFrame();
  const t = interpolate(frame - delay, [0, 14], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
  return (
    <div
      style={{
        position: "absolute",
        top: 150,
        left: 70,
        right: 70,
        textAlign: "center",
        opacity: t,
        transform: `translateY(${(1 - t) * 30}px)`,
      }}
    >
      {kicker && (
        <div
          style={{
            fontSize: 34,
            fontWeight: 800,
            letterSpacing: "0.32em",
            backgroundImage: gradient,
            WebkitBackgroundClip: "text",
            color: "transparent",
            marginBottom: 22,
          }}
        >
          {kicker}
        </div>
      )}
      <div style={{ fontSize: 64, fontWeight: 800, lineHeight: 1.12, color: C.snow }}>{title}</div>
      {sub && <div style={{ fontSize: 34, color: C.mist, marginTop: 20, fontWeight: 400 }}>{sub}</div>}
    </div>
  );
};

const Wordmark = ({ progress, size = 1 }: { progress: number; size?: number }) => (
  <div style={{ textAlign: "center", opacity: progress, transform: `translateY(${(1 - progress) * 20}px)` }}>
    <div style={{ fontSize: 92 * size, fontWeight: 900, letterSpacing: "0.04em", color: C.snow }}>XPERIENCE</div>
    <div
      style={{
        fontSize: 56 * size,
        fontWeight: 600,
        letterSpacing: `${0.2 + 0.42 * progress}em`,
        marginLeft: `${0.2 + 0.42 * progress}em`,
        backgroundImage: gradient,
        WebkitBackgroundClip: "text",
        color: "transparent",
      }}
    >
      MUSIC
    </div>
  </div>
);

const Tagline = ({ progress }: { progress: number }) => (
  <div style={{ textAlign: "center", fontSize: 30, letterSpacing: "0.22em", color: C.mist, opacity: progress, marginTop: 40 }}>
    RÉPÉTER · ENREGISTRER · GRANDIR
  </div>
);

/* ---------- Scenes ---------- */

const Intro = () => {
  const frame = useCurrentFrame();
  const draw = interpolate(frame, [8, 60], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const words = interpolate(frame, [55, 80], [0, 1], clamp);
  const tag = interpolate(frame, [80, 100], [0, 1], clamp);
  const waves = interpolate(frame, [0, 40], [0, 1], clamp);
  return (
    <AbsoluteFill style={{ opacity: useSceneFade(120, 8) }}>
      <div style={{ position: "absolute", top: 520, width: "100%", display: "flex", justifyContent: "center" }}>
        <Monogram size={380} draw={draw} />
      </div>
      <div style={{ position: "absolute", top: 940, width: "100%" }}>
        <Wordmark progress={words} />
        <Tagline progress={tag} />
      </div>
      <div style={{ position: "absolute", top: 1330, left: 0 }}>
        <Waves width={1080} height={260} reveal={waves} />
      </div>
    </AbsoluteFill>
  );
};

const messages = [
  { text: "Qui a la setlist de dimanche ?", right: false },
  { text: "Tu peux renvoyer l'audio ?", right: true },
  { text: "C'est dans quelle tonalité ?", right: false },
  { text: "Le lien ne marche plus…", right: true },
  { text: "On garde l'intro acoustique ?", right: false },
];

const Problem = () => {
  const frame = useCurrentFrame();
  const scatter = interpolate(frame, [62, 84], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  return (
    <AbsoluteFill style={{ opacity: useSceneFade(120) }}>
      <Sequence durationInFrames={70}>
        <Caption title="Fini la setlist perdue dans les messages" />
      </Sequence>
      {messages.map((message, index) => {
        const appear = spring({ frame: frame - index * 8, fps: 30, config: { damping: 14 } });
        const away = scatter * (index % 2 === 0 ? -1 : 1);
        return (
          <div
            key={index}
            style={{
              position: "absolute",
              top: 620 + index * 170,
              left: message.right ? undefined : 90,
              right: message.right ? 90 : undefined,
              maxWidth: 760,
              padding: "28px 38px",
              borderRadius: 40,
              fontSize: 40,
              color: C.snow,
              background: message.right ? "#2A2640" : C.surface,
              border: `2px solid ${C.outline}`,
              opacity: appear * (1 - scatter),
              transform: `translateX(${away * 600}px) scale(${0.85 + 0.15 * appear}) rotate(${away * 8}deg)`,
            }}
          >
            {message.text}
          </div>
        );
      })}
      <Sequence from={74}>
        <Caption title="La semaine du groupe, au même endroit" sub="Répétition mardi · culte dimanche" />
        <Phone>
          <Shot name="semaine" />
        </Phone>
      </Sequence>
    </AbsoluteFill>
  );
};

const Repeat = () => {
  const frame = useCurrentFrame();
  const shots = ["chant-nashville", "chant-solfege", "chant-lettres", "chant-transpose"];
  const labels = ["1 · 4 · 5", "Do · Ré · Mi", "C · D · E", "Mi majeur"];
  const step = 34;
  const current = Math.min(shots.length - 1, Math.floor(Math.max(0, frame - 18) / step));
  return (
    <AbsoluteFill style={{ opacity: useSceneFade(150) }}>
      <Caption kicker="RÉPÉTER" title="Paroles et accords, dans ta tonalité" />
      <Phone>
        {shots.map((name, index) => {
          const start = 18 + index * step;
          const opacity = index === 0 ? 1 : interpolate(frame, [start - 6, start + 4], [0, 1], clamp);
          return <Shot key={name} name={name} opacity={opacity} />;
        })}
      </Phone>
      <div
        style={{
          position: "absolute",
          top: 470,
          width: "100%",
          display: "flex",
          justifyContent: "center",
        }}
      >
        <div
          key={current}
          style={{
            padding: "14px 34px",
            borderRadius: 999,
            background: gradient,
            color: "white",
            fontSize: 34,
            fontWeight: 600,
          }}
        >
          {labels[current]}
        </div>
      </div>
    </AbsoluteFill>
  );
};

const Listen = () => {
  const frame = useCurrentFrame();
  const progress = interpolate(frame, [10, 150], [0.18, 0.62], clamp);
  const seconds = Math.round(212 * progress);
  const time = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
  return (
    <AbsoluteFill style={{ opacity: useSceneFade(150) }}>
      <Caption kicker="ÉCOUTER" title="La version de référence, même hors connexion" />
      <Phone>
        <Shot name="lecteur" />
        {/* The capture ended its demo tone: redraw the waves and a running progress bar. */}
        <Overlay x={0} y={1560} w={SHOT_W} h={260} style={{ background: C.night }}>
          <Waves width={PHONE_W} height={260 * SCALE} energy={1.25} />
        </Overlay>
        <Overlay x={60} y={1880} w={1086} h={200} style={{ background: C.night }}>
          <div style={{ position: "absolute", top: 18, left: 0, right: 0, height: 10, borderRadius: 5, background: "#221F32" }} />
          <div style={{ position: "absolute", top: 18, left: 0, width: `${progress * 100}%`, height: 10, borderRadius: 5, background: gradient }} />
          <div
            style={{
              position: "absolute",
              top: 8,
              left: `calc(${progress * 100}% - 15px)`,
              width: 30,
              height: 30,
              borderRadius: 15,
              background: C.violet,
            }}
          />
          <div style={{ position: "absolute", top: 50, left: 0, fontSize: 20, color: C.mist }}>{time(seconds)}</div>
          <div style={{ position: "absolute", top: 50, right: 0, fontSize: 20, color: C.mist }}>3:32</div>
        </Overlay>
        {/* It is playing: pause icon on the big button. */}
        <Overlay x={483} y={2108} w={240} h={240}>
          <div
            style={{
              width: "100%",
              height: "100%",
              borderRadius: "50%",
              background: `linear-gradient(135deg, ${C.purple}, ${C.indigo} 55%, ${C.blue})`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 9,
            }}
          >
            <div style={{ width: 10, height: 34, borderRadius: 3, background: "white" }} />
            <div style={{ width: 10, height: 34, borderRadius: 3, background: "white" }} />
          </div>
        </Overlay>
      </Phone>
    </AbsoluteFill>
  );
};

const Grow = () => {
  const frame = useCurrentFrame();
  const exercise = frame >= 48;
  const local = frame - 48;
  // The voice starts a little sharp and settles in tune.
  const needle = interpolate(local, [0, 30, 50, 70], [0.65, -0.3, 0.12, 0.03], clamp) + Math.sin(local / 3) * 0.02;
  const hold = interpolate(local, [50, 125], [0, 10], clamp);
  const inTune = local > 46;
  const done = hold >= 10;
  return (
    <AbsoluteFill style={{ opacity: useSceneFade(180) }}>
      <Caption kicker="GRANDIR" title={exercise ? "Des exercices de chant, validés au micro" : "Ta progression de chantre"} />
      <Phone>
        <Shot name="progression" />
        <div style={{ position: "absolute", inset: 0, opacity: interpolate(frame, [44, 54], [0, 1], clamp) }}>
          <Shot name="exercice" />
          <Overlay x={0} y={1330} w={SHOT_W} h={1292} style={{ background: C.night }}>
            <div style={{ padding: `${40 * SCALE}px ${52 * SCALE}px`, fontFamily, color: C.snow }}>
              <Tuner position={needle} />
              <div style={{ marginTop: 6, marginLeft: -26 }}>
                <Waves width={PHONE_W} height={70} energy={inTune ? 1 : 0.5} />
              </div>
              <div style={{ textAlign: "center", fontSize: 30, fontWeight: 600, color: inTune ? C.success : C.snow, marginTop: 6 }}>
                {done ? "Exercice validé !" : inTune ? "Juste ! Tiens la note" : "Un peu haut, descends légèrement"}
              </div>
              <div style={{ fontSize: 30, fontWeight: 600, marginTop: 22 }}>
                {hold.toFixed(1).replace(".", ",")} / 10 s
              </div>
              <div style={{ marginTop: 12, height: 10, borderRadius: 5, background: "#221F32" }}>
                <div style={{ width: `${hold * 10}%`, height: 10, borderRadius: 5, background: gradient }} />
              </div>
            </div>
          </Overlay>
        </div>
      </Phone>
    </AbsoluteFill>
  );
};

/** The app's tuner: ±100 cents, green band for in tune, white needle. */
const Tuner = ({ position }: { position: number }) => {
  const width = PHONE_W - 2 * 52 * SCALE;
  return (
    <svg width={width} height={70}>
      <rect x={width / 4} y={0} width={width / 2} height={70} rx={10} fill="rgba(111,211,160,0.35)" />
      {Array.from({ length: 17 }, (_, i) => {
        const x = (width * i) / 16;
        const major = i % 4 === 0;
        return <line key={i} x1={x} x2={x} y1={major ? 6 : 18} y2={major ? 64 : 52} stroke={C.outline} strokeWidth={2} />;
      })}
      <line x1={width / 2 + position * (width / 2 - 8)} x2={width / 2 + position * (width / 2 - 8)} y1={4} y2={66} stroke={C.snow} strokeWidth={6} strokeLinecap="round" />
    </svg>
  );
};

const Stage = () => (
  <AbsoluteFill style={{ opacity: useSceneFade(90) }}>
    <Caption kicker="EN SCÈNE" title="Tout l'écran pour les paroles" sub="Le chant suivant à portée de doigt" />
    <Phone>
      <Shot name="scene" />
    </Phone>
  </AbsoluteFill>
);

const Outro = () => {
  const frame = useCurrentFrame();
  const appear = interpolate(frame, [0, 20], [0, 1], clamp);
  const tag = interpolate(frame, [22, 40], [0, 1], clamp);
  return (
    <AbsoluteFill style={{ opacity: interpolate(frame, [0, 10], [0, 1], clamp) }}>
      <div style={{ position: "absolute", top: 560, width: "100%", display: "flex", justifyContent: "center", transform: `scale(${0.9 + 0.1 * appear})` }}>
        <Monogram size={340} />
      </div>
      <div style={{ position: "absolute", top: 940, width: "100%" }}>
        <Wordmark progress={appear} />
        <Tagline progress={tag} />
      </div>
      <div style={{ position: "absolute", top: 1300, left: 0 }}>
        <Waves width={1080} height={240} />
      </div>
    </AbsoluteFill>
  );
};

export const XmPromo = () => (
  <AbsoluteFill style={{ fontFamily, backgroundColor: C.night }}>
    <Background />
    <Sequence durationInFrames={120}>
      <Intro />
    </Sequence>
    <Sequence from={120} durationInFrames={120}>
      <Problem />
    </Sequence>
    <Sequence from={240} durationInFrames={150}>
      <Repeat />
    </Sequence>
    <Sequence from={390} durationInFrames={150}>
      <Listen />
    </Sequence>
    <Sequence from={540} durationInFrames={180}>
      <Grow />
    </Sequence>
    <Sequence from={720} durationInFrames={90}>
      <Stage />
    </Sequence>
    <Sequence from={810} durationInFrames={90}>
      <Outro />
    </Sequence>
  </AbsoluteFill>
);
