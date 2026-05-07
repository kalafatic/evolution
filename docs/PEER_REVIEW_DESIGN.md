# Peer Review Editor Design

## 1. System Architecture Diagram

```text
+----------------------------------------------------------------------------------+
|                                     UI Layer                                     |
|  +------------------+       +-------------------------+       +---------------+  |
|  |  FileTreeGroup   | <---> |    DiffViewerGroup      | <---> | CommentsGroup |  |
|  +------------------+       +-------------------------+       +---------------+  |
|           ^                            ^                             ^           |
+-----------|----------------------------|-----------------------------|-----------+
            |                            |                             |
+-----------v----------------------------v-----------------------------v-----------+
|                                  Service Layer                                   |
|                          +---------------------------+                           |
|                          |    PeerReviewService      |                           |
|                          +---------------------------+                           |
+----------------------------------------|-----------------------------------------+
                                         |
+----------------------------------------v-----------------------------------------+
|                                  Domain Layer                                    |
|  +-----------------+    +----------------+    +--------------+    +-----------+  |
|  |  ReviewSession  |    |    ChangeSet   |    |  FileChange  |    |  Comment  |  |
|  +-----------------+    +----------------+    +--------------+    +-----------+  |
|                                                      |                           |
|                                               +------v-------+                   |
|                                               |   DiffHunk   |                   |
|                                               +--------------+                   |
+----------------------------------------------------------------------------------+
                                         |
+----------------------------------------v-----------------------------------------+
|                               VCS Adapter Layer                                  |
|                          +---------------------------+                           |
|                          |  VersionControlProvider   |                           |
|                          +---------------------------+                           |
|                                        |                                         |
|                          +---------------------------+                           |
|                          | GitVersionControlProvider |                           |
|                          +---------------------------+                           |
+----------------------------------------|-----------------------------------------+
                                         |
                                  +------v-------+
                                  |  Git (CLI)   |
                                  +--------------+
```

## 2. Java Package Structure

- `eu.kalafatic.evolution.model.orchestration`: EMF Domain objects (`ReviewSession`, `Comment`, `FileChange`, `DiffHunk`, `ReviewDecision`)
- `eu.kalafatic.evolution.controller.review.service`: Business logic (`PeerReviewService`)
- `eu.kalafatic.evolution.controller.vcs`: VCS abstraction (`VersionControlProvider`) and Git implementation (`GitVersionControlProvider`)
- `eu.kalafatic.evolution.view.editors.pages`: Main `PeerReviewPage`
- `eu.kalafatic.evolution.view.editors.pages.peerreview`: Functional UI components (`FileTreeGroup`, `DiffViewerGroup`, `CommentsGroup`)

## 3. Core Class Skeletons

### ReviewSession
```java
public interface ReviewSession extends EObject {
    String getId();
    void setId(String value);
    ChangeSet getChangeSet();
    void setChangeSet(ChangeSet value);
    EList<Comment> getComments();
    ReviewDecision getDecision();
    void setDecision(ReviewDecision value);
}
```

### FileChange
```java
public class FileChange {
    private String filePath;
    private String status;
    private List<DiffHunk> hunks;
    // Getters and setters
}
```

### VersionControlProvider
```java
public interface VersionControlProvider {
    List<String> fetchCommits(File workingDir) throws Exception;
    String getDiff(File workingDir, String commitId) throws Exception;
    String getFileDiff(File workingDir, String commitId, String filePath) throws Exception;
    void checkoutBranch(File workingDir, String branchName) throws Exception;
    void commitChanges(File workingDir, String message) throws Exception;
    void push(File workingDir) throws Exception;
}
```

## 4. UI Layout Description

The Peer Review Editor uses a functional 3-panel layout:
1. **Left Panel (FileTreeGroup)**: Displays a tree of files in the project. Selecting a file triggers a diff view update.
2. **Center Panel (DiffViewerGroup)**: Renders the Git diff using an embedded SWT Browser. Supports syntax highlighting for added (green) and removed (red) lines.
3. **Right Panel (CommentsGroup)**: Displays the current review status and provides action buttons (Approve & Commit, Request Changes, Reject) along with a text area for overall comments.

## 5. Git Integration Approach

The integration is achieved via a robust CLI abstraction layer. The `GitVersionControlProvider` uses `ShellTool` to execute standard Git commands. Commit messages are safely escaped to prevent shell injection. The `VersionControlProvider` interface allows for easy extension to JGit, SVN, or Mercurial.

## 6. Example Workflow

1. **Initialize**: The editor opens and `PeerReviewPage` initializes by scanning the project files.
2. **Reviewing**: User navigates the file tree. Selecting a file executes `git diff` via `PeerReviewService`.
3. **Workflow Transition**: User starts the review, moving the session to `IN_REVIEW`.
4. **Commenting & Decision**: User adds comments and clicks "Approve & Commit".
5. **VCS Action**: `PeerReviewService` calls `vcsProvider.commitChanges()` and `vcsProvider.push()`, finalizing the review by persisting changes to the repository.
