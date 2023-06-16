package knightbrew;

import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
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

    private Map<String, ArrayList<HomeCard>> section_cards;

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
        section_cards = new HashMap<String, ArrayList<HomeCard>>();

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

        // If there are files in the archive, build it a section
        if (total_files > 0)
        {
            sections.add("archive");
        }

        total_files += countFiles(new File(path + File.separator + "Latest"));

        // Figure out which sections we have

        File latesst = new File(path + File.separator + "Latest");
        File latest = new File(path + File.separator + "Latest" + File.separator + latesst.list()[0]);

        String[] lisst = latest.list();
        Arrays.sort(lisst);
        for (String section : lisst)
        {
            File file = new File(latest.getAbsolutePath() + File.separator + section);
            if (file.isDirectory())
            {
                sections.add(file.getName());
            }
        }

        // Add sections, homepage, and index.js
        total_files += sections.size() + 1 + 1;

        // Do the archive
        buildSection(new File(path + File.separator + "Archive"), "Archive");

        if (!latest.exists() && !latest.isDirectory())
        {
            o.out(" ========= INVALID ARCHIVE ========= \n");
            o.out("exiting.\n");
            return 1;
        }

        for (String section : lisst)
        {
            File file = new File(latest.getAbsolutePath() + File.separator + section);
            System.out.println("Found file: " + file.getAbsolutePath());
            if (file.isDirectory())
            {
                System.out.println("Entering...");
                buildSection(file, file.getName());
            }
        }

        // Build the home pages
        TemplateEngine t = TemplateEngine.getInstance();
        all_cards = new ArrayList<HomeCard>();
        for (String sec : sections)
        {
            String upper_blurb = "";
            if (!sec.equals("Archive"))
            {
                upper_blurb = getUpperBlurb(latest.getAbsolutePath() + File.separator + sec);
            }
            String out = t.makeHome(sec, section_cards.get(slugify(sec)), sections, upper_blurb);

            String pt = path + File.separator + "NewSource" + File.separator + "section" + File.separator +
                        slugify(sec) + File.separator + "index.html";

            try (FileWriter sfr = new FileWriter(pt))
            {
                sfr.write(out);
                sfr.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            files_so_far++;
            f.setProgress((int)(((double)files_so_far / total_files) * 100));
            o.out("Built homepage for section " + sec + "\n");

            if (slugify(sec).equals("prompts") || slugify(sec).equals("about"))
                continue;

            all_cards.addAll(section_cards.get(slugify(sec)));
        }

        o.out("Building homepage\n");

        String upper_blurb = getUpperBlurb(latest.getAbsolutePath());
        String out = t.makeHome("Home", all_cards, sections, upper_blurb);

        String pt = path + File.separator + "NewSource" + File.separator + "index.html";

        try (FileWriter sfr = new FileWriter(pt))
        {
            sfr.write(out);
            sfr.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        files_so_far++;
        f.setProgress((int)(((double)files_so_far / total_files) * 100));
        o.out("Built homepage\n");

        // Why is converting a Date to a LocalDate so damn hard!
        out = t.makeJS("", subd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        pt = path + File.separator + "NewSource" + File.separator + "index.js";

        try (FileWriter sfr = new FileWriter(pt))
        {
            sfr.write(out);
            sfr.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
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

        // TODO: Make resources folder actually work when built in a jar
        // TODO: Move in javascript
        // TODO: About page and home page heading
        //  |-> Maybe just give every folder a SECTION file?
        // TODO: Deploying (this will suck)

        return 0;
    }

    String getUpperBlurb(String pat)
    {
        File f = new File(pat + File.separator + "SECTION.docx");
        if (!f.exists())
            return "";

        // Mammoth f

        DocumentConverter converter = new DocumentConverter().preserveEmptyParagraphs();

        String result = "";
        try
        {
            result = converter.convertToHtml(f).getValue();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
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
        Arrays.sort(items);

        if (items == null)
            return;

        try
        {
            section_cards.put(slugify(section), new ArrayList<HomeCard>());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Added thing1");

        for (int i = 0; i < items.length; i++)
        {
            String f = items[i];

            File file = new File(directory.getPath() + File.separator + f);
            if (file.isDirectory())
            {
                buildSection(file, section);
            }
            else if (file.getName().endsWith("SECTION.docx"))
            {
                // We'll deal with this... later.
            }
            else if (file.getName().toLowerCase().endsWith(".docx"))
            {

                System.out.println("Building file " + file.getName());
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
            // File old = new File(old_location);
            String new_loc = getNewLocation(file, section);
            // new File(new_loc).mkdirs();
            // old.renameTo(new File(new_loc + File.separator + "index.html"));
            try
            {
                Files.move(Path.of(old_location), Path.of(new_loc));
            }
            catch (IOException e)
            {
                o.out("Failed to re-use\n");
                e.printStackTrace();
                return;
            }
            props.setProperty("mammoth/cache/name/" + file.getAbsoluteFile(), new_loc.replace("NewSource", "Source"));

            files_so_far++;
            f.setProgress((int)(((double)files_so_far / total_files) * 100));
            o.out("Re-using file " + file.getName() + " from cache\n");
            // System.out.println("Re-using");

            // Make the card
            HomeCard h = new HomeCard();
            h.title = props.getProperty("mammoth/cache/title/" + file.getAbsoluteFile());
            h.author = props.getProperty("mammoth/cache/author/" + file.getAbsoluteFile());
            h.blurb = props.getProperty("mammoth/cache/blurb/" + file.getAbsoluteFile());
            h.date = LocalDate.parse(props.getProperty("mammoth/cache/date/" + file.getAbsoluteFile()));

            h.has_image = new File(new_loc + File.separator + "0.jpeg").exists();
            h.image_url = new_loc.replace(path + File.separator + "NewSource", "") + File.separator + "0.jpeg";

            h.url = new_loc.replace(path + File.separator + "NewSource", "");

            section_cards.get(slugify(section)).add(h);

            return;
        }

        total_images = 0;

        String new_loc = getNewLocation(file, section);
        new File(new_loc).mkdirs();

        // Actually generate it
        Lock l = new ReentrantLock();
        DocumentConverter converter =
            new DocumentConverter()
                .imageConverter(image -> {
                    l.lock();
                    long img_num = total_images;
                    total_images++;
                    l.unlock();
                    o.out("Processing image #" + img_num + "\n");
                    try
                    {
                        BufferedImage i = ImageIO.read(image.getInputStream());
                        String fname = new_loc + File.separator + img_num + ".jpeg";

                        // Create a new BufferedImage with RGB color space
                        BufferedImage rgbImage =
                            new BufferedImage(i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_RGB);

                        // Create a ColorConvertOp to convert from CMYK to RGB
                        ColorConvertOp op = new ColorConvertOp(null);

                        // Apply the conversion to the original image and store it in the new image
                        op.filter(i, rgbImage);

                        // Get an ImageWriter instance for JPEG format
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");
                        ImageWriter writer = writers.next();

                        // Get an ImageWriteParam instance
                        ImageWriteParam param = writer.getDefaultWriteParam();

                        // Set the compression mode to explicit
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                        // Set the compression quality to 0.6
                        param.setCompressionQuality(0.6f);

                        // Create an output file
                        File outputFile = new File(fname);
                        ImageOutputStream output = ImageIO.createImageOutputStream(outputFile);

                        writer.setOutput(output);

                        // Write the image with the specified parameters
                        writer.write(rgbImage);

                        // Dispose the writer
                        writer.dispose();
                    }
                    catch (Exception e)
                    {
                        o.out("Invalid image");
                        e.printStackTrace();
                    }
                    Map<String, String> attributes = new HashMap<>();

                    // Use absolute path so /<article> and /<article>/<index.html> both work
                    attributes.put("src", new_loc.replace(path + File.separator + "NewSource", "") + File.separator +
                                              img_num + ".jpeg");
                    attributes.put("alt", image.getAltText().orElse(""));
                    return attributes;
                })
                .preserveEmptyParagraphs();
        Result<String> result;

        try
        {
            result = converter.convertToHtml(file);
            String raw = converter.extractRawText(file).getValue();
            String total = result.getValue(); // The generated HTML
            // Set<String> warnings = result.getWarnings(); // Any warnings during conversion

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
                String header = total.substring(0, in + 7);
                header = header.replace("<p>", "").replace("</p>", "\n");
                String[] bits = header.split("\n");
                String title = bits[0].replace("Title:", "").trim();
                String author = bits[1].replace("Author:", "").trim();
                String date = bits[2].replace("Date:", "").trim();

                LocalDate hdate;
                // if the date doesn't fit this, crash
                DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("[yyyy-MM-dd][yyyy-M-dd][yyyy-MM-d][yyyy-M-d]");
                hdate = LocalDate.parse(date, formatter);

                FileWriter writer = new FileWriter(new_loc + File.separator + "index.html");

                // create a new string writer
                StringWriter swriter = new StringWriter();
                // store the properties without comment
                // p.store(swriter, null);
                // get the output as a string
                String output = swriter.toString();
                output = output.substring(output.indexOf("\n") + 1);
                TemplateEngine ti = TemplateEngine.getInstance();
                total = ti.makePage(title, author, hdate.toString(), total.substring(in + 7), section, sections);

                writer.append(total);
                writer.close();

                // Make the card
                HomeCard h = new HomeCard();
                h.title = title;
                h.author = author;
                int idx = 0;
                raw = raw.substring(raw.indexOf("---") + 4);
                for (int i = 0; i < 28; i++)
                {
                    idx = raw.indexOf(" ", idx + 1);
                }
                String blurb = raw.substring(0, idx == -1 ? raw.length() : idx) + (idx == -1 ? "" : "... ");
                h.blurb = blurb;

                props.setProperty("mammoth/cache/blurb/" + file.getAbsoluteFile(), blurb);
                props.setProperty("mammoth/cache/title/" + file.getAbsoluteFile(), title);
                props.setProperty("mammoth/cache/author/" + file.getAbsoluteFile(), author);
                props.setProperty("mammoth/cache/date/" + file.getAbsoluteFile(), hdate.toString());

                h.has_image = new File(new_loc + File.separator + "0.jpeg").exists();
                h.image_url = new_loc.replace(path + File.separator + "NewSource", "") + File.separator + "0.jpeg";

                h.url = new_loc.replace(path + File.separator + "NewSource", "");
                h.date = hdate;

                section_cards.get(slugify(section)).add(h);
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

        String[] listt = p.list();
        Arrays.sort(listt);
        for (String name : listt)
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
            File.separator + f_slugged;
    }

    private int countFiles(File directory)
    {
        int count = 0;
        String[] items = directory.list();
        Arrays.sort(items);
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
    private long total_images;
    private ArrayList<HomeCard> all_cards;

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
