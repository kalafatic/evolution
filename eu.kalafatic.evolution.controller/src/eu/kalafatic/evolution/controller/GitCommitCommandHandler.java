package eu.kalafatic.evolution.controller;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class GitCommitCommandHandler extends AbstractOrchestratorHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Orchestrator orchestrator = getOrchestrator(event);
        if (orchestrator != null) {
            EObject git = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("git"));
            if (git != null) {
                String repositoryUrl = (String) git.eGet(git.eClass().getEStructuralFeature("repositoryUrl"));
                String branch = (String) git.eGet(git.eClass().getEStructuralFeature("branch"));
                String username = (String) git.eGet(git.eClass().getEStructuralFeature("username"));
                String localPath = (String) git.eGet(git.eClass().getEStructuralFeature("localPath"));

                if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                    commitToGit(HandlerUtil.getActiveShell(event), repositoryUrl, branch, username, localPath);
                }
            }
        }
        return null;
    }

    private void commitToGit(Shell shell, String repositoryUrl, String branch, String username, String localPathStr) {
        File localPath = new File(localPathStr);

        try {
            Git git;
            if (localPath.exists()) {
                git = Git.open(localPath);
            } else {
                git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(localPath)
                    .call();
            }

            git.add().addFilepattern("orchestrator.xml").call();
            git.commit().setMessage("Updating orchestrator configuration").call();

            CredentialsProvider credentialsProvider = CredentialsProvider.getDefault();
            git.push()
               .setCredentialsProvider(credentialsProvider)
               .setRemote("origin")
               .call();

            System.out.println("Committed and pushed orchestrator.xml to " + repositoryUrl);
            git.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
