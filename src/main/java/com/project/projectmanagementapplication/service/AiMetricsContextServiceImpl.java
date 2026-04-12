package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.ContextSection;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.DerivedSection;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.HintsSection;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.PrimarySection;
import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload.TrendsSection;
import com.project.projectmanagementapplication.dto.ai.CycleLeadByDayPoint;
import com.project.projectmanagementapplication.dto.ai.MetricsContextRequest;
import com.project.projectmanagementapplication.dto.ai.ThroughputByDayPoint;
import com.project.projectmanagementapplication.dto.ai.VelocityBySprintRow;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.MetricsTimeRange;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.RecentCompletionActivity;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.SprintRepository;
import com.project.projectmanagementapplication.service.metrics.KanbanMetricsWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AiMetricsContextServiceImpl implements AiMetricsContextService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59, 999_999_999);
    /** Minimum distinct calendar days with a completion in the trend window for {@code hints.enoughTrendData}. */
    private static final int MIN_COMPLETION_DAYS_FOR_TREND = 4;

    private final ProjectService projectService;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;

    public AiMetricsContextServiceImpl(
            ProjectService projectService,
            IssueRepository issueRepository,
            SprintRepository sprintRepository) {
        this.projectService = projectService;
        this.issueRepository = issueRepository;
        this.sprintRepository = sprintRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AiMetricsContextPayload buildContext(MetricsContextRequest request, User caller) {
        validateRequest(request);
        Project project = projectService.getProjectById(request.getProjectId()).getData();
        assertProjectMember(project, caller);
        if (project.getFramework() != request.getFramework()) {
            throw new BadRequestException(
                    "framework in request does not match project framework (project is " + project.getFramework() + ")");
        }
        return switch (request.getFramework()) {
            case KANBAN -> buildKanban(project, request.getTimeRange());
            case SCRUM -> buildScrum(project, request.getSprintId());
        };
    }

    private static void validateRequest(MetricsContextRequest request) {
        if (request.getProjectId() == null) {
            throw new BadRequestException("projectId is required");
        }
        if (request.getFramework() == null) {
            throw new BadRequestException("framework is required");
        }
        if (request.getFramework() == PROJECT_FRAMEWORK.KANBAN && request.getTimeRange() == null) {
            throw new BadRequestException("timeRange is required for KANBAN");
        }
        if (request.getFramework() == PROJECT_FRAMEWORK.SCRUM && request.getSprintId() == null) {
            throw new BadRequestException("sprintId is required for SCRUM");
        }
    }

    private AiMetricsContextPayload buildKanban(Project project, MetricsTimeRange timeRange) {
        List<Issue> issues = issueRepository.findByProjectId(project.getId());
        LocalDate today = LocalDate.now(ZONE);
        LocalDate rangeStart = KanbanMetricsWindow.rangeStart(timeRange, today);
        LocalDate rangeEnd = today;
        String scopeLabel = KanbanMetricsWindow.scopeLabel(timeRange);

        LocalDateTime windowStart = rangeStart.atStartOfDay();
        LocalDateTime windowEnd = LocalDateTime.of(rangeEnd, END_OF_DAY);

        List<Issue> completedInRange = issues.stream()
                .filter(i -> i.getTaskCompletedAt() != null)
                .filter(i -> !i.getTaskCompletedAt().isBefore(windowStart) && !i.getTaskCompletedAt().isAfter(windowEnd))
                .toList();

        int wip = (int) issues.stream().filter(i -> i.getStatus() == ISSUE_STATUS.IN_PROGRESS).count();

        Double avgCycle = avgCycleDays(completedInRange);
        Double avgLead = avgLeadDays(completedInRange);
        int throughput = completedInRange.size();

        List<LocalDate> days = enumerateDays(rangeStart, rangeEnd);
        List<CycleLeadByDayPoint> cycleLeadByDay = buildCycleLeadByDay(completedInRange, days);
        List<ThroughputByDayPoint> throughputByDay = buildThroughputByDay(completedInRange, days);

        DailyActivitySummary dailyActivity = summarizeDailyCompletions(cycleLeadByDay);
        DerivedSection derived = buildDerived(avgCycle, avgLead, null, null, null, dailyActivity);

        List<String> notes = new ArrayList<>(baseNotes());
        notes.add("Kanban throughput counts tasks completed in the selected date range.");
        if (completedInRange.size() < 3) {
            notes.add("Insights are based on limited recent completions in this range.");
        }

        HintsSection hints = HintsSection.builder()
                .enoughTrendData(hasEnoughCompletionDaysForTrend(dailyActivity))
                .notes(notes)
                .build();

        return AiMetricsContextPayload.builder()
                .context(ContextSection.builder()
                        .framework(PROJECT_FRAMEWORK.KANBAN.name())
                        .projectId(project.getId())
                        .projectName(project.getName())
                        .scopeLabel(scopeLabel)
                        .scopeStartDate(rangeStart)
                        .scopeEndDate(rangeEnd)
                        .generatedAt(java.time.Instant.now())
                        .build())
                .primary(PrimarySection.builder()
                        .wip(wip)
                        .avgCycleTimeDays(avgCycle)
                        .avgLeadTimeDays(avgLead)
                        .throughput(throughput)
                        .build())
                .trends(TrendsSection.builder()
                        .cycleLeadByDay(cycleLeadByDay)
                        .throughputByDay(throughputByDay)
                        .velocityBySprint(null)
                        .build())
                .derived(derived)
                .hints(hints)
                .build();
    }

    private AiMetricsContextPayload buildScrum(Project project, Long sprintId) {
        Sprint sprint = sprintRepository
                .findByIdAndProject_Id(sprintId, project.getId())
                .orElseThrow(() -> new BadRequestException("Sprint not found for this project"));

        List<Issue> issues = issueRepository.findByProjectId(project.getId());
        List<Issue> inSprint = issues.stream()
                .filter(i -> i.getSprint() != null && Objects.equals(i.getSprint().getId(), sprintId))
                .toList();

        int wip = (int) inSprint.stream().filter(i -> i.getStatus() == ISSUE_STATUS.IN_PROGRESS).count();
        int velocity = (int) inSprint.stream().filter(i -> i.getStatus() == ISSUE_STATUS.DONE).count();
        int totalInSprint = inSprint.size();
        Double completionRate = totalInSprint > 0 ? (velocity * 1.0 / totalInSprint) : null;

        List<Issue> completedInSprint = inSprint.stream()
                .filter(i -> i.getStatus() == ISSUE_STATUS.DONE)
                .filter(i -> i.getTaskCompletedAt() != null)
                .toList();

        Double avgCycle = avgCycleDays(completedInSprint);
        Double avgLead = avgLeadDays(completedInSprint);

        SprintBounds bounds = sprintChartDayBounds(sprint, completedInSprint);
        List<LocalDate> days = enumerateDays(bounds.start(), bounds.end());
        List<CycleLeadByDayPoint> cycleLeadByDay = buildCycleLeadByDay(completedInSprint, days);

        List<Sprint> velocitySprints = sprintRepository.findByProject_Id(project.getId()).stream()
                .filter(s -> s.getStatus() == SPRINT_STATUS.ACTIVE || s.getStatus() == SPRINT_STATUS.COMPLETED)
                .sorted(Comparator.comparing(Sprint::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Sprint::getId))
                .toList();

        List<VelocityBySprintRow> velocityRows = velocitySprints.stream()
                .map(s -> {
                    long sid = s.getId();
                    int cnt = (int) issues.stream()
                            .filter(i -> i.getStatus() == ISSUE_STATUS.DONE)
                            .filter(i -> i.getSprint() != null && Objects.equals(i.getSprint().getId(), sid))
                            .count();
                    return VelocityBySprintRow.builder()
                            .sprintId(sid)
                            .sprintName(Optional.ofNullable(s.getName()).orElse("Sprint " + sid))
                            .completedTasks(cnt)
                            .sprintStatus(s.getStatus())
                            .build();
                })
                .toList();

        DailyActivitySummary dailyActivity = summarizeDailyCompletions(cycleLeadByDay);
        DerivedSection derived = buildDerived(
                avgCycle, avgLead, velocity, totalInSprint, completionRate, dailyActivity);

        List<String> notes = new ArrayList<>(baseNotes());
        notes.add("Scrum velocity KPI counts DONE tasks in the selected sprint (not limited by completion date).");
        notes.add("Cycle/lead trends use DONE tasks in the sprint with valid taskCompletedAt, by completion day.");
        if (completedInSprint.size() < 3) {
            notes.add("Insights are based on limited completions in this sprint.");
        }

        HintsSection hints = HintsSection.builder()
                .enoughTrendData(hasEnoughCompletionDaysForTrend(dailyActivity))
                .notes(notes)
                .build();

        String sprintLabel = Optional.ofNullable(sprint.getName()).orElse("Sprint " + sprint.getId());

        return AiMetricsContextPayload.builder()
                .context(ContextSection.builder()
                        .framework(PROJECT_FRAMEWORK.SCRUM.name())
                        .projectId(project.getId())
                        .projectName(project.getName())
                        .scopeLabel(sprintLabel)
                        .scopeStartDate(bounds.start())
                        .scopeEndDate(bounds.end())
                        .generatedAt(java.time.Instant.now())
                        .build())
                .primary(PrimarySection.builder()
                        .wip(wip)
                        .avgCycleTimeDays(avgCycle)
                        .avgLeadTimeDays(avgLead)
                        .throughput(velocity)
                        .build())
                .trends(TrendsSection.builder()
                        .cycleLeadByDay(cycleLeadByDay)
                        .throughputByDay(null)
                        .velocityBySprint(velocityRows)
                        .build())
                .derived(derived)
                .hints(hints)
                .build();
    }

    private record SprintBounds(LocalDate start, LocalDate end) {}

    private record DailyActivitySummary(
            int completedDaysCount,
            int zeroCompletionDaysCount,
            RecentCompletionActivity recentCompletionActivity) {}

    /**
     * See class Javadoc for {@link RecentCompletionActivity} thresholds.
     */
    private static DailyActivitySummary summarizeDailyCompletions(List<CycleLeadByDayPoint> series) {
        int n = series.size();
        int d = (int) series.stream().filter(p -> p.getCompletedTasks() > 0).count();
        int zero = n - d;
        RecentCompletionActivity level;
        if (n == 0 || d == 0) {
            level = RecentCompletionActivity.SPARSE;
        } else {
            double r = d / (double) n;
            if (r < 0.30) {
                level = RecentCompletionActivity.SPARSE;
            } else if (r < 0.65) {
                level = RecentCompletionActivity.MODERATE;
            } else {
                level = RecentCompletionActivity.STEADY;
            }
        }
        return new DailyActivitySummary(d, zero, level);
    }

    /**
     * Mirrors {@code sprintChartDayBounds} in ScrumMetricsView.jsx.
     */
    private SprintBounds sprintChartDayBounds(Sprint sprint, List<Issue> completedInSprint) {
        LocalDate today = LocalDate.now(ZONE);

        LocalDate start;
        if (sprint.getStartDate() != null) {
            start = sprint.getStartDate();
        } else {
            Optional<LocalDate> minC = completedInSprint.stream()
                    .map(Issue::getTaskCompletedAt)
                    .filter(Objects::nonNull)
                    .map(LocalDateTime::toLocalDate)
                    .min(Comparator.naturalOrder());
            start = minC.orElse(today);
        }

        LocalDate endDate;
        if (sprint.getEndDate() != null) {
            LocalDate endFromSprint = sprint.getEndDate();
            if (sprint.getStatus() == SPRINT_STATUS.ACTIVE && endFromSprint.isAfter(today)) {
                endDate = today;
            } else {
                endDate = endFromSprint;
            }
        } else {
            endDate = today;
        }

        if (start.isAfter(endDate)) {
            LocalDate tmp = start;
            start = endDate;
            endDate = tmp;
        }
        return new SprintBounds(start, endDate);
    }

    private static List<LocalDate> enumerateDays(LocalDate start, LocalDate end) {
        List<LocalDate> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.add(d);
        }
        return out;
    }

    private static List<CycleLeadByDayPoint> buildCycleLeadByDay(List<Issue> completedInWindow, List<LocalDate> days) {
        record Agg(long cycleSum, int cycleCount, long leadSum, int leadCount) {
            Agg addCycle(long m) {
                return new Agg(cycleSum + m, cycleCount + 1, leadSum, leadCount);
            }

            Agg addLead(long m) {
                return new Agg(cycleSum, cycleCount, leadSum + m, leadCount + 1);
            }
        }
        Map<LocalDate, Integer> completionsByDay = new HashMap<>();
        Map<LocalDate, Agg> byDay = new HashMap<>();
        for (Issue i : completedInWindow) {
            LocalDateTime completedAt = i.getTaskCompletedAt();
            if (completedAt == null) {
                continue;
            }
            LocalDate day = completedAt.toLocalDate();
            completionsByDay.merge(day, 1, Integer::sum);
            Agg cur = byDay.getOrDefault(day, new Agg(0, 0, 0, 0));
            if (i.getTaskStartedAt() != null) {
                long mins = ChronoUnit.MINUTES.between(i.getTaskStartedAt(), completedAt);
                cur = cur.addCycle(mins);
            }
            if (i.getCreatedAt() != null) {
                long mins = ChronoUnit.MINUTES.between(i.getCreatedAt(), completedAt);
                cur = cur.addLead(mins);
            }
            byDay.put(day, cur);
        }

        List<CycleLeadByDayPoint> points = new ArrayList<>();
        for (LocalDate d : days) {
            int ct = completionsByDay.getOrDefault(d, 0);
            Agg a = byDay.get(d);
            if (ct == 0) {
                points.add(CycleLeadByDayPoint.builder()
                        .date(d)
                        .avgCycleTimeDays(null)
                        .avgLeadTimeDays(null)
                        .completedTasks(0)
                        .build());
            } else {
                points.add(CycleLeadByDayPoint.builder()
                        .date(d)
                        .avgCycleTimeDays(a != null && a.cycleCount > 0
                                ? roundDays(a.cycleSum / (double) a.cycleCount)
                                : null)
                        .avgLeadTimeDays(
                                a != null && a.leadCount > 0 ? roundDays(a.leadSum / (double) a.leadCount) : null)
                        .completedTasks(ct)
                        .build());
            }
        }
        return points;
    }

    private static List<ThroughputByDayPoint> buildThroughputByDay(List<Issue> completedInRange, List<LocalDate> days) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (Issue i : completedInRange) {
            LocalDate d = i.getTaskCompletedAt().toLocalDate();
            counts.merge(d, 1, Integer::sum);
        }
        List<ThroughputByDayPoint> out = new ArrayList<>();
        for (LocalDate d : days) {
            out.add(ThroughputByDayPoint.builder()
                    .date(d)
                    .completedTasks(counts.getOrDefault(d, 0))
                    .build());
        }
        return out;
    }

    private static Double avgCycleDays(List<Issue> completed) {
        List<Issue> eligible = completed.stream().filter(i -> i.getTaskStartedAt() != null).toList();
        if (eligible.isEmpty()) {
            return null;
        }
        long totalMins = 0;
        for (Issue i : eligible) {
            totalMins += ChronoUnit.MINUTES.between(i.getTaskStartedAt(), i.getTaskCompletedAt());
        }
        return roundDays(totalMins / (double) eligible.size());
    }

    private static Double avgLeadDays(List<Issue> completed) {
        List<Issue> eligible = completed.stream().filter(i -> i.getCreatedAt() != null).toList();
        if (eligible.isEmpty()) {
            return null;
        }
        long totalMins = 0;
        for (Issue i : eligible) {
            totalMins += ChronoUnit.MINUTES.between(i.getCreatedAt(), i.getTaskCompletedAt());
        }
        return roundDays(totalMins / (double) eligible.size());
    }

    private static double roundDays(double minutes) {
        double days = minutes / (60.0 * 24.0);
        return Math.round(days * 10.0) / 10.0;
    }

    private static DerivedSection buildDerived(
            Double avgCycle,
            Double avgLead,
            Integer sprintDone,
            Integer sprintTotal,
            Double completionRate,
            DailyActivitySummary dailyActivity) {
        return DerivedSection.builder()
                .avgWaitingTimeDays(waitingDays(avgCycle, avgLead))
                .leadVsCycleGapIsLarge(gapIsLarge(avgCycle, avgLead))
                .selectedSprintCompletedTasks(sprintDone)
                .selectedSprintTotalTasks(sprintTotal)
                .selectedSprintCompletionRate(completionRate)
                .completedDaysCount(dailyActivity.completedDaysCount())
                .zeroCompletionDaysCount(dailyActivity.zeroCompletionDaysCount())
                .recentCompletionActivity(dailyActivity.recentCompletionActivity())
                .build();
    }

    /** Waiting = lead minus cycle (both already in days), one decimal. */
    private static Double waitingDays(Double cycleDays, Double leadDays) {
        if (cycleDays == null || leadDays == null) {
            return null;
        }
        return Math.round((leadDays - cycleDays) * 10.0) / 10.0;
    }

    private static Boolean gapIsLarge(Double cycle, Double lead) {
        if (cycle == null || lead == null) {
            return null;
        }
        double wait = lead - cycle;
        double threshold = Math.max(1.0, 0.5 * cycle);
        return wait >= threshold;
    }

    /**
     * Drives {@code hints.enoughTrendData}: true when there are enough distinct calendar days with at least one
     * completion in the trend window to treat trend interpretation as reasonably grounded. Uses the same count as
     * {@code derived.completedDaysCount} ({@link DailyActivitySummary#completedDaysCount()}).
     */
    private static boolean hasEnoughCompletionDaysForTrend(DailyActivitySummary daily) {
        return daily.completedDaysCount() >= MIN_COMPLETION_DAYS_FOR_TREND;
    }

    private static List<String> baseNotes() {
        return new ArrayList<>(List.of(
                "Cycle time uses taskStartedAt to taskCompletedAt; tasks without taskStartedAt are excluded from cycle averages.",
                "Lead time uses Issue.createdAt (audit creation) to taskCompletedAt; tasks missing createdAt are excluded from lead averages."));
    }

    private static void assertProjectMember(Project project, User user) {
        if (project.getOwner().getId().equals(user.getId())) {
            return;
        }
        boolean inTeam = project.getTeam() != null
                && project.getTeam().stream().anyMatch(m -> m.getId().equals(user.getId()));
        if (!inTeam) {
            throw new UnauthorizedException("You are not a member of this project");
        }
    }
}
