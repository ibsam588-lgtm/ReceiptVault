import { createRequire } from "node:module";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const require = createRequire(import.meta.url);
const sharp = require("../cloudflare/receiptvault-backup/node_modules/sharp");

const outDir = path.resolve("playstore/graphics");
await mkdir(outDir, { recursive: true });

const iconSvg = `
<svg width="512" height="512" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
  <rect width="512" height="512" rx="96" fill="#123c3d"/>
  <circle cx="396" cy="92" r="74" fill="#f5bf4f"/>
  <path d="M142 92h224c22 0 40 18 40 40v300l-34-22-34 22-34-22-34 22-34-22-34 22-34-22-34 22V132c0-22 18-40 40-40z" fill="#fff8e8"/>
  <path d="M178 172h158M178 224h124M178 276h158" stroke="#123c3d" stroke-width="24" stroke-linecap="round"/>
  <path d="M306 330l28 28 62-72" fill="none" stroke="#2f9c78" stroke-width="28" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`;

const featureSvg = `
<svg width="1024" height="500" viewBox="0 0 1024 500" xmlns="http://www.w3.org/2000/svg">
  <rect width="1024" height="500" fill="#f8f4ea"/>
  <rect x="0" y="0" width="1024" height="138" fill="#123c3d"/>
  <circle cx="894" cy="92" r="98" fill="#f5bf4f"/>
  <circle cx="92" cy="418" r="112" fill="#2f9c78" opacity="0.24"/>
  <text x="84" y="96" font-family="Arial, Helvetica, sans-serif" font-size="56" font-weight="800" fill="#ffffff">ReceiptVault</text>
  <text x="84" y="202" font-family="Arial, Helvetica, sans-serif" font-size="44" font-weight="800" fill="#123c3d">Save receipts. Find proof fast.</text>
  <text x="84" y="258" font-family="Arial, Helvetica, sans-serif" font-size="28" fill="#36595a">OCR, expense tracking, returns, and warranty records</text>
  <g transform="translate(646 148)">
    <rect x="0" y="0" width="238" height="318" rx="34" fill="#123c3d"/>
    <rect x="24" y="30" width="190" height="258" rx="24" fill="#fff8e8"/>
    <rect x="48" y="70" width="116" height="18" rx="9" fill="#123c3d"/>
    <rect x="48" y="118" width="144" height="14" rx="7" fill="#8aa1a0"/>
    <rect x="48" y="154" width="116" height="14" rx="7" fill="#8aa1a0"/>
    <rect x="48" y="190" width="144" height="14" rx="7" fill="#8aa1a0"/>
    <path d="M76 244l30 30 70-82" fill="none" stroke="#2f9c78" stroke-width="18" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
  <g transform="translate(84 314)" font-family="Arial, Helvetica, sans-serif" font-size="24" font-weight="700">
    <rect x="0" y="0" width="174" height="58" rx="29" fill="#ffffff"/>
    <text x="28" y="38" fill="#123c3d">OCR scan</text>
    <rect x="194" y="0" width="202" height="58" rx="29" fill="#ffffff"/>
    <text x="222" y="38" fill="#123c3d">Warranty log</text>
    <rect x="416" y="0" width="174" height="58" rx="29" fill="#ffffff"/>
    <text x="444" y="38" fill="#123c3d">Local vault</text>
  </g>
</svg>`;

await sharp(Buffer.from(iconSvg)).png().toFile(path.join(outDir, "app-icon-512.png"));
await sharp(Buffer.from(featureSvg)).png().toFile(path.join(outDir, "feature-graphic-1024x500.png"));
await writeFile(path.join(outDir, "README.md"), [
  "# Play Store Graphics",
  "",
  "- `app-icon-512.png`: required 512 x 512 Play Store icon.",
  "- `feature-graphic-1024x500.png`: required Play Store feature graphic.",
  "- Phone screenshots are in `../screenshots/phone/`.",
  ""
].join("\n"));
