package com.scms.core.competition.service;

import com.scms.core.competition.domain.CompetitionLeadEntity;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import com.scms.core.competition.domain.CompetitionSourceEntity;
import com.scms.core.competition.domain.CompetitionSourceEntity.SourceType;
import com.scms.core.competition.repository.CompetitionLeadRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rule-based scanning: fetches official-website pages and detects competition
 * related text by keyword matching. No AI involved - leads still require
 * admin confirmation before entering the competition library.
 */
@Service
public class CompetitionScanService {

    private static final List<String> KEYWORDS = List.of(
            "比赛", "竞赛", "大赛", "报名", "参赛", "截止", "选拔", "赛事",
            "青少年", "科技创新", "机器人", "编程", "信息学", "创新"
    );
    private static final int TIMEOUT_SECONDS = 15;
    private static final int MAX_FETCH_BYTES = 1_500_000;
    private static final int MAX_TEXT_CHARS = 200_000;
    private static final int SNIPPET_WINDOW_LINES = 2;
    private static final int SNIPPET_MAX_CHARS = 400;

    private final CompetitionLeadRepository leadRepository;
    private final HttpClient httpClient;

    public CompetitionScanService(CompetitionLeadRepository leadRepository) {
        this.leadRepository = leadRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Scans a single source; returns how many new leads were created.
     */
    public int scanSource(CompetitionSourceEntity source) {
        if (source.getSourceType() != SourceType.OFFICIAL_WEBSITE || isBlank(source.getUrl())) {
            return 0;
        }
        String html = fetch(source.getUrl());
        if (html == null) {
            return 0;
        }
        String text = htmlToText(html);
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }
        List<String> hitLines = extractHitLines(text);
        if (hitLines.isEmpty()) {
            return 0;
        }

        String title = buildTitle(hitLines, source.getName());
        String snippet = buildSnippet(text, hitLines.get(0));

        if (leadRepository.existsByTitleAndStatus(title, LeadStatus.PENDING)) {
            return 0;
        }

        CompetitionLeadEntity lead = new CompetitionLeadEntity();
        lead.setSourceId(source.getId());
        lead.setTitle(title);
        lead.setUrl(source.getUrl());
        lead.setSnippet(snippet);
        lead.setDetectedAt(Instant.now());
        lead.setStatus(LeadStatus.PENDING);
        leadRepository.save(lead);
        return 1;
    }

    /**
     * Creates a lead from manually pasted article content (wechat etc).
     */
    public int importArticle(String title, String content) {
        String cleanTitle = isBlank(title) ? extractTitleFromContent(content) : title.trim();
        if (isBlank(cleanTitle) && isBlank(content)) {
            return 0;
        }
        List<String> hitLines = extractHitLines(content == null ? "" : content);
        String resolvedTitle = cleanTitle.length() > 300 ? cleanTitle.substring(0, 300) : cleanTitle;
        if (isBlank(resolvedTitle)) {
            if (hitLines.isEmpty()) {
                return 0;
            }
            resolvedTitle = hitLines.get(0).length() > 300 ? hitLines.get(0).substring(0, 300) : hitLines.get(0);
        }
        if (leadRepository.existsByTitleAndStatus(resolvedTitle, LeadStatus.PENDING)) {
            return 0;
        }

        CompetitionLeadEntity lead = new CompetitionLeadEntity();
        lead.setTitle(resolvedTitle);
        lead.setSnippet(hitLines.isEmpty() ? null : buildSnippet(content, hitLines.get(0)));
        lead.setDetectedAt(Instant.now());
        lead.setStatus(LeadStatus.PENDING);
        leadRepository.save(lead);
        return 1;
    }

    private List<String> extractHitLines(String text) {
        return List.of(text.split("\n")).stream()
                .map(String::trim)
                .filter(line -> line.length() >= 6 && line.length() <= 200)
                .filter(line -> KEYWORDS.stream().anyMatch(line::contains))
                .distinct()
                .limit(10)
                .toList();
    }

    private String buildTitle(List<String> hitLines, String fallback) {
        String first = hitLines.get(0);
        if (first.length() <= 60) {
            return first;
        }
        int cut = first.indexOf("。");
        String candidate = cut > 0 && cut <= 60 ? first.substring(0, cut) : first.substring(0, 60);
        return isBlank(candidate) ? fallback : candidate;
    }

    private String buildSnippet(String text, String hitLine) {
        String[] lines = text.split("\n");
        int index = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(hitLine)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return truncate(hitLine, SNIPPET_MAX_CHARS);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = Math.max(0, index - SNIPPET_WINDOW_LINES);
             i <= Math.min(lines.length - 1, index + SNIPPET_WINDOW_LINES);
             i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                builder.append(line).append("\n");
            }
            if (builder.length() >= SNIPPET_MAX_CHARS) {
                break;
            }
        }
        return truncate(builder.toString(), SNIPPET_MAX_CHARS);
    }

    private String extractTitleFromContent(String content) {
        String firstLine = content == null ? "" : content.trim();
        if (firstLine.isEmpty()) {
            return "";
        }
        int newline = firstLine.indexOf('\n');
        String candidate = newline > 0 ? firstLine.substring(0, newline) : firstLine;
        return candidate.length() > 60 ? candidate.substring(0, 60) : candidate;
    }

    private String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            String body = response.body();
            return body.length() > MAX_FETCH_BYTES ? body.substring(0, MAX_FETCH_BYTES) : body;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String htmlToText(String html) {
        String withoutScripts = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String withoutTags = withoutScripts.replaceAll("(?is)<[^>]+>", "\n");
        String decoded = decodeEntities(withoutTags);
        return decoded.replaceAll("[\\r\\t]+", "").replaceAll("[ \\u00a0]+", " ");
    }

    private String decodeEntities(String input) {
        return input
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”");
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
