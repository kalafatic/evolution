# RCP Launching UI — Progress Bar Position & Version Alignment (EVO / TRYST / EVO-Code / EVO-Studio)

> Reference: EVO (`evolution-221126`). Targets: TRYST (`tryst-221126`), EVO-Code (`evo-code-221126`), EVO-Studio (`evo-studio-221126`).
> Goal: reproduce EVO's startup splash geometry (progress bar + upper-right version) exactly.

---

## 1. EVO reference implementation (discovered)

**Splash assets:** `splash.bmp` — 651×400, 24-bit BMP (shipped in the defining bundle).

**Product splash properties** (`*.product` → `<splash>` element):

| Property | Value |
|---|---|
| `startupProgressRect` | `212,348,350,2` |
| `startupMessageRect` | `212,335,350,5` |
| `startupForegroundColor` | `FF0000` |

**Splash handler chain:**
- `eu.kalafatic.utils.hack.BasicSplashHandler` — `getBundleProgressMonitor()` builds a JFace
  `ProgressMonitorPart` (absolute-positioned) and applies `progressRect`/`messageRect`/`foreground`.
- `eu.kalafatic.utils.hack.EclipseSplashHandler` — `init()` reads the product properties
  (`IProductConstants.STARTUP_PROGRESS_RECT` / `STARTUP_MESSAGE_RECT` / `STARTUP_FOREGROUND_COLOR`)
  and `drawVersion(...)`.
- `...controller.splashHandlers.EvolutionSplashHandler` — sets the `splash.bmp` background.

**Progress bar** (exact): rectangle `(x=212, y=348, width=350, height=2)`, foreground `#FF0000`,
message text at `(212,335,350,5)`.

**Version label** (exact): painted by `EclipseSplashHandler.drawVersion` —
- position `x = splashBounds.width − 80`, `y = 10` (upper-right, right inset 80px, top inset 10px);
- foreground light-blue `RGB(112,176,255)`;
- default font (no `setFont`);
- text `"v " + version` with a trailing `.qualifier` stripped, transparent background (`drawText(..., true)`).

For the 651px splash the version origin is `(571, 10)`.

**Version resolution** (`EclipseSplashHandler.resolveVersion`): product `"version"` property →
product defining bundle `Bundle-Version` → own framework bundle version → `eu.kalafatic.evolution.view`
bundle version → fallback `2.6.11.qualifier`. The `.qualifier` suffix is stripped before display.

---

## 2. Exact geometry summary

- Progress bar: `(212, 348, 350, 2)`, red `FF0000`.
- Message: `(212, 335, 350, 5)`.
- Version: `(width − 80, 10)`, light-blue `(112,176,255)`, default font, `"v 2.6.11"`.

---

## 3. Original differences per application

| App | Progress rect | Version label | Notes |
|---|---|---|---|
| TRYST | `.product` OK; **plugin.xml product ext WRONG** `40,240,420,15` / `40,220,420,20` / `94A3B8` | upper-LEFT `(14,12)`, gray `94A3B8`, font 8 bold | two conflicting product declarations |
| EVO-Code | **WRONG** `48,352,555,4` / `48,332,555,8` / `8FA5FF` | **absent** (no splash handler) | |
| EVO-Studio | OK `212,348,350,2` | **absent** (no splash handler) | |

---

## 4. Files modified

### TRYST
- `plugins/eu.kalafatic.tryst.application/plugin.xml` — product extension rects → `212,348,350,2` / `212,335,350,5` / `FF0000`.
- `.../application/splash/TrystSplashHandler.java` — progress/message rect defaults → EVO values; version
  rendering → upper-right `(width−80,10)` light-blue default-font; removed the now-unused `foregroundColor`
  field and `Font`/`FontData` usage.

### EVO-Code
- `product/eu.kalafatic.dcode.product/dcode.product` — `<splash>` rects → `212,348,350,2` / `212,335,350,5` / `FF0000`.
- `plugins/eu.kalafatic.dcode.ui/src/.../splash/DCodeSplashHandler.java` — **new** splash handler (extends
  platform `org.eclipse.ui.splash.BasicSplashHandler`): product-rect configuration + upper-right version.
- `plugins/eu.kalafatic.dcode.ui/plugin.xml` — `org.eclipse.ui.splashHandlers` extension + product binding (`eu.kalafatic.dcode.ui.rcp`).

### EVO-Studio
- `plugins/eu.kalafatic.dstudio.application/src/.../splash/DStudioSplashHandler.java` — **new** splash handler (same as above).
- `plugins/eu.kalafatic.dstudio.application/plugin.xml` — `org.eclipse.ui.splashHandlers` extension + product binding (`eu.kalafatic.dstudio.application.product`).

---

## 5. Implementation approach

- Reused the **platform** `org.eclipse.ui.splash.BasicSplashHandler` (present in `org.eclipse.ui.workbench`,
  already required by TRYST/EVO-Code/EVO-Studio) for the progress bar + message + foreground; it reads the
  product `startupProgressRect`/`startupMessageRect`/`startupForegroundColor`. No copy of EVO's
  `eu.kalafatic.utils.hack.*` classes was introduced into the other products (they do not depend on that bundle).
- Added only the EVO-specific **version paint listener** (upper-right, light-blue, default font) in each target.
- Version is resolved dynamically from product/bundle metadata (never a hardcoded product constant); `.qualifier`
  is stripped for display.

---

## 6. Product version resolution

Each handler resolves the version in the same chain as EVO:
`Platform.getProduct().getProperty("version")` → product defining bundle `Bundle-Version` → own bundle
`Bundle-Version` → fallback `2.6.11`; trailing `.qualifier` stripped. This yields the actual exported product
version (all four products are `2.6.11.qualifier`).

---

## 7. Build & validation results

- **Compilation:** `TrystSplashHandler.java`, `DCodeSplashHandler.java`, `DStudioSplashHandler.java` all compile
  cleanly (`javac --release 21`, RC=0) against each repo's compiled classes + Eclipse target-platform bundles.
- **Geometry audit:** grep-confirmed all four products now declare `212,348,350,2` / `212,335,350,5` / `FF0000`,
  and all four version renderers use `width − 80`, `RGB(112,176,255)` and `drawText("v …", x, y, true)`.
- **Runtime / screenshot:** not executed — this environment has no GUI/display and the four applications are not
  launched here; SWT runtime + full Tycho product export remain to be validated in an interactive environment.

---

## 8. Remaining issues / limitations

1. Runtime splash rendering and product export were not executed (headless environment); the changes are
   validated at source/geometry level only.
2. DPI-scaling behaviour was not exercised interactively; both EVO and the targets use the same absolute pixel
   rects and the same relative `width − 80` inset, so scaling follows the shared Eclipse mechanism identically.
3. `eu.kalafatic.utils.hack.BasicSplashHandler`/`EclipseSplashHandler` remain a historical duplicate of the
   platform `org.eclipse.ui.splash.BasicSplashHandler`; left unchanged (EVO is the reference, and the targets now
   use the platform class directly).
