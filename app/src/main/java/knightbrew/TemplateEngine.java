package knightbrew;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
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
            root.put("content", content.replace("<img", "<img class=\"content-image\""));
            root.put("section", section);

            // Convert sections
            List<HashMap<String, Object>> secs = new ArrayList<>();
            for (String secc : sections)
            {
                HashMap<String, Object> a = new HashMap<String, Object>();
                a.put("slug", secc);
                a.put("deslug", MammothStep.toNormalText(secc));
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
