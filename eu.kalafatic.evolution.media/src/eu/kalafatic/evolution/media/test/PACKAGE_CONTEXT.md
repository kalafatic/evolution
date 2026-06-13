# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.media/src/eu/kalafatic/evolution/media/test/

## Domain: general

## Components
* `MediaServiceManualTest.java`: package eu.kalafatic.evolution.media.test; import eu.kalafatic.evolution.media.MediaService; import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel; import eu.kalafatic.evolution.controller.orchestration.design.DesignModel; import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord; import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord; import java.io.File; import java.io.IOException; public class MediaServiceManualTest { public static void main(String[] args) throws IOException { MediaService mediaService = new MediaService(); DesignModel designModel = new DesignModel(); designModel.setName("Test System"); ComponentRecord c1 = new ComponentRecord(); c1.setId("c1"); c1.setName("Core"); designModel.getComponents().add(c1); ComponentRecord c2 = new ComponentRecord(); c2.setId("c2"); c2.setName("UI \"Widget\"");
