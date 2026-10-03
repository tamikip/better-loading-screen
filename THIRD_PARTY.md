# Third-party assets

Apache-2.0 covers this project's original code, configuration, documentation,
and project-created artwork. It does not grant rights to third-party fonts,
audio, trademarks, or any other third-party material.

The following files are present in the author's local development copy but
are excluded from the public Git repository and its source archives:

| Asset | Local paths | Source and status |
| --- | --- | --- |
| HYWenHei-85W font | `src/main/resources/assets/teleport_loading/font/wenhei.ttf`, example `font/ui.ttf` | Converted from the WOFF2 hosted at https://github.com/Zamburg1/Genshin-Font-Host. A redistribution license has not been verified. Hosting alone is not a license grant. |
| Loading audio | `src/main/resources/assets/teleport_loading/sounds/loading.ogg`, example `sounds/arrival.ogg` | Converted from the user-supplied recording. Ownership and redistribution permission have not been verified. |

A clean public-source Gradle build uses Minecraft's default font and an empty
custom sound event when these optional files are absent. Teleport detection,
loading visuals, and resource/data pack customization remain available.
Supply your own properly licensed font and OGG Vorbis audio through a resource
pack, following `examples/README.md`.

Previously built local JARs may contain these third-party assets. The source
license change does not relicense those assets or establish permission to
redistribute them.
