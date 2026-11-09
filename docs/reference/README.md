# Upstream reference

Blanket 0.8.0, commit `a961c1faeab69a61605b8733692b709ab5d2f09f`.
Source was cloned read-only to `/tmp/blanket-upstream`.

The screenshots are the images linked by https://apps.gnome.org/en/Blanket/:

- Light: https://static.gnome.org/catalog/app-screenshot/com.rafaelmardojai.Blanket/image-1_orig.png
- Dark: https://static.gnome.org/catalog/app-screenshot/com.rafaelmardojai.Blanket/image-2_orig.png

The source is GPL-3.0-or-later. Audio has the individual licenses in ../../SOUNDS_LICENSING.md. Icons are copied unchanged from upstream and credited to Jakub Steiner. Cantarell is bundled under SIL OFL 1.1; its full license is in the app assets.

Run `scripts/verify-assets.sh` from the repository root to verify the copied audio and symbolic icon bytes. Pass an APK path to also verify all packaged audio bytes. The hash manifests were captured immediately after copying from the upstream tag.

The original launcher SVG is copied as `blanket-app.svg`; its 192px PNG was rendered with the preinstalled librsvg and cairo libraries. The notification VectorDrawable preserves the original symbolic SVG paths and translations, omitting its negligible blur filter. Original SVGs remain available for attribution and reproduction.
