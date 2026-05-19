package com.fpt.swp.service;

import com.fpt.swp.dto.TrendAnalysisDto;
import com.fpt.swp.dto.YearlyData;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TrendCalculator {

    public TrendAnalysisDto.TopicStatus classifyStatus(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return TrendAnalysisDto.TopicStatus.STABLE;

        double recentGrowth = calculateLatestGrowthRate(yearlyData);
        int recentVolume = sumRecentPapers(yearlyData, 2);
        double historicalAvg = averageHistoricalPapers(yearlyData, 5);

        if (recentVolume < 100 && recentGrowth > 0.5) return TrendAnalysisDto.TopicStatus.EMERGING;
        if (recentVolume > 500 && recentGrowth > 0.3) return TrendAnalysisDto.TopicStatus.HOT;
        if (recentVolume > 300 && Math.abs(recentGrowth) < 0.2) return TrendAnalysisDto.TopicStatus.MATURE;
        if (recentGrowth < -0.2) return TrendAnalysisDto.TopicStatus.DECLINING;
        return TrendAnalysisDto.TopicStatus.STABLE;
    }

    public String getStatusLabel(TrendAnalysisDto.TopicStatus status) {
        return switch (status) {
            case EMERGING -> "Emerging";
            case HOT -> "Hot";
            case STABLE -> "Stable";
            case MATURE -> "Mature";
            case DECLINING -> "Declining";
        };
    }

    public Double calculateLatestGrowthRate(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return 0.0;
        List<YearlyData> sorted = sortedValid(yearlyData);
        if (sorted.size() < 2) return 0.0;
        YearlyData latest = sorted.get(sorted.size() - 1);
        YearlyData previous = sorted.get(sorted.size() - 2);
        if (previous.getPaperCount() == null || previous.getPaperCount() == 0) return 0.0;
        return ((double) latest.getPaperCount() - previous.getPaperCount()) / previous.getPaperCount();
    }

    public Double calculateCumulativeGrowth(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return 0.0;
        List<YearlyData> valid = sortedValid(yearlyData);
        if (valid.isEmpty()) return 0.0;
        int first = valid.get(0).getPaperCount();
        int last = valid.get(valid.size() - 1).getPaperCount();
        if (first == 0) return last > 0 ? 1.0 : 0.0;
        return ((double) last - first) / first;
    }

    public Double[] calculateYoYGrowth(List<YearlyData> yearlyData) {
        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();
        Double[] yoy = new Double[valid.size()];
        for (int i = 1; i < valid.size(); i++) {
            int prev = valid.get(i - 1).getPaperCount() != null ? valid.get(i - 1).getPaperCount() : 0;
            int curr = valid.get(i).getPaperCount() != null ? valid.get(i).getPaperCount() : 0;
            yoy[i] = prev > 0 ? ((double) curr - prev) / prev : 0.0;
        }
        return yoy;
    }

    public Double calculateMomentum(List<YearlyData> yearlyData) {
        Double[] yoy = calculateYoYGrowth(yearlyData);
        if (yoy == null || yoy.length < 2) return 0.0;
        double[] weights = {0.1, 0.2, 0.3, 0.4};
        double momentum = 0.0;
        int count = 0;
        int weightIdx = 0;
        for (int i = yoy.length - 1; i >= Math.max(0, yoy.length - 4); i--) {
            if (yoy[i] != null) {
                momentum += yoy[i] * weights[Math.min(weightIdx, weights.length - 1)];
                count++;
                weightIdx++;
            }
        }
        return count > 0 ? momentum / count : 0.0;
    }

    public Double forecastNextYear(List<YearlyData> yearlyData, int lastYear) {
        List<YearlyData> valid = sortedValid(yearlyData);
        if (valid.size() < 3) return null;
        int n = valid.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (YearlyData yd : valid) {
            double x = yd.getYear();
            double y = yd.getPaperCount();
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 0.001) return null;
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        double forecast = slope * (lastYear + 1) + intercept;
        return forecast > 0 ? forecast : null;
    }

    public Double calculateForecastConfidence(List<YearlyData> yearlyData) {
        int n = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .toList().size();
        if (n < 4) return 0.3;
        if (n < 6) return 0.5;
        return 0.7;
    }

    public double calculateTrendScore(TrendAnalysisDto analysis) {
        double growth = analysis.getGrowthRate() != null ? analysis.getGrowthRate() : 0.0;
        double momentum = analysis.getMomentum() != null ? analysis.getMomentum() : 0.0;
        int recentPapers = sumRecentPapers(analysis.getYearlyData(), 2);
        return Math.max(-1, Math.min(growth, 5.0)) * 30
                + Math.max(-1, Math.min(momentum, 3.0)) * 20
                + Math.log1p(Math.min(recentPapers, 5000)) * 2;
    }

    public int sumRecentPapers(List<YearlyData> yearlyData, int years) {
        if (yearlyData == null || yearlyData.isEmpty()) return 0;
        return yearlyData.stream()
                .filter(y -> y.getYear() != null)
                .sorted(Comparator.comparing(YearlyData::getYear).reversed())
                .limit(years)
                .mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0)
                .sum();
    }

    public double averageHistoricalPapers(List<YearlyData> yearlyData, int years) {
        if (yearlyData == null || yearlyData.isEmpty()) return 0;
        List<YearlyData> sorted = yearlyData.stream()
                .filter(y -> y.getYear() != null)
                .sorted(Comparator.comparing(YearlyData::getYear).reversed())
                .toList();
        int count = Math.min(years, sorted.size());
        if (count == 0) return 0;
        return sorted.stream().limit(count)
                .mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0)
                .average().orElse(0.0);
    }

    public String generateInsight(String keyword, List<YearlyData> yearlyData,
                                  TrendAnalysisDto.TopicStatus status,
                                  Double growthRate, Double cumulativeGrowth, Double momentum) {
        if (yearlyData == null || yearlyData.isEmpty()) {
            return "No trend data available for '" + keyword + "' yet.";
        }
        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();
        if (valid.isEmpty()) return "No valid publication data found for '" + keyword + "'.";

        int total = valid.stream().mapToInt(y -> y.getPaperCount()).sum();
        int first = valid.get(0).getPaperCount();
        int firstYear = valid.get(0).getYear();
        int lastYear = valid.get(valid.size() - 1).getYear();

        StringBuilder sb = new StringBuilder();
        sb.append("The '").append(keyword).append("' field has accumulated ");
        sb.append(total > 1000 ? (total / 1000) + "K publications" : total + " publications");
        sb.append(" between ").append(firstYear).append("-").append(lastYear);

        if (growthRate != null && Math.abs(growthRate) > 0.05) {
            String dir = growthRate > 0 ? "increased" : "decreased";
            sb.append(", ").append(dir).append(" ").append(String.format("%.0f%%", Math.abs(growthRate) * 100));
            if (growthRate > 0 && growthRate > 0.3) sb.append(" year-over-year, showing strong momentum");
            else if (growthRate < 0 && growthRate < -0.2) sb.append(" year-over-year, indicating a declining trend");
        }
        if (momentum != null && momentum > 0.3) sb.append(". Momentum is accelerating");
        else if (momentum != null && momentum < -0.2) sb.append(". Momentum is decelerating");

        switch (status) {
            case EMERGING -> sb.append(". This is an emerging research area with high growth potential.");
            case HOT -> sb.append(". This topic is currently experiencing explosive growth.");
            case DECLINING -> sb.append(". Interest in this area has been declining recently.");
            case STABLE -> sb.append(". This remains a steady, consistent research area.");
            case MATURE -> sb.append(". This is a mature research field with established foundations.");
        }
        return sb.toString();
    }

    public String generateComparisonInsight(List<String> keywords, java.util.Map<String, List<YearlyData>> dataMap,
                                            String maxGrowthKeyword, double maxGrowth) {
        if (keywords == null || keywords.isEmpty()) return "";

        if (keywords.size() == 1) {
            String kw = keywords.get(0);
            Double growth = calculateLatestGrowthRate(dataMap.get(kw.toLowerCase()));
            if (growth != null && growth > 0.5) return "'" + kw + "' is experiencing explosive growth.";
            if (growth != null && growth < -0.2) return "'" + kw + "' is showing declining publication trends.";
            return "'" + kw + "' maintains a stable publication trajectory.";
        }

        if (keywords.size() == 2) {
            String kw1 = keywords.get(0), kw2 = keywords.get(1);
            Double g1 = calculateLatestGrowthRate(dataMap.get(kw1.toLowerCase()));
            Double g2 = calculateLatestGrowthRate(dataMap.get(kw2.toLowerCase()));
            if (g1 != null && g2 != null) {
                double diff = g1 - g2;
                if (diff > 0.3) return "'" + kw1 + "' publications are growing " + String.format("%.0f%%", diff * 100)
                        + " faster than '" + kw2 + "', suggesting a significant shift in research focus.";
                if (diff < -0.3) return "'" + kw2 + "' publications are growing " + String.format("%.0f%%", Math.abs(diff) * 100)
                        + " faster than '" + kw1 + "'.";
                return "'" + kw1 + "' and '" + kw2 + "' show similar growth trajectories.";
            }
        }

        return "'" + maxGrowthKeyword + "' leads with " + String.format("%.0f%%", maxGrowth * 100)
                + " growth, outpacing " + (keywords.size() - 1) + " other research areas.";
    }

    private List<YearlyData> sortedValid(List<YearlyData> yearlyData) {
        return yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();
    }
}
