package hudson.plugins.testng;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link Publisher}
 *
 * @author nullin
 */
public class PublisherTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @WithoutJenkins
    @Test
    public void testLocateReports() throws Exception {
        // Create a temporary workspace in the system
        File w = tmp.newFolder();
        FilePath workspace = new FilePath(w);
        // Create 4 files in the workspace
        File f1 = File.createTempFile("testng-results", ".xml", w);
        f1.deleteOnExit();
        File f2 = File.createTempFile("anyname", ".xml", w);
        f2.deleteOnExit();
        File f3 = File.createTempFile("testng-results", ".xml", w);
        f3.deleteOnExit();
        File f4 = File.createTempFile("anyname", ".xml", w);
        f4.deleteOnExit();
        // Create a folder and move 2 files there
        File d1 = new File(workspace.child("subdir").getRemote());
        d1.mkdir();
        d1.deleteOnExit();
        File f5 = new File(workspace.child(d1.getName()).child(f3.getName()).getRemote());
        File f6 = new File(workspace.child(d1.getName()).child(f4.getName()).getRemote());
        f3.renameTo(f5);
        f4.renameTo(f6);
        f5.deleteOnExit();
        f6.deleteOnExit();
        // Look for files in the entire workspace recursively without providing
        // the includes parameter
        FilePath[] reports = Publisher.locateReports(workspace, "**/testng*.xml");
        Assert.assertEquals(2, reports.length);
        // Generate a includes string and look for files
        String includes = f1.getName() + "; " + f2.getName() + "; " + d1.getName();
        reports = Publisher.locateReports(workspace, includes);
        Assert.assertEquals(3, reports.length);
        // Save files in local workspace
        FilePath local = workspace.child("publishertest_localfolder");
        boolean saved = Publisher.saveReports(local, reports, System.out);
        Assert.assertTrue(saved);
        Assert.assertEquals(3, local.list().size());
        local.deleteRecursive();
    }

    @WithoutJenkins
    @Test
    public void testBuildAborted() throws Exception {
        PublisherCtor publisherCtor = new PublisherCtor().setReportFilenamePattern("testng.xml")
                        .setEscapeTestDescp(false).setEscapeExceptionMsg(false).setShowFailedBuilds(false);
        Publisher publisher = publisherCtor.getNewPublisher();
        Launcher launcherMock = mock(Launcher.class);
        AbstractBuild<?,?> buildMock = mock(AbstractBuild.class);
        BuildListener listenerMock = mock(BuildListener.class);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);

        when(buildMock.getResult()).thenReturn(Result.ABORTED);
        when(listenerMock.getLogger()).thenReturn(ps);

        Assert.assertTrue(publisher.perform(buildMock, launcherMock, listenerMock));

        String str = os.toString();
        Assert.assertTrue(str.contains("Build Aborted"));
    }

    @Test
    public void testRoundTrip() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        Publisher before = new Publisher("", false, false, true, false, 0, 0, 0, 0, 1);
        p.getPublishersList().add(before);

        r.submit(r.createWebClient().getPage(p,"configure").getFormByName("config"));

        Publisher after = p.getPublishersList().get(Publisher.class);

        r.assertEqualBeans(before, after, "reportFilenamePattern,escapeTestDescp,escapeExceptionMsg,showFailedBuilds");
    }

}