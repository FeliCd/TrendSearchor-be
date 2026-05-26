package com.fpt.swp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.swp.dto.CreateReportRequest;
import com.fpt.swp.dto.ReportResponseDto;
import com.fpt.swp.dto.TrendAnalysisDto;
import com.fpt.swp.dto.TrendDataDto;
import jakarta.persistence.EntityNotFoundException;
import com.fpt.swp.model.DashboardReport;
import com.fpt.swp.model.ReportType;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.DashboardReportRepository;
import com.fpt.swp.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final DashboardReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TrendAnalysisService trendAnalysisService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReportResponseDto createReport(Long userId, CreateReportRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Map<String, Object> content = new HashMap<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("keyword", request.getKeyword());
        parameters.put("startYear", request.getStartYear());
        parameters.put("endYear", request.getEndYear());

        String title = "Report: " + request.getKeyword();

        if (request.getReportType() == ReportType.TREND_SUMMARY) {
            TrendAnalysisDto analysis = trendAnalysisService.analyzeKeyword(
                    request.getKeyword(), request.getStartYear(), request.getEndYear());
            
            // Convert analysis to Map
            try {
                String json = objectMapper.writeValueAsString(analysis);
                content = objectMapper.readValue(json, Map.class);
            } catch (Exception e) {
                log.error("Error converting analysis to map", e);
            }
            title = "Trend Analysis Report: " + request.getKeyword();
        }

        DashboardReport report = DashboardReport.builder()
                .user(user)
                .reportType(request.getReportType())
                .title(title)
                .parameters(parameters)
                .content(content)
                .build();

        DashboardReport savedReport = reportRepository.save(report);
        return mapToDto(savedReport);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getUserReports(Long userId, int page, int size) {
        Page<DashboardReport> reports = reportRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return reports.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ReportResponseDto getReportById(Long userId, Long id) {
        DashboardReport report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        if (!report.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Report not found for this user");
        }

        return mapToDto(report);
    }

    @Transactional(readOnly = true)
    public byte[] generateCsv(Long userId, Long id) {
        ReportResponseDto report = getReportById(userId, id);
        
        StringBuilder csv = new StringBuilder();
        
        if (report.getReportType() == ReportType.TREND_SUMMARY) {
            csv.append("Year,Paper Count,Citation Count,Average Citations\n");
            try {
                Map<String, Object> content = report.getContent();
                if (content != null && content.containsKey("yearlyData")) {
                    List<Map<String, Object>> trendData = (List<Map<String, Object>>) content.get("yearlyData");
                    for (Map<String, Object> data : trendData) {
                        csv.append(data.get("year")).append(",")
                           .append(data.get("paperCount")).append(",")
                           .append(data.get("citationCount")).append(",")
                           .append(data.get("avgCitations")).append("\n");
                    }
                }
            } catch (Exception e) {
                log.error("Error generating CSV", e);
            }
        }
        
        return csv.toString().getBytes();
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long userId, Long id) {
        ReportResponseDto report = getReportById(userId, id);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph(report.getTitle(), titleFont);
            title.setSpacingAfter(20);
            document.add(title);

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Report Type: " + report.getReportType(), bodyFont));
            document.add(new Paragraph("Created At: " + report.getCreatedAt(), bodyFont));
            document.add(new Paragraph("Parameters: " + report.getParameters(), bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            if (report.getReportType() == ReportType.TREND_SUMMARY) {
                Map<String, Object> content = report.getContent();
                if (content != null) {
                    if (content.containsKey("status")) {
                        document.add(new Paragraph("Status: " + content.get("status"), bodyFont));
                    }
                    if (content.containsKey("insight")) {
                        document.add(new Paragraph("Insight: " + content.get("insight"), bodyFont));
                        document.add(new Paragraph(" ", bodyFont));
                    }
                    
                    if (content.containsKey("yearlyData")) {
                        PdfPTable table = new PdfPTable(4);
                        table.addCell("Year");
                        table.addCell("Paper Count");
                        table.addCell("Citation Count");
                        table.addCell("Avg Citations");

                        List<Map<String, Object>> trendData = (List<Map<String, Object>>) content.get("yearlyData");
                        for (Map<String, Object> data : trendData) {
                            table.addCell(String.valueOf(data.get("year")));
                            table.addCell(String.valueOf(data.get("paperCount")));
                            table.addCell(String.valueOf(data.get("citationCount")));
                            table.addCell(String.valueOf(data.get("avgCitations")));
                        }
                        document.add(table);
                    }
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private ReportResponseDto mapToDto(DashboardReport report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .title(report.getTitle())
                .content(report.getContent())
                .parameters(report.getParameters())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
