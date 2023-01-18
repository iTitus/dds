# dds / ddsiio / ddsfx

[![GitHub License](https://img.shields.io/github/license/iTitus/commons)](https://github.com/iTitus/commons/blob/main/LICENSE)
[![Repo Size](https://img.shields.io/github/repo-size/iTitus/commons.svg)](https://github.com/iTitus/commons)
[![Maven Central - dds](https://img.shields.io/maven-central/v/io.github.ititus/dds?label=Maven%20Central%20-%20dds)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22dds%22)
[![Maven Central - ddsiio](https://img.shields.io/maven-central/v/io.github.ititus/ddsiio?label=Maven%20Central%20-%20dds)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22ddsiio)
[![Maven Central - ddsfx](https://img.shields.io/maven-central/v/io.github.ititus/ddsfx?label=Maven%20Central%20-%20ddsfx)](https://search.maven.org/search?q=g:%22io.github.ititus%22%20AND%20a:%22ddsfx%22)
[![Gradle Build](https://github.com/iTitus/commons/workflows/Gradle%20Build/badge.svg)](https://github.com/iTitus/commons/actions?query=workflow%3A%22Gradle+Build%22)

Read `dds` images (DirectDrawSurface), an image format made by Microsoft for DirectX.

- Targets: Java `BufferedImage` (`dds` module) and JavaFX `Image` (`ddsfx` module)

## dds

- contains the reading logic and api

## ddsiio

- adds dds support to `ImageIO` automatically

## ddsfx

- adds dds support for JavaFX `Image`
- one needs to call `io.github.ititus.ddsfx.DdsFx.init()` once to register the format
- this requires JavaFX in the module path, but does not add a transitive dependency for it due to classifier problems (see https://github.com/openjfx/javafx-gradle-plugin/issues/65)

When including this in a modular build you will need to add the following compile and run options:
- `--add-export javafx.graphics/com.sun.javafx.iio=io.github.ititus.ddsfx`
- `--add-export javafx.graphics/com.sun.javafx.iio.common=io.github.ititus.ddsfx`
