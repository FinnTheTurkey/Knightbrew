package knightbrew;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

enum BrowseType { File, Directory }

class FileBrowse extends JPanel implements ActionListener {
  String path;
  boolean has_path;

  BrowseType browseType;

  JButton browseButton;
  JTextField pathField;

  public FileBrowse(String label, BrowseType type) {
    has_path = false;
    browseType = type;
    buildUI(label);
  }
  public FileBrowse(String label, String path, BrowseType type) {
    has_path = true;
    this.path = path;
    browseType = type;
    buildUI(label);
  }

  private void buildUI(String label) {
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(5, 5, 5, 5),
        BorderFactory.createTitledBorder(label)));

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    pathField = new JTextField(path);
    pathField.setColumns(25);
    add(pathField);

    browseButton = new JButton("Browse");
    browseButton.addActionListener(this);
    browseButton.setActionCommand("browse");
    add(browseButton);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // Browse button pressed

    // We can only do files because AWT is stupid
    // And Swing is also stupid
    FileDialog fd = new FileDialog(
        (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this),
        "Choose a file",
        browseType == BrowseType.Directory ? FileDialog.LOAD : FileDialog.LOAD);
    fd.setDirectory(path);
    // fd.setFile("*.txt");
    fd.setVisible(true);

    String filename = fd.getFile();

    if (filename == null) {
      return;
    }

    path = new File(fd.getDirectory(), filename).getPath();
    pathField.setText(path);
  }

  public String getPath() { return pathField.getText(); }

  public void setPath(String path) {
    this.path = path;
    pathField.setText(path);
  }
}
