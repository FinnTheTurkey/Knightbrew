package knightbrew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

class MammothStep implements MajorStep
{

    private OutputFunction o;
    private String path;
    private ProgressFunction f;
    int total_files = 0;
    int files_so_far = 0;
    private Preferences prefs;
    private Properties props;

    private ArrayList<String> sections;

    @Override public String getName()
    {
        return "Converting to HTML...";
    };

    @Override
    public int start(String archive, String heading, String about, String current, Date subd, ProgressFunction f,
                     OutputFunction o)
    {
        prefs = Preferences.userNodeForPackage(MammothStep.class);

        this.f = f;
        this.o = o;

        sections = new ArrayList<String>();

        AppDirs appDirs = AppDirsFactory.getInstance();
        path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");

        File p = new File(path + File.separator + "Source");
        if (!p.exists())
            p.mkdirs();

        p = new File(path + File.separator + "NewSource");
        if (p.exists())
            p.delete();

        p.mkdirs();

        props = new Properties();
        File propsp = new File(path + File.separator + "mammothcache.properties");
        if (propsp.exists() && prefs.getBoolean("mammoth/has_cache", false))
        {
            try
            {
                props.load(new FileInputStream(propsp));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
                o.out("Internal error: Could not find properties file\n");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                o.out("Internal error: Could not read properties file\n");
            }
        }

        // Recursively count # of files in Archive and Latest for the progress bar
        total_files += countFiles(new File(path + File.separator + "Archive"));
        total_files += countFiles(new File(path + File.separator + "Latest"));

        // Do the archive
        buildSection(new File(path + File.separator + "Archive"), "Archive");
        sections.add("archive");

        File latesst = new File(path + File.separator + "Latest");
        File latest = new File(path + File.separator + "Latest" + File.separator + latesst.list()[0]);

        if (!latest.exists() && !latest.isDirectory())
        {
            o.out(" ========= INVALID ARCHIVE ========= \n");
            o.out("exiting.\n");
            return 1;
        }

        for (String section : latest.list())
        {
            File file = new File(latest.getAbsolutePath() + File.separator + section);
            System.out.println("Found file: " + file.getAbsolutePath());
            if (file.isDirectory())
            {
                System.out.println("Entering...");
                buildSection(file, file.getName());
                sections.add(file.getName());
            }
        }

        try
        {
            deleteDirectory(new File(path + File.separator + "Source"));
            // p.renameTo(new File(path + File.separator + "Source"));
            Files.move(Paths.get(path + File.separator + "NewSource"), Paths.get(path + File.separator + "Source"));
            deleteDirectory(new File(path + File.separator + "NewSource"));

            URL dir_url = ClassLoader.getSystemResource("css");
            copyDirectory(Paths.get(dir_url.toURI()),
                          Paths.get(path + File.separator + "Source" + File.separator + "css"));
            dir_url = ClassLoader.getSystemResource("assets");
            copyDirectory(Paths.get(dir_url.toURI()),
                          Paths.get(path + File.separator + "Source" + File.separator + "assets"));
            dir_url = ClassLoader.getSystemResource("icons");
            copyDirectory(Paths.get(dir_url.toURI()),
                          Paths.get(path + File.separator + "Source" + File.separator + "icons"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            props.store(new FileWriter(propsp), "");
            prefs.putBoolean("mammoth/has_cache", true);
        }
        catch (IOException e)
        {
            o.out("Internal error: Could not write properties file\n");
            e.printStackTrace();
        }

        f.setProgress(100);
        o.out("Finished converting files to HTML\n");

        return 0;
    }

    static void copyDirectory(Path sourcePath, Path destinationPath)
    {
        // Copy the folder recursively
        try
        {
            Files.walk(sourcePath).forEach(source -> {
                Path destination = destinationPath.resolve(sourcePath.relativize(source));
                try
                {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void buildSection(File directory, String section)
    {
        FilenameFilter filter = new FilenameFilter() {
            @Override public boolean accept(File dir, String name)
            {
                // Return true for all files and directories
                return true;
            }
        };
        System.out.println("Entering: " + directory.getAbsolutePath());
        String[] items = directory.list(filter);

        if (items == null)
            return;

        for (int i = 0; i < items.length; i++)
        {
            String f = items[i];

            File file = new File(directory.getPath() + File.separator + f);
            if (file.isDirectory())
            {
                buildSection(file, section);
            }
            else if (file.getName().toLowerCase().endsWith(".docx"))
            {
                buildFile(file, section);
            }
        }
    }

    private void buildFile(File file, String section)
    {
        // Check cache
        // System.out.println("Building file: " + file.getName());
        long current_size = file.getTotalSpace();
        long old_size = Long.parseLong(props.getProperty("mammoth/cache/size/" + file.getAbsoluteFile(), "0"));
        String old_location = props.getProperty("mammoth/cache/name/" + file.getAbsoluteFile(), "");

        if (old_size == current_size)
        {
            // Horray!
            // Just move it over
            File old = new File(old_location);
            String new_loc = getNewLocation(file, section);
            old.renameTo(new File(new_loc));
            props.setProperty("mammoth/cache/name/" + file.getAbsoluteFile(), new_loc.replace("NewSource", "Source"));

            files_so_far++;
            f.setProgress((int)(((double)files_so_far / total_files) * 100));
            o.out("Re-using file " + file.getName() + " from cache\n");
            // System.out.println("Re-using");

            return;
        }

        // Actually generate it
        DocumentConverter converter = new DocumentConverter();
        Result<String> result;

        try
        {
            result = converter.convertToHtml(file);
            String total = result.getValue(); // The generated HTML
            // Set<String> warnings = result.getWarnings(); // Any warnings during conversion

            String new_loc = getNewLocation(file, section);

            int in = total.indexOf("---</p>");
            if (in == -1)
                in = total.indexOf("---");

            if (in == -1)
            {
                o.out(" ======== MALFORMED HEADER: " + file.getName() + " ======== \n");
                o.out("Removing from build...\n");
                return;
            }

            try
            {
                // TODO: This is really, really, really, really slow!!!
                String header = total.substring(0, in + 7);
                header = header.replace("<p>", "").replace("</p>", "\n");
                String[] bits = header.split("\n");
                String title = bits[0].replace("Title:", "").trim();
                String author = bits[1].replace("Author:", "").trim();
                String date = bits[2].replace("Date:", "").trim();
                // TODO: verify date

                // Properties p = new Properties();
                // p.setProperty("title", title);
                // p.setProperty("author", author);
                // p.setProperty("date", date);
                // p.setProperty("type", "post");
                // p.setProperty("status", "published");

                FileWriter writer = new FileWriter(new_loc);

                // create a new string writer
                StringWriter swriter = new StringWriter();
                // store the properties without comment
                // p.store(swriter, null);
                // get the output as a string
                String output = swriter.toString();
                output = output.substring(output.indexOf("\n") + 1);
                TemplateEngine ti = TemplateEngine.getInstance();
                total = ti.makePage(title, author, date, total.substring(in + 7), section, sections);

                writer.append(total);
                writer.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();

                o.out(" -------- MALFORMED HEADER: " + file.getName() + " -------- \n");
                o.out("Removing from build...");
                return;
            }

            files_so_far++;
            f.setProgress((int)(((double)files_so_far / total_files) * 100));
            o.out("Build file " + file.getName() + "\n");

            // Add to cache
            props.setProperty("mammoth/cache/name/" + file.getAbsoluteFile(), new_loc.replace("NewSource", "Source"));
            props.setProperty("mammoth/cache/size/" + file.getAbsoluteFile(),
                              Long.toString(new File(new_loc).getTotalSpace()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            o.out("Internal error\n");
        }
    }

    private String getNewLocation(File f, String section)
    {

        String f_slugged = slugify(f.getName());

        // Make sure it doesn't already exist...
        File p = new File(path + File.separator + "NewSource" + File.separator + "section" + File.separator +
                          slugify(section));
        if (!p.exists())
            p.mkdirs();

        for (String name : p.list())
        {
            if (name == f_slugged)
            {

                String filename = f.getAbsolutePath();
                // Remove file extension
                int dotIndex = filename.lastIndexOf(".");
                if (dotIndex > 0)
                {
                    filename = filename.substring(0, dotIndex);
                }

                // This will end up with whatever-1-1 etc...
                // But I'm too lazy to fix that
                return getNewLocation(new File(filename + "-1"), section);
            }
        }

        return path + File.separator + "NewSource" + File.separator + "section" + File.separator + slugify(section) +
            File.separator + f_slugged + ".html";
    }

    private int countFiles(File directory)
    {
        int count = 0;
        String[] items = directory.list();
        for (String f : items)
        {
            File file = new File(directory.getPath() + File.separator + f);
            if (file.isDirectory())
            {
                count += countFiles(file);
            }
            else if (file.getName().toLowerCase().endsWith(".docx"))
            {
                count++;
            }
        }

        return count;
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String slugify(String filename)
    {
        // Remove file extension
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0)
        {
            filename = filename.substring(0, dotIndex);
        }
        // Normalize and replace
        String nowhitespace = WHITESPACE.matcher(filename).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        // Convert to lowercase
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public static String toTitleCase(String input)
    {
        if (input == null || input.isEmpty())
        {
            return input;
        }
        StringBuilder titleCase = new StringBuilder();
        for (String part : input.split(" "))
        {
            if (!part.isEmpty())
            {
                titleCase.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        return titleCase.toString().trim();
    }

    public static String toNormalText(String input)
    {
        if (input == null || input.isEmpty())
        {
            return input;
        }
        // Split the string by hyphens or underscores
        String[] parts = input.split("[-_]");
        // Make the first letter of each part uppercase
        parts = Arrays.stream(parts)
                    .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                    .toArray(String[] ::new);
        // Join the parts with spaces
        input = String.join(" ", parts);
        // Return the result
        return input;
    }

    // recursive method to delete a directory and all its contents
    public static boolean deleteDirectory(File dir)
    {
        // get all files and subdirectories
        File[] allContents = dir.listFiles();
        if (allContents != null)
        {
            // delete each file and subdirectory
            for (File file : allContents)
            {
                deleteDirectory(file);
            }
        }
        // delete the directory itself
        return dir.delete();
    }
}
