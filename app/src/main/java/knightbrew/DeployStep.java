package knightbrew;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

class DeployStep
{

    private String path;
    int start()
    {

        // pswd: werunanewspaper123!
        // AppDirs appDirs = AppDirsFactory.getInstance();
        // path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");

        // File p = new File(path + File.separator + "Source");
        // if (!p.exists())
        //     p.mkdirs();

        // File git_path = new File(path + File.separator + "GitSource");
        // if (git_path.exists())
        // {
        //     MammothStep.deleteDirectory(git_path);
        // }
        // git_path.mkdirs();

        // try
        // {

        //     try (Git git = Git.cloneRepository()
        //                        .setURI("https://github.com/Green-Robot-Dev-Studios/Knightwatch")
        //                        .setDirectory(git_path)
        //                        .call())
        //     {
        //         UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider("token", "");
        //         git.pull().setCredentialsProvider().call();
        //         git.branchCreate().setForce(true).setName(branch).setStartPoint("origin/" + branch).call();
        //         git.checkout().setName(branch).call();
        //         // Change branch
        //         // Move in files
        //         // ADD CNAME FILE!!!!!
        //         // Push
        //         // Done!

        //         // The only issue is authentication.........
        //         //
        //         https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token
        //     }
        //     catch (Exception e)
        //     {
        //         e.printStackTrace();
        //     }
        // }
        // catch (Exception e)
        // {
        //     e.printStackTrace();
        // }

        return 0;
    }
}
