# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/review/service/

## Domain: general

## Components
* `PeerReviewService.java`: package eu.kalafatic.evolution.controller.review.service; import java.io.File; import java.util.UUID; import eu.kalafatic.evolution.model.orchestration.ReviewSession; import eu.kalafatic.evolution.model.orchestration.ReviewDecision; import eu.kalafatic.evolution.model.orchestration.ChangeSet; import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory; import eu.kalafatic.evolution.controller.vcs.VersionControlProvider; import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider; public class PeerReviewService { private static PeerReviewService instance; private VersionControlProvider vcsProvider; private ReviewSession activeSession; private PeerReviewService() { this.vcsProvider = new GitVersionControlProvider(); } public static synchronized PeerReviewService getInstance() { if (instance == null) { instance = new PeerReviewService(); }
