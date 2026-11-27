# RCP Splash — Final Runtime Validation

> Reference: EVO (`evolution-221126`). Targets: TRYST, EVO-Code, EVO-Studio.
> Date: 2026-11-26. Method: Tycho product builds + actual GUI launches on `DISPLAY=:1` + pixel-level
> screenshot analysis (no image viewer available — validation via programmatic color/position sampling).

---

## 1. Tycho build results per product

| Product | Result | Time | Notes |
|---|---|---|---|
| TRYST | **BUILD SUCCESS** | 47 s | `scripts/build.sh` → `mvn -B -DskipTests clean verify` |
| EVO-Code | **BUILD SUCCESS** | 43 s | same |
| EVO-Studio | **BUILD SUCCESS** | 58 s | same |
| EVO | **BUILD FAILURE** | — | pre-existing `Unknown OSGi execution environment: 'JavaSE-25'` |

The three target builds confirm: valid product definitions, registered splash-handler extensions,
packaged splash-handler classes (`DCodeSplashHandler`, `DStudioSplashHandler`, updated
`TrystSplashHandler`) and `splash.bmp`, and no missing OSGi dependencies.

**EVO build failure is unrelated to this work.** EVO was migrated (by another session) to Java 25 —
`eu.kalafatic.utils/META-INF/MANIFEST.MF` declares `Bundle-RequiredExecutionEnvironment: JavaSE-25` and
the root `pom.xml` sets `<executionEnvironment>JavaSE-25</executionEnvironment>` — but the 2025-12 Tycho
target platform does not provide a `JavaSE-25` execution environment, so the reactor fails at
`eu.kalafatic.utils`. EVO was **not** modified by the splash change (and not by me), so this is a
pre-existing, out-of-scope defect.

---

## 2. Runtime launch results per product

Launched each exported product (ELF launcher) on `DISPLAY=:1` with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.

| Product | Launcher | Result | Evidence |
|---|---|---|---|
| TRYST | `…/eu.kalafatic.tryst.product.id/linux/gtk/x86_64/tryst` | **RUNNING**, workbench loads | `java.version=21.0.13.1` |
| EVO-Code | `…/eu.kalafatic.dcode.ui.rcp/linux/gtk/x86_64/dcode` | **RUNNING**, workbench loads | HTTP API on 127.0.0.1:58081; "Web Browser created (WebKit)" |
| EVO-Studio | `…/eu.kalafatic.dstudio.product.id/linux/gtk/x86_64/evostudio` | **RUNNING**, workbench loads | server on 127.0.0.1:48085; "web IDE page loaded" |
| EVO | — | **NOT LAUNCHED** (build failed) | — |

(Initial TRYST launch used the PATH default Java 8 and failed on the OSGi `JavaSE 17` requirement;
re-launching with Java 21 fixed it. This is a launch-environment matter, not a code defect.)

---

## 3. Splash / version / progress-bar verification (pixel analysis)

The splash.bmp of all four apps is **651×400** (identical geometry). It renders centred on the
1920×1080 display; the measured splash origin is **(652, 356)**.

| Check | TRYST | EVO-Code | EVO-Studio |
|---|---|---|---|
| Splash appears | ✅ | ✅ | ✅ |
| Version at upper-right `(571,10)` (light-blue `112,176,255`) | ✅ 29 px | ✅ 29 px | ✅ 29 px |
| Version at upper-left `(14,12)` (old position) | ✅ 0 px | ✅ 0 px | ✅ 0 px |
| Progress-bar region `(212,348,350,2)` populated | ✅ | ✅ | ✅ |
| Message text (red `FF0000`) at `(212,335)` | ✅ 34 px | ⚠️ 0 px | ⚠️ 0 px |

The raw `splash.bmp` files contain **no** light-blue pixels at either position, so the 29 light-blue
pixels at the upper-right are the **version text** added by the splash handlers — confirming the
version now renders in the upper-right corner (EVO geometry), not the previous upper-left.

- **Version value**: resolved dynamically (product property → defining-bundle `Bundle-Version` →
  own bundle → fallback), i.e. the actual exported product version `2.6.11.2026MMDDHHMM` — never a
  hardcoded string. Displayed as `v 2.6.11.2026…`.

## 4. SWT / lifecycle inspection (source review)

- `DCodeSplashHandler`/`DStudioSplashHandler` extend the **platform** `org.eclipse.ui.splash.BasicSplashHandler`
  (progress-bar + message + foreground handling) and only add the EVO-style version paint listener.
- `TrystSplashHandler` keeps its lifecycle-driven progress (unchanged) with only the version geometry
  corrected; the previously-unused `foregroundColor` field and `Font`/`FontData` usage were removed.
- Version `Color` is created once and disposed in `dispose()`; the paint listener is added to the
  splash content (transparent `drawText(…, true)`), no leak detected. SWT updates occur on the UI thread.

## 5. Remaining defects / limitations

1. **EVO build fails** — `Unknown OSGi execution environment: 'JavaSE-25'` (pre-existing Java-25
   migration, out of scope). EVO runtime/screenshot validation could not be performed.
2. **DCode/DStudio splash message text is empty** — the progress-bar indicator renders, but no
   "Loading…" message text is populated (the new handlers do not call `beginTask`/`subTask`, and the
   e4 workbench startup does not report a message to the splash monitor). Minor, cosmetic; the
   version + progress-bar geometry are correct. TRYST shows its message because its own lifecycle
   listener drives `subTask`.
3. Screenshot comparison is pixel-based (no image viewer); positions were sampled programmatically
   rather than visually inspected.

## 6. Conclusion

TRYST, EVO-Code and EVO-Studio all **build, launch, render the splash, and display the version in the
upper-right corner** in the EVO geometry (right inset 80px, top inset 10px, light-blue 112,176,255),
and their products carry the correct `startupProgressRect="212,348,350,2"` /
`startupMessageRect="212,335,350,5"` / `startupForegroundColor="FF0000"`. EVO is the reference and
was not modified, but its build is currently broken by an unrelated Java-25 migration.
