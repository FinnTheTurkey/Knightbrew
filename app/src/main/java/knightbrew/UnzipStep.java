package knightbrew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
// import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import net.lingala.zip4j.ZipFile;

class UnzipStep implements MajorStep
{

    private OutputFunction o;
    private String path;
    private ProgressFunction f;

    @Override public String getName()
    {
        return "Unzipping...";
    };

    @Override
    public int start(String archive, String heading, String about, String current, Date subd, ProgressFunction f,
                     OutputFunction o)
    {
        this.f = f;
        this.o = o;
        AppDirs appDirs = AppDirsFactory.getInstance();
        path = appDirs.getUserDataDir("Knightbrew", null, "Knightwatch");

        File p = new File(path + File.separator + "Archive");
        if (!p.exists())
            p.mkdirs();
        p = new File(path + File.separator + "Latest");
        if (!p.exists())
            p.mkdirs();

        // Caching
        Preferences prefs = Preferences.userNodeForPackage(UnzipStep.class);

        f.setProgress(0);

        p = new File(archive);
        if (p.exists())
        {
            o.out("Found archive " + archive + "\nUnzipping.\n");
            String a = prefs.get("archive", "");
            boolean unzipit = true;
            if (a.equals(archive))
            {
                System.out.println("They are same");
                System.out.println("" + p.lastModified() + " ?= " + prefs.getLong("archive/lastmod", 0));
                // We can maybe save some work
                if (p.lastModified() == prefs.getLong("archive/lastmod", 0))
                {
                    o.out("Re-using archive from cache\n");
                    unzipit = false;
                }
            }

            if (unzipit)
            {
                // We have to do a full clean
                File r = new File(path + File.separator + "Archive");
                MammothStep.deleteDirectory(r);
                r.mkdirs();

                unzip(archive, path + File.separator + "Archive" + File.separator);
            }

            prefs.put("archive", archive);
            prefs.putLong("archive/lastmod", p.lastModified());
        }
        else
        {
            o.out("No archive; Skipping\n");

            // So MammothStep doesn't accidentally load it
            MammothStep.deleteDirectory(new File(path + File.separator + "Archive"));

            new File(path + File.separator + "Archive").mkdirs();
        }

        f.setProgress(50);

        p = new File(current);
        if (p.exists())
        {
            o.out("Found latest archive " + current + "\nUnzipping.\n");
            String a = prefs.get("current", "");
            boolean unzipit = true;
            if (a.equals(current))
            {
                // We can maybe save some work
                if (p.lastModified() == prefs.getLong("current/lastmod", 0))
                {
                    o.out("Re-using current from cache\n");
                    unzipit = false;
                }
            }

            if (unzipit)
            {
                // We have to do a full clean
                File r = new File(path + File.separator + "Latest");
                MammothStep.deleteDirectory(r);
                r.mkdirs();

                unzip(current, path + File.separator + "Latest" + File.separator);
            }

            prefs.put("current", current);
            prefs.putLong("current/lastmod", p.lastModified());
        }
        else
        {
            o.out("No archive; Skipping\n");

            // So MammothStep doesn't accidentally load it
            MammothStep.deleteDirectory(new File(path + File.separator + "Latest"));
            new File(path + File.separator + "Latest").mkdirs();
        }

        f.setProgress(100);

        return 0;
    }

    private void unzip(String zip, String dest)
    {
        o.out("Starting to unzip file " + zip + "\nThis may take a while...\n");

        try
        {
            new ZipFile(zip).extractAll(dest);
        }
        catch (Exception e)
        {
            o.out("Failed to unzip file :(\n");
            e.printStackTrace();
        }
    }

    // https://www.digitalocean.com/community/tutorials/java-unzip-file-example
    // private void unzip_old(String zipFilePath, String destDir)
    // {
    //     // A buffer for reading and writing data
    //     System.out.println("UNZIPPING " + zipFilePath + " TO " + destDir);

    //     File dir = new File(destDir);
    //     // create output directory if it doesn't exist
    //     if (!dir.exists())
    //         dir.mkdirs();

    //     FileInputStream fis;
    //     // buffer for read and write data to file
    //     byte[] buffer = new byte[1024];
    //     try
    //     {
    //         // Create a ZipFile object
    //         ZipFile zipFile = new ZipFile(zipFilePath);

    //         f.setProgress(0);
    //         int count = 0;
    //         int total = zipFile.size();

    //         zipFile.close();

    //         fis = new FileInputStream(zipFilePath);
    //         ZipInputStream zis = new ZipInputStream(fis);
    //         ZipEntry ze = zis.getNextEntry();
    //         while (ze != null)
    //         {
    //             String fileName = ze.getName();
    //             File newFile = new File(destDir + File.separator + fileName);
    //             o.out("Unzipping " + newFile.getName() + "\n");
    //             // create directories for sub directories in zip
    //             new File(newFile.getParent()).mkdirs();
    //             FileOutputStream fos = new FileOutputStream(newFile);
    //             int len;
    //             while ((len = zis.read(buffer)) > 0)
    //             {
    //                 fos.write(buffer, 0, len);
    //             }
    //             fos.close();
    //             // close this ZipEntry
    //             zis.closeEntry();
    //             ze = zis.getNextEntry();

    //             count++;
    //             f.setProgress((int)(((double)count / (double)total) * 100));
    //         }
    //         // close last ZipEntry
    //         zis.closeEntry();
    //         zis.close();
    //         fis.close();
    //     }
    //     catch (IOException e)
    //     {
    //         e.printStackTrace();
    //     }
    // }
}
