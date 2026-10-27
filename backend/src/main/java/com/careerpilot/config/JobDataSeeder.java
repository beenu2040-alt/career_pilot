package com.careerpilot.config;

import com.careerpilot.entity.Job;
import com.careerpilot.entity.Skill;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.JobSkillRepository;
import com.careerpilot.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Development-only seed mechanism for populating sample job listings and skills.
 * All sample vacancies are explicitly labeled and tagged with isSample=true.
 */
@Component
public class JobDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JobDataSeeder.class);

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final JobSkillRepository jobSkillRepository;

    @Value("${careerpilot.seed.enabled:true}")
    private boolean seedEnabled;

    public JobDataSeeder(
            JobRepository jobRepository,
            SkillRepository skillRepository,
            JobSkillRepository jobSkillRepository
    ) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.jobSkillRepository = jobSkillRepository;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Development job seeding is disabled via configuration.");
            return;
        }

        try {
            seedSampleDataIfEmpty();
        } catch (Exception ex) {
            log.warn("Skipping automatic development seed data: database is not reachable or uninitialized ({})", ex.getMessage());
        }
    }

    @Transactional
    public void seedSampleDataIfEmpty() {
        if (jobRepository.countByIsActiveTrue() > 0) {
            log.info("Database already contains active job listings. Skipping development seeder.");
            return;
        }

        log.info("Seeding development sample job listings and technical skills...");

        // 1. Seed Skills Catalog
        Map<String, Skill> skills = seedSkills();

        // 2. Seed Sample Jobs
        seedSampleJobs(skills);

        log.info("Successfully loaded development sample jobs and normalized skill relationships.");
    }

    private Map<String, Skill> seedSkills() {
        String[][] skillData = {
                {"Java", "Backend"},
                {"Spring Boot", "Backend"},
                {"TypeScript", "Frontend"},
                {"React", "Frontend"},
                {"PostgreSQL", "Database"},
                {"Tailwind CSS", "Frontend"},
                {"Docker", "DevOps"},
                {"AWS", "Cloud"},
                {"REST API Design", "Architecture"},
                {"System Architecture", "Architecture"},
                {"Python", "Backend"},
                {"Kubernetes", "DevOps"},
                {"Node.js", "Backend"},
                {"Go", "Backend"},
                {"GraphQL", "API"},
                {"Next.js", "Frontend"},
                {"Redis", "Database"},
                {"Linux", "Infrastructure"},
                {"CI/CD", "DevOps"},
                {"Microservices", "Architecture"}
        };

        Map<String, Skill> skillMap = new HashMap<>();
        for (String[] entry : skillData) {
            String name = entry[0];
            String category = entry[1];
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(new Skill(name, category)));
            skillMap.put(name.toLowerCase(), skill);
        }
        return skillMap;
    }

    private void seedSampleJobs(Map<String, Skill> skills) {
        Instant now = Instant.now();

        // Job 1: Senior Full-Stack Engineer
        Job job1 = new Job(
                "Senior Full-Stack Engineer",
                "Acme Cloud Systems [Sample]",
                "San Francisco, CA",
                "We are seeking a seasoned Senior Full-Stack Engineer to design and scale enterprise multi-tenant cloud platforms. You will work across distributed Java/Spring Boot microservices and responsive React/TypeScript frontends.",
                "FULL_TIME",
                "https://example.com/careers/sample-senior-fullstack"
        );
        job1.setRemoteType("REMOTE");
        job1.setExperienceLevel("SENIOR");
        job1.setSalaryMin(BigDecimal.valueOf(145000));
        job1.setSalaryMax(BigDecimal.valueOf(185000));
        job1.setSalaryCurrency("USD");
        job1.setPostedAt(now.minus(2, ChronoUnit.DAYS));
        job1.setActive(true);
        job1.setSample(true);
        jobRepository.save(job1);

        addSkill(job1, skills.get("java"), true);
        addSkill(job1, skills.get("spring boot"), true);
        addSkill(job1, skills.get("typescript"), true);
        addSkill(job1, skills.get("react"), true);
        addSkill(job1, skills.get("docker"), false);
        addSkill(job1, skills.get("aws"), false);

        // Job 2: Lead Platform Architect
        Job job2 = new Job(
                "Lead Platform Architect",
                "Nova AI Labs [Sample]",
                "San Francisco, CA",
                "Join Nova AI Labs to lead our core compute and orchestration platform. Responsible for scalable event-driven architectures, container mesh deployment on Kubernetes, and high-throughput data pipelines.",
                "FULL_TIME",
                "https://example.com/careers/sample-lead-platform-architect"
        );
        job2.setRemoteType("HYBRID");
        job2.setExperienceLevel("LEAD");
        job2.setSalaryMin(BigDecimal.valueOf(190000));
        job2.setSalaryMax(BigDecimal.valueOf(240000));
        job2.setSalaryCurrency("USD");
        job2.setPostedAt(now.minus(4, ChronoUnit.DAYS));
        job2.setActive(true);
        job2.setSample(true);
        jobRepository.save(job2);

        addSkill(job2, skills.get("system architecture"), true);
        addSkill(job2, skills.get("docker"), true);
        addSkill(job2, skills.get("aws"), true);
        addSkill(job2, skills.get("kubernetes"), true);
        addSkill(job2, skills.get("java"), false);
        addSkill(job2, skills.get("rest api design"), false);

        // Job 3: Frontend UI/UX Engineer
        Job job3 = new Job(
                "Frontend UI/UX Engineer",
                "PixelCraft Studio [Sample]",
                "New York, NY",
                "Craft delightful, accessible user experiences using modern React, TypeScript, and Tailwind CSS. Collaborate closely with product designers to ship responsive, high-performance web applications.",
                "FULL_TIME",
                "https://example.com/careers/sample-frontend-engineer"
        );
        job3.setRemoteType("REMOTE");
        job3.setExperienceLevel("MID_LEVEL");
        job3.setSalaryMin(BigDecimal.valueOf(110000));
        job3.setSalaryMax(BigDecimal.valueOf(140000));
        job3.setSalaryCurrency("USD");
        job3.setPostedAt(now.minus(1, ChronoUnit.DAYS));
        job3.setActive(true);
        job3.setSample(true);
        jobRepository.save(job3);

        addSkill(job3, skills.get("react"), true);
        addSkill(job3, skills.get("typescript"), true);
        addSkill(job3, skills.get("tailwind css"), true);
        addSkill(job3, skills.get("next.js"), false);
        addSkill(job3, skills.get("graphql"), false);

        // Job 4: Backend Systems Engineer
        Job job4 = new Job(
                "Backend Systems Engineer",
                "FinFlow Technologies [Sample]",
                "Austin, TX",
                "Develop transactional banking gateways, resilient REST APIs, and automated ledger services. Strong background in Java, Spring Boot, relational databases, and containerized Docker environments required.",
                "FULL_TIME",
                "https://example.com/careers/sample-backend-engineer"
        );
        job4.setRemoteType("HYBRID");
        job4.setExperienceLevel("SENIOR");
        job4.setSalaryMin(BigDecimal.valueOf(150000));
        job4.setSalaryMax(BigDecimal.valueOf(190000));
        job4.setSalaryCurrency("USD");
        job4.setPostedAt(now.minus(3, ChronoUnit.DAYS));
        job4.setActive(true);
        job4.setSample(true);
        jobRepository.save(job4);

        addSkill(job4, skills.get("java"), true);
        addSkill(job4, skills.get("spring boot"), true);
        addSkill(job4, skills.get("postgresql"), true);
        addSkill(job4, skills.get("docker"), true);
        addSkill(job4, skills.get("redis"), false);
        addSkill(job4, skills.get("rest api design"), false);

        // Job 5: Cloud DevOps & Reliability Lead
        Job job5 = new Job(
                "Cloud DevOps & Reliability Lead",
                "Apex Infrastructure [Sample]",
                "Seattle, WA",
                "Maintain 99.99% uptime for global microservices workloads. Expertise in Kubernetes cluster administration, Terraform, Linux systems, and comprehensive CI/CD deployment automation.",
                "FULL_TIME",
                "https://example.com/careers/sample-devops-lead"
        );
        job5.setRemoteType("ONSITE");
        job5.setExperienceLevel("LEAD");
        job5.setSalaryMin(BigDecimal.valueOf(160000));
        job5.setSalaryMax(BigDecimal.valueOf(210000));
        job5.setSalaryCurrency("USD");
        job5.setPostedAt(now.minus(5, ChronoUnit.DAYS));
        job5.setActive(true);
        job5.setSample(true);
        jobRepository.save(job5);

        addSkill(job5, skills.get("docker"), true);
        addSkill(job5, skills.get("kubernetes"), true);
        addSkill(job5, skills.get("aws"), true);
        addSkill(job5, skills.get("linux"), true);
        addSkill(job5, skills.get("ci/cd"), false);
        addSkill(job5, skills.get("system architecture"), false);

        // Job 6: Software Engineering Exploratory Intern (ZERO required skills to test edge cases)
        Job job6 = new Job(
                "Software Engineering Exploratory Intern",
                "InnovateX Labs [Sample]",
                "Remote",
                "An open-ended summer internship position for passionate technologists curious about software development, automated testing, and product experimentation. No rigid skill prerequisites required.",
                "INTERNSHIP",
                "https://example.com/careers/sample-internship"
        );
        job6.setRemoteType("REMOTE");
        job6.setExperienceLevel("ENTRY_LEVEL");
        job6.setSalaryMin(BigDecimal.valueOf(50000));
        job6.setSalaryMax(BigDecimal.valueOf(70000));
        job6.setSalaryCurrency("USD");
        job6.setPostedAt(now.minus(6, ChronoUnit.DAYS));
        job6.setActive(true);
        job6.setSample(true);
        jobRepository.save(job6);
    }

    private void addSkill(Job job, Skill skill, boolean isRequired) {
        if (skill != null) {
            job.addJobSkill(skill, isRequired);
            jobRepository.save(job);
        }
    }
}
