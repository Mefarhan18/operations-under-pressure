package com.newtonite.ops.common;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.Comment;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;
    private final WorkItemRepository workItemRepository;
    private final ActivityEventRepository activityEventRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(UserRepository userRepository,
                      TeamRepository teamRepository,
                      MembershipRepository membershipRepository,
                      WorkItemRepository workItemRepository,
                      ActivityEventRepository activityEventRepository,
                      CommentRepository commentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.workItemRepository = workItemRepository;
        this.activityEventRepository = activityEventRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("Database seeding is disabled (app.seed.enabled=false)");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Database already contains data ({} users found). Skipping seed.", userRepository.count());
            return;
        }

        log.info("Starting Newtonite Ops database seeding with secure hashed passwords...");

        // 1. Seed Demo Users with BCrypt hashed passwords
        String defaultHashedPassword = passwordEncoder.encode("Password123!");

        User admin = userRepository.save(new User(
                "Sarah Admin",
                "admin@newtonite.internal",
                defaultHashedPassword
        ));

        User lead = userRepository.save(new User(
                "David TechLead",
                "lead@newtonite.internal",
                defaultHashedPassword
        ));

        User operator = userRepository.save(new User(
                "Elena Operator",
                "ops@newtonite.internal",
                defaultHashedPassword
        ));

        User auditor = userRepository.save(new User(
                "Marcus Compliance",
                "auditor@newtonite.internal",
                defaultHashedPassword
        ));

        log.info("Seeded 4 demo users with BCrypt hashed credentials");

        // 2. Seed Teams
        Team platformTeam = teamRepository.save(new Team(
                "Core Platform Operations",
                "Infrastructure, site reliability and platform scalability under high transaction pressure."
        ));

        Team paymentTeam = teamRepository.save(new Team(
                "Payment & Financial Clearing",
                "Payment processing gateways, settlement reconciliations and fraud detection."
        ));

        Team complianceTeam = teamRepository.save(new Team(
                "Compliance & Regulatory Risk",
                "Compliance audits, regulatory oversight and data integrity guarantees."
        ));

        Team incidentTeam = teamRepository.save(new Team(
                "Customer Incident Response",
                "Tier 2 & 3 customer escalation triage, high-impact outage resolution."
        ));

        log.info("Seeded 4 operational teams");

        // 3. Seed Memberships with roles
        membershipRepository.save(new Membership(admin, platformTeam, MembershipRole.ADMIN));
        membershipRepository.save(new Membership(admin, paymentTeam, MembershipRole.ADMIN));
        membershipRepository.save(new Membership(lead, paymentTeam, MembershipRole.LEAD));
        membershipRepository.save(new Membership(lead, incidentTeam, MembershipRole.LEAD));
        membershipRepository.save(new Membership(operator, platformTeam, MembershipRole.MEMBER));
        membershipRepository.save(new Membership(operator, paymentTeam, MembershipRole.MEMBER));
        membershipRepository.save(new Membership(auditor, complianceTeam, MembershipRole.LEAD));
        membershipRepository.save(new Membership(auditor, paymentTeam, MembershipRole.VIEWER));

        log.info("Seeded team memberships with role assignments");

        // 4. Seed WorkItems across various types, priorities and statuses
        WorkItem item1 = workItemRepository.save(new WorkItem(
                "Production Payment Webhook Timeout under Peak Load",
                "Payment webhook handlers experiencing p99 latency spikes over 4500ms causing merchant timeout retries.",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                WorkItemStatus.IN_PROGRESS,
                paymentTeam,
                operator,
                admin,
                LocalDate.now().plusDays(1)
        ));

        WorkItem item2 = workItemRepository.save(new WorkItem(
                "Daily Settlement Reconciliation Discrepancy",
                "Clearing batch 8829 reported a $12,450 variance between acquiring ledger and core balance entries.",
                WorkItemType.PAYMENT,
                WorkItemPriority.HIGH,
                WorkItemStatus.AWAITING_APPROVAL,
                paymentTeam,
                lead,
                operator,
                LocalDate.now().plusDays(2)
        ));

        WorkItem item3 = workItemRepository.save(new WorkItem(
                "Q4 PCI-DSS Access Control Audit Preparation",
                "Collate privileged access logs and IAM rotation verification for external auditor review.",
                WorkItemType.COMPLIANCE,
                WorkItemPriority.MEDIUM,
                WorkItemStatus.OPEN,
                complianceTeam,
                auditor,
                auditor,
                LocalDate.now().plusDays(14)
        ));

        WorkItem item4 = workItemRepository.save(new WorkItem(
                "Merchant Portal Slow Transaction Export",
                "CSV exports exceeding 50,000 rows run into worker memory constraints during month-end closes.",
                WorkItemType.CUSTOMER_ISSUE,
                WorkItemPriority.MEDIUM,
                WorkItemStatus.OPEN,
                incidentTeam,
                operator,
                lead,
                LocalDate.now().plusDays(5)
        ));

        WorkItem item5 = workItemRepository.save(new WorkItem(
                "Upgrade Database Replica Connection Pool Metrics",
                "Add Micrometer gauges for HikariCP active vs idle connections on read replicas.",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                WorkItemStatus.RESOLVED,
                platformTeam,
                lead,
                admin,
                LocalDate.now().minusDays(1)
        ));

        log.info("Seeded 5 operational work items with optimistic locking enabled");

        // 5. Seed ActivityEvents
        activityEventRepository.save(new ActivityEvent(
                item1,
                admin,
                "STATUS_CHANGE",
                "OPEN",
                "IN_PROGRESS"
        ));
        activityEventRepository.save(new ActivityEvent(
                item1,
                admin,
                "ASSIGNED",
                null,
                operator.getName()
        ));
        activityEventRepository.save(new ActivityEvent(
                item2,
                operator,
                "STATUS_CHANGE",
                "IN_PROGRESS",
                "AWAITING_APPROVAL"
        ));

        // 6. Seed Comments
        commentRepository.save(new Comment(
                item1,
                operator,
                "Identified thread contention on the webhook dispatch queue. Deploying connection pool adjustments now."
        ));
        commentRepository.save(new Comment(
                item2,
                lead,
                "Variance isolated to 3 pending wire transfers from foreign banking partners. Approval pending dual sign-off."
        ));

        log.info("Database seeding successfully completed for Newtonite Ops Phase 1!");
    }
}
