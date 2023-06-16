package knightbrew;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

class TemplateEngine
{
    static TemplateEngine inst = null;

    static TemplateEngine getInstance()
    {
        if (inst == null)
        {
            inst = new TemplateEngine();
        }

        return inst;
    }

    Configuration cfg;

    TemplateEngine()
    {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        // Load the directory as a resource
        URL dir_url = ClassLoader.getSystemResource("_includes");
        // Turn the resource into a File object
        File dir;
        try
        {
            dir = new File(dir_url.toURI());
            cfg.setDirectoryForTemplateLoading(dir);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());
    }

    String makePage(String title, String author, String date, String content, String section,
                    ArrayList<String> sections)
    {
        try
        {
            Template temp = cfg.getTemplate("article.ftl");
            // Load up the data
            Map<String, Object> root = new HashMap<>();
            root.put("title", title);
            root.put("author", author);
            // TODO: Format the date prettily
            root.put("date", date);
            root.put("content",
                     content.replace("<img", "<img class=\"content-image\" loading=lazy").replace("<p></p>", "<br />"));
            root.put("section", MammothStep.slugify(section));

            // Convert sections
            List<HashMap<String, Object>> secs = new ArrayList<>();
            for (String secc : sections)
            {
                HashMap<String, Object> a = new HashMap<String, Object>();
                a.put("slug", MammothStep.slugify(secc));
                a.put("deslug", MammothStep.toNormalText(secc));
                secs.add(a);
            }

            root.put("sections", secs);

            StringWriter s = new StringWriter();
            temp.process(root, s);

            return s.toString();
        }
        catch (Exception e)
        {
            System.out.println(" ===== Failed to build template. Stack trace:");
            e.printStackTrace();
            return "ERROR";
        }
    }

    String makeJS(String section, LocalDate date)
    {
        try
        {
            Template temp = cfg.getTemplate("index.ftl");

            Map<String, Object> root = new HashMap<>();
            root.put("year", Integer.toString(date.getYear()));
            root.put("month", date.getMonthValue());
            root.put("date", date.getDayOfMonth());

            StringWriter s = new StringWriter();
            temp.process(root, s);

            return s.toString();
        }
        catch (Exception e)
        {
            System.out.println(" ===== Failed to build template. Stack trace:");
            e.printStackTrace();
            return "ERROR";
        }
    }

    String makeHome(String section, ArrayList<HomeCard> content, ArrayList<String> sections, String upper_blurb)
    {
        try
        {
            Template temp = cfg.getTemplate("home.ftl");
            // Load up the data
            Map<String, Object> root = new HashMap<>();
            root.put("section", MammothStep.slugify(section));
            root.put("content", upper_blurb);
            root.put("title", section);

            // Sort content
            content.sort(new Comparator<>() {
                @Override public int compare(HomeCard c1, HomeCard c2)
                {
                    return (int)(c2.date.toEpochDay() - c1.date.toEpochDay());
                }
            });

            // Convert sections
            List<Map<String, Object>> its = new ArrayList<>();
            for (HomeCard it : content)
            {
                its.add(it.getMap());
            }

            root.put("items", its);

            root.put("section", MammothStep.slugify(section));

            // Convert sections
            List<HashMap<String, Object>> secs = new ArrayList<>();
            for (String secc : sections)
            {
                if (MammothStep.slugify(secc).equals("about"))
                    continue;

                HashMap<String, Object> a = new HashMap<String, Object>();
                a.put("slug", MammothStep.slugify(secc));
                a.put("deslug", MammothStep.toNormalText(secc));
                secs.add(a);
            }

            root.put("sections", secs);

            StringWriter s = new StringWriter();
            temp.process(root, s);

            return s.toString();
        }
        catch (Exception e)
        {
            System.out.println(" ===== Failed to build template. Stack trace:");
            e.printStackTrace();
            return "ERROR";
        }
    }
}

class HomeCard
{
    public String title;
    public String author;
    public boolean has_image;
    public String image_url;
    public String blurb;
    public String url;

    public LocalDate date;

    Map<String, Object> getMap()
    {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("author", author);
        m.put("has_image", has_image);
        m.put("image_url", image_url);
        m.put("blurb", blurb);
        m.put("url", url);

        return m;
    }
}
