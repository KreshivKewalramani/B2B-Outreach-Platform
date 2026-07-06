package com.example.demo.services;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class HtmlRendererService {

    private final Parser markdownParser;
    private final HtmlRenderer markdownRenderer;

    public HtmlRendererService() {
        this.markdownParser = Parser.builder().build();
        // Configure Markdown softbreaks to be translated to HTML break tags (<br/>)
        this.markdownRenderer = HtmlRenderer.builder()
                .softbreak("<br/>")
                .build();
    }

    public String render(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return "";
        }

        // First, convert markdown (if any) to HTML.
        // Note: Commonmark handles inline HTML and leaves it intact.
        String renderedHtml = markdownRenderer.render(markdownParser.parse(rawContent));

        // Sanitize HTML using Jsoup to prevent XSS while allowing rich styling (colors, fonts, bold, etc.)
        Safelist safelist = Safelist.relaxed()
                .addTags("span", "u", "hr", "font")
                .addAttributes("span", "style", "class")
                .addAttributes("p", "style", "class")
                .addAttributes("div", "style", "class")
                .addAttributes("font", "color", "size", "face")
                .addAttributes("a", "target", "href");

        return Jsoup.clean(renderedHtml, safelist);
    }
}
