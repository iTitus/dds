# dds / ddsiio / ddsfx

[![GitHub License](https://img.shields.io/github/license/iTitus/commons)](https://github.com/iTitus/commons/blob/main/LICENSE)
[![Repo Size](https://img.shields.io/github/repo-size/iTitus/commons.svg)](https://github.com/iTitus/commons)
[![Maven Central - dds](https://img.shields.io/maven-central/v/io.github.ititus/dds?label=Maven%20Central%20-%20dds)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22dds%22)
[![Maven Central - ddsiio](https://img.shields.io/maven-central/v/io.github.ititus/ddsiio?label=Maven%20Central%20-%20dds)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22ddsiio)
[![Maven Central - ddsfx](https://img.shields.io/maven-central/v/io.github.ititus/ddsfx?label=Maven%20Central%20-%20ddsfx)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22ddsfx%22)
[![Gradle Build](https://github.com/iTitus/commons/workflows/Gradle%20Build/badge.svg)](https://github.com/iTitus/commons/actions?query=workflow%3A%22Gradle+Build%22)

Read `dds` images (DirectDrawSurface), an image format made by Microsoft for DirectX.

Currently targeting Java 21+ and JavaFX 21+.

## Supported formats

- All UINT/UNORM/SRGB RGB(A) formats
- Block compressed formats
  - BC1/DXT1
  - BC2/DXT2/DXT3
  - BC3/DXT4/DXT5
  - BC7

## Targets:

- `DdsFile` (`dds` module, basic routines to load a dds file into memory)
- Java `BufferedImage` (`ddsiio` module, this is the one you want)
- JavaFX `Image` (`ddsfx` module, obsolete starting with JavaFX 24)


## dds

- contains the reading logic and api

## ddsiio

- adds dds support to `ImageIO` automatically
- this requires the `java.desktop` module
- also adds dds support for JavaFX `Image` starting with JavaFX 24

## ddsfx

> [!IMPORTANT]  
> This module is no longer required as of JavaFX 24, one can use `ddsiio` instead!
> JavaFX will plug into `ImageIO` and use those providers automatically.

- adds dds support for JavaFX `Image`
- one needs to call `io.github.ititus.ddsfx.DdsFx.setup()` once to register the format
- this requires the `java.desktop` module (via `ddsiio`) and JavaFX
- JavaFX needs to be included separately, there is no transitive dependency
- if you have problems with duplicate JavaFX dependencies see https://github.com/openjfx/javafx-gradle-plugin/issues/65
- when including this in a modular build you will need to add the following compile and run options:
    - `--add-export javafx.graphics/com.sun.javafx.iio=io.github.ititus.ddsfx`
    - `--add-export javafx.graphics/com.sun.javafx.iio.common=io.github.ititus.ddsfx`
