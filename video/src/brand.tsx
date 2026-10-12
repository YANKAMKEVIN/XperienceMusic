import { interpolate, useCurrentFrame } from "remotion";
import { loadFont } from "@remotion/google-fonts/Inter";

export const { fontFamily } = loadFont("normal", { weights: ["400", "600", "800", "900"], subsets: ["latin", "latin-ext"] });

export const C = {
  night: "#0B0A14",
  snow: "#F4F3FA",
  mist: "#A9A6BD",
  purple: "#B46CFF",
  indigo: "#6D5BFF",
  blue: "#3D7BFF",
  violet: "#9B7BFF",
  success: "#6FD3A0",
  surface: "#15131F",
  outline: "#2E2A44",
};

export const gradient = `linear-gradient(90deg, ${C.purple}, ${C.indigo}, ${C.blue})`;

/** Night background with the soft violet glow of the app's welcome screens. */
export const Background = () => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      background: `radial-gradient(ellipse 90% 45% at 50% 22%, rgba(109,91,255,0.28), rgba(11,10,20,0) 70%), ${C.night}`,
    }}
  />
);

/**
 * The XM monogram, same geometry as the app icon (design/make_icon.swift), y pointing down.
 * [draw] 0..1 traces the strokes, then the note fills in.
 */
export const Monogram = ({ size, draw = 1 }: { size: number; draw?: number }) => {
  const lines = interpolate(draw, [0, 0.6], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const note = interpolate(draw, [0.5, 1], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const dash = (length: number) => ({ strokeDasharray: length, strokeDashoffset: length * (1 - lines) });
  return (
    <svg width={size} height={size} viewBox="0 0 1000 1000">
      <defs>
        <linearGradient id="xm" gradientUnits="userSpaceOnUse" x1="480" y1="180" x2="900" y2="760">
          <stop offset="0" stopColor={C.purple} />
          <stop offset="0.5" stopColor={C.indigo} />
          <stop offset="1" stopColor={C.blue} />
        </linearGradient>
        <clipPath id="flatTop">
          <rect x="0" y="267" width="1000" height="733" />
        </clipPath>
      </defs>
      <g transform="translate(-42 20)">
        <path d="M150 700 L420 300" stroke={C.snow} strokeWidth="96" style={dash(482)} />
        <path d="M150 300 L420 700" stroke={C.snow} strokeWidth="96" style={dash(482)} />
        <g clipPath="url(#flatTop)">
          <path
            d="M505 700 L505 300 L640 500 L775 300"
            fill="none"
            stroke="url(#xm)"
            strokeWidth="92"
            strokeMiterlimit="10"
            style={dash(1050)}
          />
        </g>
        <g opacity={note} transform={`translate(0 ${(1 - note) * 40})`}>
          <rect x="729" y="200" width="92" height="470" fill="url(#xm)" />
          <path d="M805 215 C845 270 935 310 885 400" fill="none" stroke="url(#xm)" strokeWidth="56" strokeLinecap="round" />
          <ellipse cx="708" cy="678" rx="100" ry="70" fill="url(#xm)" transform="rotate(-24 708 678)" />
        </g>
      </g>
    </svg>
  );
};

const waveLines = [
  { offset: 0, amplitude: 0.22, frequency: 1.6, speed: 1, width: 3.2, alpha: 0.95 },
  { offset: 0.02, amplitude: 0.16, frequency: 2.1, speed: -1, width: 2, alpha: 0.6 },
  { offset: -0.03, amplitude: 0.28, frequency: 1.2, speed: 2, width: 1.5, alpha: 0.4 },
  { offset: 0.05, amplitude: 0.1, frequency: 3, speed: -2, width: 1.2, alpha: 0.3 },
];

/** The luminous sound waves of the app (BrandWaves), drifting with the frame. [energy] scales them. */
export const Waves = ({ width, height, energy = 1, reveal = 1 }: { width: number; height: number; energy?: number; reveal?: number }) => {
  const frame = useCurrentFrame();
  const phase = (frame / 270) * Math.PI * 2;
  const id = `w${width}${height}`;
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
      <defs>
        <linearGradient id={`${id}g`} x1="0" x2="1">
          <stop offset="0" stopColor={C.purple} />
          <stop offset="0.5" stopColor={C.indigo} />
          <stop offset="1" stopColor={C.blue} />
        </linearGradient>
        <linearGradient id={`${id}f`} x1="0" x2="1">
          <stop offset="0" stopColor="white" stopOpacity="0" />
          <stop offset="0.2" stopColor="white" />
          <stop offset="0.8" stopColor="white" />
          <stop offset="1" stopColor="white" stopOpacity="0" />
        </linearGradient>
        <mask id={`${id}m`}>
          <rect width={width * reveal} height={height} fill={`url(#${id}f)`} />
        </mask>
      </defs>
      <g mask={`url(#${id}m)`}>
        {waveLines.map((line, index) => {
          const points: string[] = [];
          for (let step = 0; step <= 120; step++) {
            const t = step / 120;
            const envelope = Math.sin(Math.PI * t);
            const y =
              height / 2 +
              line.offset * height +
              Math.sin(t * line.frequency * 2 * Math.PI + phase * line.speed + index) *
                line.amplitude * energy * height * (0.35 + 0.65 * envelope);
            points.push(`${step === 0 ? "M" : "L"}${(t * width).toFixed(1)} ${y.toFixed(1)}`);
          }
          return (
            <path key={index} d={points.join(" ")} fill="none" stroke={`url(#${id}g)`} strokeWidth={line.width} opacity={line.alpha} />
          );
        })}
      </g>
    </svg>
  );
};
