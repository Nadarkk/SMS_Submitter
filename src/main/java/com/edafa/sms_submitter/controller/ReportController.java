package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.SmsStatsRowDto;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import com.edafa.sms_submitter.service.AccessService;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class ReportController {

    private final ReportService reportService;
    private final AccessService accessService;
    private final CompanyService companyService;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/sms-stats/select")
    public String showExportForm(@AuthenticationPrincipal User requester, Model model) {
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        model.addAttribute("isAdmin", isAdmin);
        if (isAdmin) {
            model.addAttribute("companies", companyService.getAllCompanies());
        }
        return "reports/export-select";
    }
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/sms-stats/export")
    public String exportSmsStats(
            @AuthenticationPrincipal User requester,
            @RequestParam(required = false) Integer targetCompanyId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) throws IOException {

        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please select both a start and end date.");
            redirectAttributes.addAttribute("targetCompanyId", targetCompanyId);
            return "redirect:/dashboard/sms-stats/select";
        }

        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date format.");
            redirectAttributes.addAttribute("targetCompanyId", targetCompanyId);
            return "redirect:/dashboard/sms-stats/select";
        }

        if (toDate.isBefore(fromDate)) {
            redirectAttributes.addFlashAttribute("error", "End date cannot be before start date.");
            redirectAttributes.addAttribute("targetCompanyId", targetCompanyId);
            return "redirect:/dashboard/sms-stats/select";
        }
        if (fromDate.isAfter(LocalDate.now()) || toDate.isAfter(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Dates cannot be in the future.");
            redirectAttributes.addAttribute("targetCompanyId", targetCompanyId);
            return "redirect:/dashboard/sms-stats/select";
        }

        Integer resolvedCompanyId;
        try {
            resolvedCompanyId = accessService.resolveCompanyId(requester, targetCompanyId);
        } catch (TargetCompanyRequiredException e) {
            model.addAttribute("redirectUrl",
                    "/dashboard/sms-stats/export?from=" + from + "&to=" + to);
            return "select-company";
        }

        List<SmsStatsRowDto> rows = reportService.getSmsStats(resolvedCompanyId, fromDate, toDate);

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"sms_stats_" + fromDate + "_to_" + toDate + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Date,Total Messages,Total Segments,Delivered,Failed,Delivery Rate (%)");
        for (SmsStatsRowDto r : rows) {
            writer.printf("%s,%d,%d,%d,%d,%.2f%n",
                    r.date(), r.totalMessages(), r.totalSegments(), r.delivered(), r.failed(), r.deliveryRatePercent());
        }
        writer.flush();

        return null;
    }
}