# Amazon Reseam patches

Amazon Shopping patches ported from [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches/tree/main/patches/src/main/kotlin/app/template/patches/amazon) to the Reseam patch API.

## Included patches

- **Remove ads** — hides a focused set of sponsored cards and promotional slots with injected CSS.
- **Hide Rufus tab** — forces the Rufus bottom-navigation gate off.
- **Dark mode** — applies dark CSS when Android system dark mode is active and tints tab icons.
- **Disable video autoplay** — requires user interaction before WebView media playback.
- **Disable search suggestions tracking** — drops the suggestion builder's event field.
- **Open links in browser** — routes non-Amazon HTTP(S) links to the external browser; malformed and non-web URLs fail closed.
- **Price history charts** — adds a product-page section and loads Keepa/CamelCamelCamel charts only after the user taps the button.
- **Fix Amazon manifest conflicts** — namespaces declared permissions and updates references.

Both `com.amazon.mShop.android.shopping` and `in.amazon.mShop.android.shopping` are declared as compatible package names.

## Build

The repository uses the Reseam workspace plugin version pinned in `settings.gradle.kts`.

```sh
./gradlew bundle
```

The resulting bundle should be written under `build/reseam/`. The CLI version and a signing key must match the Reseam workspace requirements; see the [Reseam authoring docs](https://reseam.app/docs/authoring/start/).

## Porting notes

This is a source-level port; it has **not been validated against an Amazon APK**. The target method descriptions are based on the supplied Morphe source, so exact resolution still depends on the Amazon build being patched. No APK was needed for this source port.

Some options and implementation details from the Morphe version are simplified:
- Dark mode follows Android's system setting rather than exposing Off / Follow system / Always on options.
- The ad-block selector set is a smaller curated subset.
- Price charts use a fixed one-year view and don't expose the Morphe chart options.
- Price-history provider requests happen only after the user explicitly taps Load.

## Attribution and licensing

The patch behavior was adapted from [Morphe Amazon patches](https://github.com/rushiranpise/morphe-patches/tree/main/patches/src/main/kotlin/app/template/patches/amazon). The ad selectors and dark-mode CSS are inspired by and partially derived from [amznkiller](https://github.com/hxreborn/amznkiller) by hxreborn (GPL-3.0). The upstream Morphe README contains further attribution details.

These adapted Amazon patch files are licensed under GPL-3.0-only, consistent with the upstream-derived code. The rest of this repository retains its existing license and notices.
