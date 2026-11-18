package eu.kalafatic.evolution.controller.contextual;

/**
 * Canonical contextual action ids — Darwin Family Contextual Action Contract v1.0
 * (owned by DStudio; EVO implements the harness/provider/session/repository subset
 * locally). EVO owns Darwin orchestration and its existing session/iteration
 * lifecycle.
 */
public final class ContextualActionIds {

    public static final String CONTRACT_VERSION = "1.0";

    // harness
    public static final String HARNESS_SELECT = "eu.kalafatic.contextual.harness.select";
    public static final String HARNESS_SET_DEFAULT = "eu.kalafatic.contextual.harness.set-default";
    public static final String HARNESS_TEST = "eu.kalafatic.contextual.harness.test";
    public static final String HARNESS_ENABLE = "eu.kalafatic.contextual.harness.enable";
    public static final String HARNESS_DISABLE = "eu.kalafatic.contextual.harness.disable";
    public static final String HARNESS_REMOVE = "eu.kalafatic.contextual.harness.remove";
    public static final String HARNESS_VIEW_CAPABILITIES = "eu.kalafatic.contextual.harness.view-capabilities";

    // provider
    public static final String PROVIDER_SELECT = "eu.kalafatic.contextual.provider.select";
    public static final String PROVIDER_TEST = "eu.kalafatic.contextual.provider.test";
    public static final String PROVIDER_VIEW_ENDPOINT = "eu.kalafatic.contextual.provider.view-endpoint";

    // session / task / execution
    public static final String SESSION_SELECT = "eu.kalafatic.contextual.session.select";
    public static final String SESSION_RESUME = "eu.kalafatic.contextual.session.resume";
    public static final String SESSION_PAUSE = "eu.kalafatic.contextual.session.pause";
    public static final String SESSION_CANCEL = "eu.kalafatic.contextual.session.cancel";
    public static final String SESSION_DELETE = "eu.kalafatic.contextual.session.delete";
    public static final String SESSION_HISTORY = "eu.kalafatic.contextual.session.history";
    public static final String SESSION_DETAILS = "eu.kalafatic.contextual.session.details";
    public static final String SESSION_DUPLICATE = "eu.kalafatic.contextual.session.duplicate";
    public static final String SESSION_ARCHIVE = "eu.kalafatic.contextual.session.archive";

    // project / repository
    public static final String PROJECT_OPEN = "eu.kalafatic.contextual.project.open";
    public static final String PROJECT_SELECT = "eu.kalafatic.contextual.project.select";
    public static final String PROJECT_CLOSE = "eu.kalafatic.contextual.project.close";
    public static final String PROJECT_REFRESH = "eu.kalafatic.contextual.project.refresh";
    public static final String PROJECT_GIT_STATUS = "eu.kalafatic.contextual.project.git-status";
    public static final String PROJECT_COMMIT = "eu.kalafatic.contextual.project.commit";
    public static final String PROJECT_PUSH = "eu.kalafatic.contextual.project.push";
    public static final String PROJECT_DIAGNOSTICS = "eu.kalafatic.contextual.project.diagnostics";
    public static final String PROJECT_CREATE_SESSION = "eu.kalafatic.contextual.project.create-session";

    private ContextualActionIds() {
    }
}
