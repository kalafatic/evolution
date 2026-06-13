# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.media/src/eu/kalafatic/evolution/media/

## Domain: general

## Components
* `MediaService.java`: package eu.kalafatic.evolution.media; import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel; import eu.kalafatic.evolution.controller.orchestration.design.DesignModel; import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord; import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord; import eu.kalafatic.evolution.media.generator.*; import eu.kalafatic.evolution.media.model.*; import eu.kalafatic.evolution.media.render.*; import eu.kalafatic.evolution.media.video.*; import java.io.File; import java.io.IOException; import java.nio.file.Files; import java.nio.charset.StandardCharsets; import java.util.List; import java.util.Arrays; public class MediaService { private final HtmlRenderer htmlRenderer = new HtmlRenderer(); private final SvgRenderer svgRenderer = new SvgRenderer(); private final PresentationGenerator presentationGenerator = new PresentationGenerator(); private final StoryboardGenerator storyboardGenerator = new StoryboardGenerator();
