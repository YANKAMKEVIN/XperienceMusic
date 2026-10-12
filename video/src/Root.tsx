import { Composition } from "remotion";
import { XmPromo } from "./XmPromo";

export const FPS = 30;

export const Root = () => (
  <Composition id="XmPromo" component={XmPromo} durationInFrames={30 * FPS} fps={FPS} width={1080} height={1920} />
);
