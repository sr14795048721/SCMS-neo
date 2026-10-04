package com.scms.core.yinzhi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.yinzhi.dto.YinzhiAiDeviceCommandResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionLogResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionRunResponse;
import com.scms.core.yinzhi.service.YinzhiAiInspectionQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/demo-observer")
public class YinzhiDemoObserverController {

    private static final String TELEMETRY_PROJECT = "yinzhi_guanjia";
    private static final int ONLINE_TREND_HOURS = 24;
    private static final int ONLINE_SCORE_MAX = 30;
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CHINA_ZONE);
    private static final DateTimeFormatter TREND_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(CHINA_ZONE);

    private final YinzhiAiInspectionQueryService queryService;
    private final IotDeviceTelemetryEventRepository telemetryEventRepository;
    private final ObjectMapper objectMapper;
    private final String observerToken;

    public YinzhiDemoObserverController(
            YinzhiAiInspectionQueryService queryService,
            IotDeviceTelemetryEventRepository telemetryEventRepository,
            ObjectMapper objectMapper,
            @Value("${demo.observer-token:yinzhi-demo}") String observerToken
    ) {
        this.queryService = queryService;
        this.telemetryEventRepository = telemetryEventRepository;
        this.objectMapper = objectMapper;
        this.observerToken = observerToken == null ? "" : observerToken.trim();
    }

    @GetMapping(value = "/yinzhi-guanjia", produces = MediaType.TEXT_HTML_VALUE)
    public String yinzhiGuanjia(@RequestParam(value = "token", required = false) String token) {
        if (!observerToken.isEmpty() && !observerToken.equals(token == null ? "" : token.trim())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid observer token");
        }
        YinzhiAiInspectionResponse response = queryService.getObserverLatest();
        return buildHtml(response);
    }

    private String buildHtml(YinzhiAiInspectionResponse response) {
        YinzhiAiInspectionRunResponse run = response.latestRun();
        String runSummary = run == null
                ? "<p class=\"empty\">暂无 AI 自主巡检记录。请先让 ESP32 上传一次状态。</p>"
                : """
                <div class="grid">
                  <div><b>服务端时间</b><span>%s</span></div>
                  <div><b>AI 巡检编号</b><span>%s</span></div>
                  <div><b>遥测事件编号</b><span>%s</span></div>
                  <div><b>ESP32 上传序号</b><span>%s</span></div>
                  <div><b>设备编号</b><span>%s</span></div>
                  <div><b>巡检状态</b><span>%s</span></div>
                  <div><b>主人状态</b><span>%s</span></div>
                  <div><b>风险等级</b><span>%s</span></div>
                </div>
                <section><h2>AI 巡检报告</h2><pre>%s</pre></section>
                """.formatted(
                        html(formatTime(response.serverTime())),
                        html(run.inspectionRunId()),
                        html(run.telemetryEventId()),
                        html(run.uploadSequence()),
                        html(run.deviceId()),
                        html(translateStatus(run.status())),
                        html(translateOwnerPresence(run.ownerPresence())),
                        html(translateRiskLevel(run.riskLevel())),
                        html(run.aiReport())
                );

        StringBuilder logs = new StringBuilder();
        for (YinzhiAiInspectionLogResponse item : response.logs()) {
            logs.append("<li><time>")
                    .append(html(formatTime(item.createdAt())))
                    .append("</time><strong>")
                    .append(html(translateStage(item.stage())))
                    .append("</strong><span>")
                    .append(html(item.message()))
                    .append("</span></li>");
        }
        StringBuilder commands = new StringBuilder();
        for (YinzhiAiDeviceCommandResponse item : response.commands()) {
            commands.append("<li><strong>")
                    .append(html(translateCommandType(item.commandType())))
                    .append("</strong><span>")
                    .append(html(translateStatus(item.status())))
                    .append(" / ")
                    .append(html(translateRiskLevel(item.riskLevel())))
                    .append(" / ")
                    .append(html(item.detail()));
            if (item.verificationMessage() != null && !item.verificationMessage().isBlank()) {
                commands.append("<small>闭环验证：")
                        .append(html(item.verificationMessage()))
                        .append("；验证遥测=")
                        .append(html(item.verificationTelemetryEventId()))
                        .append("，上传序号=")
                        .append(html(item.verificationUploadSequence()))
                        .append("</small>");
            }
            if (item.ackMessage() != null && !item.ackMessage().isBlank()) {
                commands.append("<small>确认/回执：")
                        .append(html(item.ackMessage()))
                        .append("</small>");
            }
            commands.append("</span></li>");
        }

        String rawJson = run == null ? "{}" : prettyJson(run.rawTelemetry());
        String decisionJson = run == null ? "{}" : prettyJson(run.decision());
        String onlineTrend = buildOnlineTrendSection();
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta http-equiv="refresh" content="2">
                  <title>隐智管家 AI 自主巡检服务端观察页</title>
                  <style>
                    body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;margin:0;background:#f6f7fb;color:#172033}
                    header{background:#11233f;color:white;padding:22px 28px}
                    main{padding:22px 28px;max-width:1180px;margin:auto}
                    h1{margin:0;font-size:24px} h2{font-size:18px;margin:0 0 12px}
                    .hint{opacity:.8;margin-top:8px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:18px 0}
                    .grid div,section{background:white;border:1px solid #dce2ee;border-radius:8px;padding:14px;box-shadow:0 8px 24px rgba(17,35,63,.06)}
                    b{display:block;font-size:12px;color:#64708a} span{display:block;margin-top:6px;font-weight:700}
                    pre{white-space:pre-wrap;word-break:break-word;background:#101827;color:#d8e7ff;border-radius:8px;padding:14px;overflow:auto}
                    ul{list-style:none;padding:0;margin:0;display:grid;gap:10px} li{background:#fff;border:1px solid #dce2ee;border-radius:8px;padding:12px}
                    time{display:block;color:#64708a;font-size:12px;margin-bottom:4px} strong{display:block;color:#0f5fd0;margin-bottom:4px}.empty{font-weight:700} small{display:block;color:#0f766e;margin-top:6px;font-weight:700}
                    .trend-card{padding:16px 18px}.trend-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:6px}
                    .trend-note{color:#64708a;font-size:12px;line-height:1.45}.trend-score{color:#0f5fd0;font-weight:800}
                    .trend-svg{width:100%%;height:auto;display:block}.trend-axis{fill:#64708a;font-size:11px}.trend-grid{stroke:#d8e1ef;stroke-width:1}.trend-line{fill:none;stroke:#1769d5;stroke-width:3}.trend-point{fill:#1769d5}
                  </style>
                </head>
                <body>
                  <header>
                    <h1>隐智管家 AI 自主巡检服务端观察页</h1>
                    <div class="hint">此页面由 SCMS 后端实时查询数据库生成，每 2 秒刷新。现场请核对 ESP32 串口、服务端、App 的“上传序号 / 遥测事件编号 / AI 巡检编号”。</div>
                  </header>
                  <main>
                    %s
                    %s
                    <section><h2>服务端动态日志</h2><ul>%s</ul></section>
                    <section><h2>命令与 ACK（含最近授权命令）</h2><ul>%s</ul></section>
                    <section><h2>原始 ESP32 遥测 JSON</h2><pre>%s</pre></section>
                    <section><h2>AI 决策 JSON</h2><pre>%s</pre></section>
                  </main>
                </body>
                </html>
                """.formatted(runSummary, onlineTrend, logs, commands, html(rawJson), html(decisionJson));
    }

    private String buildOnlineTrendSection() {
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofHours(ONLINE_TREND_HOURS));
        int[] scores = new int[ONLINE_TREND_HOURS + 1];
        List<IotDeviceTelemetryEventEntity> events = telemetryEventRepository
                .findAllByProjectAndReceivedAtAfterOrderByReceivedAtAscIdAsc(TELEMETRY_PROJECT, start);
        for (IotDeviceTelemetryEventEntity event : events) {
            Instant receivedAt = event.getReceivedAt();
            if (receivedAt == null || receivedAt.isBefore(start) || receivedAt.isAfter(now)) {
                continue;
            }
            long hoursSinceStart = Duration.between(start, receivedAt).toHours();
            int index = (int) Math.max(0, Math.min(ONLINE_TREND_HOURS, hoursSinceStart));
            scores[index] = Math.max(scores[index], calculateOnlineScore(event));
        }

        String latestLabel = events.isEmpty()
                ? "暂无近 24 小时遥测"
                : "近 24 小时遥测 " + events.size() + " 条，最高在线得分 " + maxScore(scores) + " / " + ONLINE_SCORE_MAX;
        return """
                <section class="trend-card">
                  <div class="trend-head">
                    <h2>设备在线趋势（近 24h）</h2>
                    <div class="trend-note"><span class="trend-score">%s</span><br>在线得分 = WiFi 10 分 + ATmega328 10 分 + 摄像头 10 分；每小时取最高值。</div>
                  </div>
                  %s
                </section>
                """.formatted(html(latestLabel), buildOnlineTrendSvg(start, scores));
    }

    private String buildOnlineTrendSvg(Instant start, int[] scores) {
        final int width = 820;
        final int height = 220;
        final int left = 48;
        final int right = 18;
        final int top = 18;
        final int bottom = 44;
        final int chartWidth = width - left - right;
        final int chartHeight = height - top - bottom;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"trend-svg\" viewBox=\"0 0 ")
                .append(width)
                .append(' ')
                .append(height)
                .append("\" role=\"img\" aria-label=\"设备在线趋势近24小时折线图\">");

        for (int tick = 0; tick <= ONLINE_SCORE_MAX; tick += 10) {
            double y = yForScore(tick, top, chartHeight);
            svg.append("<line class=\"trend-grid\" x1=\"").append(left).append("\" y1=\"").append(formatNumber(y))
                    .append("\" x2=\"").append(width - right).append("\" y2=\"").append(formatNumber(y)).append("\"/>");
            svg.append("<text class=\"trend-axis\" x=\"8\" y=\"").append(formatNumber(y + 4)).append("\">")
                    .append(tick)
                    .append("</text>");
        }

        StringBuilder points = new StringBuilder();
        for (int i = 0; i < scores.length; i++) {
            double x = left + (chartWidth * i / (double) (scores.length - 1));
            double y = yForScore(scores[i], top, chartHeight);
            if (!points.isEmpty()) {
                points.append(' ');
            }
            points.append(formatNumber(x)).append(',').append(formatNumber(y));
        }
        svg.append("<polyline class=\"trend-line\" points=\"").append(points).append("\"/>");

        for (int i = 0; i < scores.length; i++) {
            double x = left + (chartWidth * i / (double) (scores.length - 1));
            double y = yForScore(scores[i], top, chartHeight);
            svg.append("<circle class=\"trend-point\" cx=\"").append(formatNumber(x))
                    .append("\" cy=\"").append(formatNumber(y)).append("\" r=\"2.8\"/>");
        }

        for (int i = 0; i <= ONLINE_TREND_HOURS; i += 4) {
            double x = left + (chartWidth * i / (double) ONLINE_TREND_HOURS);
            String label = TREND_TIME_FORMATTER.format(start.plus(Duration.ofHours(i)));
            svg.append("<text class=\"trend-axis\" text-anchor=\"middle\" x=\"").append(formatNumber(x))
                    .append("\" y=\"").append(height - 16).append("\">")
                    .append(html(label))
                    .append("</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private int calculateOnlineScore(IotDeviceTelemetryEventEntity event) {
        int score = Boolean.TRUE.equals(event.getWifiConnected()) ? 10 : 0;
        JsonNode payload = parseJson(event.getPayloadJson());
        if (boolAt(payload, "atmega", "online")) {
            score += 10;
        }
        if (boolAt(payload, "vision", "online")) {
            score += 10;
        }
        return Math.min(score, ONLINE_SCORE_MAX);
    }

    private JsonNode parseJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private boolean boolAt(JsonNode root, String objectName, String fieldName) {
        JsonNode object = root == null ? null : root.get(objectName);
        JsonNode value = object == null ? null : object.get(fieldName);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText("").trim().toLowerCase();
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private int maxScore(int[] scores) {
        int max = 0;
        for (int score : scores) {
            max = Math.max(max, score);
        }
        return max;
    }

    private double yForScore(int score, int top, int chartHeight) {
        int bounded = Math.max(0, Math.min(ONLINE_SCORE_MAX, score));
        return top + (ONLINE_SCORE_MAX - bounded) * chartHeight / (double) ONLINE_SCORE_MAX;
    }

    private String formatNumber(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String formatTime(Instant value) {
        return value == null ? "" : DISPLAY_TIME_FORMATTER.format(value);
    }

    private String translateOwnerPresence(String value) {
        return switch (normalize(value)) {
            case "HOME" -> "主人在家";
            case "AWAY" -> "主人不在家";
            case "UNKNOWN" -> "摄像头未确认";
            default -> fallback(value);
        };
    }

    private String translateRiskLevel(String value) {
        return switch (normalize(value)) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            case "HIGH" -> "高风险";
            default -> fallback(value);
        };
    }

    private String translateStatus(String value) {
        return switch (normalize(value)) {
            case "ANALYZED" -> "已完成分析";
            case "COMMAND_QUEUED", "QUEUED" -> "等待 ESP32 轮询";
            case "DELIVERED" -> "已下发设备";
            case "ACKED" -> "设备已回执";
            case "VERIFIED" -> "闭环已验证";
            case "FAILED" -> "执行失败";
            case "BLOCKED" -> "高风险已拦截";
            case "MANUAL_CONFIRMED" -> "已授权执行";
            case "AUTHORIZED_COMMAND_QUEUED" -> "授权命令已入队";
            default -> fallback(value);
        };
    }

    private String translateStage(String value) {
        return switch (normalize(value)) {
            case "RECEIVED" -> "收到 ESP32 上报";
            case "PARSED" -> "解析家庭状态";
            case "ANALYZED" -> "AI 完成分析";
            case "PERMISSION_CHECKED" -> "权限与安全校验";
            case "COMMAND_QUEUED" -> "低风险命令入队";
            case "COMMAND_DELIVERED" -> "命令已下发";
            case "ACKED" -> "设备回执";
            case "CLOSED_LOOP_VERIFIED" -> "闭环验证通过";
            case "BLOCKED" -> "高风险安全拦截";
            case "MANUAL_CONFIRMED" -> "App 授权确认";
            case "AUTHORIZED_COMMAND_QUEUED" -> "授权命令入队";
            default -> fallback(value);
        };
    }

    private String translateCommandType(String value) {
        return switch (normalize(value)) {
            case "AUX_HINT_ON" -> "开启辅助灯提示";
            case "AUX_HINT_OFF" -> "关闭辅助灯提示";
            case "FAN_ON" -> "打开风扇";
            case "FAN_OFF" -> "关闭风扇";
            case "DOOR_GARAGE_CONTROL_BLOCKED" -> "门控类动作已拦截";
            case "DOOR_CLOSE" -> "关闭房门";
            case "GARAGE_CLOSE" -> "关闭车库门";
            default -> fallback(value);
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "未记录" : value;
    }

    private String prettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String html(Object value) {
        String text = value == null ? "" : value.toString();
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
