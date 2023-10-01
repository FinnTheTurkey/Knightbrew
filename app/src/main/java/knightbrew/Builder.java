package knightbrew;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.*;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

@FunctionalInterface
interface ProgressFunction {
    void setProgress(int progress);
}

@FunctionalInterface
interface OutputFunction {
    void out(String x);
}

interface MajorStep
{
    String getName();

    int start(String archive, String heading, String about, String current, Date subd, ProgressFunction f,
              OutputFunction o);
}

class MoveStep implements MajorStep
{
    @Override public String getName()
    {
        return "Moving...";
    }

    @Override
    public int start(String archive, String heading, String about, String current, Date subd, ProgressFunction f,
                     OutputFunction o)
    {
        f.setProgress(0);

        Preferences prefs = Preferences.userNodeForPackage(App.class);
        String output_file = prefs.get("output_file", "");

        AppDirs appDirs = AppDirsFactory.getInstance();
        String path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");

        MammothStep.copyDirectory(Paths.get(path + File.separator + "Source"), Paths.get(output_file));
        o.out("Moved output to given directory!");
        return 0;
    }
}

public class Builder extends JPanel implements ActionListener, PropertyChangeListener
{

    private JProgressBar progressBar;
    private JButton startButton;
    private JTextArea taskOutput;
    private Task task;

    private JLabel majorStep;
    private String archive;
    private String heading;
    private String about;
    private String current;
    private Date subd;

    class Task extends SwingWorker<Void, Void>
    {
        /*
         * Main task. Executed in background thread.
         */
        @Override public Void doInBackground()
        {

            try
            {
                if (runMajorStep(new UnzipStep()) != 0)
                    return null;

                if (runMajorStep(new MammothStep()) != 0)
                    return null;
                if (runMajorStep(new MoveStep()) != 0)
                    return null;
            }
            catch (Exception e)
            {
                majorStep.setText("It broke\n");
                taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
            }

            majorStep.setText("Done!\n");
            taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
            return null;
        }

        int runMajorStep(MajorStep s)
        {
            majorStep.setText(s.getName());
            System.out.println("Starting new Major Step");
            taskOutput.append("Starting: " + s.getName() + "\n");

            taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
            return s.start(archive, heading, about, current, subd,
                           (int progress)
                               -> { setProgress(Math.min(progress, 100)); },
                           (String i) -> {
                               taskOutput.append(i);

                               taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
                           });
        }

        /*
         * Executed in event dispatching thread
         */
        @Override public void done()
        {
            Toolkit.getDefaultToolkit().beep();
            // startButton.setEnabled(true);
            setCursor(null); // turn off the wait cursor
            taskOutput.append("Done!\n");
        }
    }

    public Builder(String archive, String heading, String about, String current, Date subd)
    {
        super(new BorderLayout());
        this.archive = archive;
        this.heading = heading;
        this.about = about;
        this.current = current;
        this.subd = subd;

        // Create the demo's UI.
        JPanel bl;
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // startButton = new JButton("Start");
        // startButton.setActionCommand("start");
        // startButton.addActionListener(this);
        // bl = new JPanel(new BorderLayout());
        // bl.add(startButton);
        // panel.add(bl);

        majorStep = new JLabel("Building...");
        panel.add(majorStep);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        bl = new JPanel(new BorderLayout());
        bl.add(progressBar);
        panel.add(bl);

        taskOutput = new JTextArea(5, 20);
        taskOutput.setMargin(new Insets(5, 5, 5, 5));
        taskOutput.setEditable(false);
        bl = new JPanel(new BorderLayout());
        bl.add(taskOutput);

        add(panel, BorderLayout.PAGE_START);
        add(new JScrollPane(bl), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    /**
     * Invoked when the user presses the start button.
     */
    public void actionPerformed(ActionEvent evt)
    {
        startButton.setEnabled(false);
        // Instances of javax.swing.SwingWorker are not reusuable, so
        // we create new instances as needed.
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if ("progress" == evt.getPropertyName())
        {
            int progress = (Integer)evt.getNewValue();
            progressBar.setValue(progress);
        }
    }
}
