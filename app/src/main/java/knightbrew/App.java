package knightbrew;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.*;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.jdesktop.swingx.JXDatePicker;

/*
   Useful stuff:
    - JGit: Pure Java git implementation
    - Mammoth Java: Convert docx to html
    - JBake: Static site generator
    - Java.util.zip: Unzipping

https://docs.oracle.com/javase/tutorial/uiswing/components/index.html
*/

/*
 * ButtonDemo.java requires the following files:
 *   images/right.gif
 *   images/middle.gif
 *   images/left.gif
 */
public class App extends JPanel implements ActionListener
{
    protected JButton b1, b2, b3;
    protected FileBrowse archiveFile, introFile, aboutFile, latestFile;
    protected JXDatePicker submissionDate;
    private JButton b4;
    private String path;

    public App()
    {
        // Load path
        AppDirs appDirs = AppDirsFactory.getInstance();
        path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");
        File p = new File(path);
        if (!p.exists())
            p.mkdirs();

        // Setup layout
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel step1 = new JPanel();
        step1.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                                                           BorderFactory.createTitledBorder("Step 1")));

        step1.setLayout(new BoxLayout(step1, BoxLayout.PAGE_AXIS));

        archiveFile = new FileBrowse("Archive folder", BrowseType.Directory);
        step1.add(archiveFile);
        introFile = new FileBrowse("Intro heading document", BrowseType.Directory);
        step1.add(introFile);

        aboutFile = new FileBrowse("About page document", BrowseType.Directory);
        step1.add(aboutFile);

        add(step1);

        JPanel step2 = new JPanel();
        step2.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                                                           BorderFactory.createTitledBorder("Step 2")));

        step2.setLayout(new BoxLayout(step2, BoxLayout.PAGE_AXIS));

        latestFile = new FileBrowse("Latest Archive", BrowseType.File);
        step2.add(latestFile);

        submissionDate = new JXDatePicker();

        // BIG HACK
        submissionDate.getEditor().setColumns(28);

        JPanel dbox = new JPanel();
        dbox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                                                          BorderFactory.createTitledBorder("Submission Date")));

        dbox.setLayout(new BoxLayout(dbox, BoxLayout.PAGE_AXIS));
        dbox.add(submissionDate);
        step2.add(dbox);
        add(step2);

        JPanel step3 = new JPanel();
        step3.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                                                           BorderFactory.createTitledBorder("Step 3")));

        step3.setLayout(new BoxLayout(step3, BoxLayout.LINE_AXIS));

        JPanel bl;

        b1 = new JButton("Build");
        b1.addActionListener(this);
        b1.setActionCommand("build");
        bl = new JPanel(new BorderLayout());
        bl.add(b1);
        step3.add(bl);

        b4 = new JButton("Clean");
        b4.addActionListener(this);
        b4.setActionCommand("clean");
        bl = new JPanel(new BorderLayout());
        bl.add(b4);
        step3.add(b4);

        b2 = new JButton("Test");
        b2.addActionListener(this);
        b2.setActionCommand("test");
        bl = new JPanel(new BorderLayout());
        bl.add(b2);
        step3.add(bl);

        b3 = new JButton("Deploy");
        b3.addActionListener(this);
        b3.setActionCommand("deploy");
        bl = new JPanel(new BorderLayout());
        bl.add(b3);
        step3.add(bl);

        add(step3);

        loadPrefs();
    }

    private void loadPrefs()
    {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        archiveFile.setPath(prefs.get("archive_file", ""));
        introFile.setPath(prefs.get("intro_file", ""));
        aboutFile.setPath(prefs.get("about_file", ""));
        latestFile.setPath(prefs.get("latest_file", ""));
        submissionDate.setDate(new Date(prefs.getLong("submission_date", new Date().getTime())));
    }

    private void savePrefs()
    {
        Preferences prefs = Preferences.userNodeForPackage(App.class);
        prefs.put("archive_file", archiveFile.getPath());
        prefs.put("intro_file", introFile.getPath());
        prefs.put("about_file", aboutFile.getPath());
        prefs.put("latest_file", latestFile.getPath());
        prefs.putLong("submission_date", submissionDate.getDate().getTime());
    }

    public void actionPerformed(ActionEvent e)
    {
        savePrefs();
        switch (e.getActionCommand())
        {
        case "clean":
            // Just nuke the directory
            File p = new File(path);
            p.delete();
            p.mkdirs();

            // Nuke the cache
            Preferences prefs = Preferences.userNodeForPackage(App.class);
            try
            {
                prefs.removeNode();
            }
            catch (BackingStoreException e1)
            {
                e1.printStackTrace();
            }

            savePrefs();

            break;

        case "build":
            build();
            break;
        case "test":

            HttpServerDialog dialog =
                new HttpServerDialog((JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this));
            dialog.setVisible(true);
            break;
        }
    }

    public void build()
    {

        JDialog dialog = new JDialog((JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this), "Builder");
        dialog.setSize(500, 400);

        dialog.setContentPane(new Builder(archiveFile.getPath(), introFile.getPath(), archiveFile.getPath(),
                                          latestFile.getPath(), submissionDate.getDate()));
        dialog.setVisible(true);
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path)
    {
        java.net.URL imgURL = App.class.getResource(path);
        if (imgURL != null)
        {
            return new ImageIcon(imgURL);
        }
        else
        {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI()
    {

        // Create and set up the window.
        JFrame frame = new JFrame("Knight Brew");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        App newContentPane = new App();
        newContentPane.setOpaque(true); // content panes must be opaque
        frame.setContentPane(newContentPane);

        // Display the window.
        frame.pack();

        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        try
        {
            // Set system look and feel
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            // Handle exception
            e.printStackTrace();
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                createAndShowGUI();
            }
        });
    }
}
