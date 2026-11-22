package com.acxiom.crm.config;

import com.acxiom.crm.entity.*;
import com.acxiom.crm.enums.*;
import com.acxiom.crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final FollowUpRepository followUpRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Seeding initial database data...");

        // 1. Users
        User admin = User.builder()
                .fullName("System Administrator")
                .email("admin@acxiomcrm.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .failedLoginCount(0)
                .build();
        admin = userRepository.save(admin);

        User manager = User.builder()
                .fullName("Sales Manager")
                .email("manager@acxiomcrm.com")
                .passwordHash(passwordEncoder.encode("Manager@123"))
                .role(Role.ROLE_MANAGER)
                .active(true)
                .failedLoginCount(0)
                .build();
        manager = userRepository.save(manager);

        User sales1 = User.builder()
                .fullName("Sales Executive One")
                .email("sales1@acxiomcrm.com")
                .passwordHash(passwordEncoder.encode("Sales@123"))
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .failedLoginCount(0)
                .build();
        sales1 = userRepository.save(sales1);

        User sales2 = User.builder()
                .fullName("Sales Executive Two")
                .email("sales2@acxiomcrm.com")
                .passwordHash(passwordEncoder.encode("Sales@123"))
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .failedLoginCount(0)
                .build();
        sales2 = userRepository.save(sales2);

        // 2. Customers (10 customers, 5 for sales1, 5 for sales2)
        List<Customer> customerList = new ArrayList<>();
        String[][] customerData = {
                {"Apex Global", "contact@apexglobal.com", "9876543210", "Apex Corp", "101 Tech Park, Bangalore"},
                {"BlueWave Solutions", "info@bluewave.com", "9876543211", "BlueWave Ltd", "202 Marina Bay, Mumbai"},
                {"CyberTech Inc", "sales@cybertech.com", "9876543212", "CyberTech Systems", "303 Cyber City, Gurgaon"},
                {"Delta Dynamics", "office@deltadynamics.com", "9876543213", "Delta Group", "404 Ring Road, Hyderabad"},
                {"Echo Enterprises", "support@echoent.com", "9876543214", "Echo Logistics", "505 Port Trust, Chennai"},
                {"Fusion Systems", "fusion@fusion.com", "9876543215", "Fusion Hub", "606 MG Road, Pune"},
                {"Global Reach", "hello@globalreach.com", "9876543216", "Global Reach Media", "707 Connaught Place, Delhi"},
                {"Horizon Tech", "info@horizon.com", "9876543217", "Horizon Cloud", "808 Salt Lake, Kolkata"},
                {"Infinity Labs", "contact@infinity.com", "9876543218", "Infinity AI", "909 Indiranagar, Bangalore"},
                {"Jupiter Media", "sales@jupitermedia.com", "9876543219", "Jupiter Networks", "1001 Bandra West, Mumbai"}
        };

        for (int i = 0; i < customerData.length; i++) {
            User assigned = (i < 5) ? sales1 : sales2;
            Customer c = Customer.builder()
                    .customerCode(String.format("CUST-%04d", i + 1))
                    .name(customerData[i][0])
                    .email(customerData[i][1])
                    .phone(customerData[i][2])
                    .company(customerData[i][3])
                    .address(customerData[i][4])
                    .notes("Key enterprise client account #" + (i + 1))
                    .active(true)
                    .assignedTo(assigned)
                    .createdBy(admin)
                    .build();
            customerList.add(customerRepository.save(c));
        }

        // 3. Leads (15 leads across all statuses)
        LeadStatus[] statuses = {
                LeadStatus.NEW, LeadStatus.CONTACTED, LeadStatus.QUALIFIED,
                LeadStatus.UNQUALIFIED, LeadStatus.CONVERTED, LeadStatus.LOST,
                LeadStatus.NEW, LeadStatus.CONTACTED, LeadStatus.QUALIFIED,
                LeadStatus.CONVERTED, LeadStatus.LOST, LeadStatus.NEW,
                LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.UNQUALIFIED
        };

        List<Lead> leadList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            User assigned = (i % 2 == 0) ? sales1 : sales2;
            Customer linkedCust = (i < customerList.size()) ? customerList.get(i % customerList.size()) : null;
            Lead l = Lead.builder()
                    .leadCode(String.format("LEAD-%04d", i + 1))
                    .name("Lead Candidate " + (i + 1))
                    .company("Company " + (char) ('A' + i))
                    .email("lead" + (i + 1) + "@example.com")
                    .phone("98123450" + String.format("%02d", i))
                    .status(statuses[i])
                    .source(i % 3 == 0 ? "Web Search" : (i % 3 == 1 ? "Referral" : "Cold Call"))
                    .notes("Initial enquiry for cloud CRM integration - " + statuses[i])
                    .expectedValue(BigDecimal.valueOf(5000 + (i * 2500)))
                    .expectedCloseDate(LocalDate.now().plusDays(15 + i * 5))
                    .customer(linkedCust)
                    .assignedTo(assigned)
                    .createdBy(assigned)
                    .build();
            leadList.add(leadRepository.save(l));
        }

        // 4. Opportunities (12 opportunities across all stages)
        OpportunityStage[] stages = {
                OpportunityStage.QUALIFICATION, OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION,
                OpportunityStage.WON, OpportunityStage.LOST, OpportunityStage.QUALIFICATION,
                OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION, OpportunityStage.WON,
                OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION, OpportunityStage.WON
        };

        int[] probabilities = {20, 50, 75, 100, 0, 25, 60, 80, 100, 45, 85, 100};
        double[] amounts = {15000, 32000, 48000, 75000, 12000, 22000, 41000, 55000, 95000, 30000, 62000, 120000};

        List<Opportunity> oppList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            User assigned = (i % 2 == 0) ? sales1 : sales2;
            Customer cust = customerList.get(i % customerList.size());
            Lead ld = leadList.get(i % leadList.size());

            Opportunity opp = Opportunity.builder()
                    .name("Deal with " + cust.getName() + " #" + (i + 1))
                    .customer(cust)
                    .lead(ld)
                    .stage(stages[i])
                    .amount(BigDecimal.valueOf(amounts[i]))
                    .probability(probabilities[i])
                    .expectedCloseDate(LocalDate.now().plusDays(10 + i * 7))
                    .description("Sales opportunity for CRM annual subscription and onboarding.")
                    .assignedTo(assigned)
                    .createdBy(assigned)
                    .build();
            opp.deriveFields();
            oppList.add(opportunityRepository.save(opp));
        }

        // 5. Follow-Ups (15 follow-ups: PLANNED, COMPLETED, MISSED, CANCELLED)
        FollowUpStatus[] fuStatuses = {
                FollowUpStatus.PLANNED, FollowUpStatus.PLANNED, FollowUpStatus.PLANNED,
                FollowUpStatus.COMPLETED, FollowUpStatus.COMPLETED, FollowUpStatus.MISSED,
                FollowUpStatus.MISSED, FollowUpStatus.CANCELLED, FollowUpStatus.PLANNED,
                FollowUpStatus.PLANNED, FollowUpStatus.COMPLETED, FollowUpStatus.PLANNED,
                FollowUpStatus.MISSED, FollowUpStatus.COMPLETED, FollowUpStatus.PLANNED
        };
        FollowUpType[] fuTypes = {
                FollowUpType.CALL, FollowUpType.MEETING, FollowUpType.EMAIL,
                FollowUpType.CALL, FollowUpType.MEETING, FollowUpType.EMAIL,
                FollowUpType.TASK, FollowUpType.CALL, FollowUpType.MEETING,
                FollowUpType.EMAIL, FollowUpType.TASK, FollowUpType.CALL,
                FollowUpType.MEETING, FollowUpType.EMAIL, FollowUpType.TASK
        };

        for (int i = 0; i < 15; i++) {
            User assigned = (i % 2 == 0) ? sales1 : sales2;
            Customer cust = customerList.get(i % customerList.size());
            Lead ld = leadList.get(i % leadList.size());

            LocalDate fuDate = switch (fuStatuses[i]) {
                case MISSED -> LocalDate.now().minusDays(2 + i);
                case COMPLETED -> LocalDate.now().minusDays(1 + i);
                default -> LocalDate.now().plusDays(1 + i);
            };

            FollowUp fu = FollowUp.builder()
                    .followUpDate(fuDate)
                    .type(fuTypes[i])
                    .remarks("Discuss requirement details and scope with stakeholder #" + (i + 1))
                    .status(fuStatuses[i])
                    .customer(cust)
                    .lead(ld)
                    .assignedTo(assigned)
                    .createdBy(assigned)
                    .completionNotes(fuStatuses[i] == FollowUpStatus.COMPLETED ? "Completed satisfactorily with client notes." : null)
                    .build();
            followUpRepository.save(fu);
        }

        // 6. Activities (10 activities: CALL, MEETING, EMAIL, TASK)
        ActivityType[] actTypes = {
                ActivityType.CALL, ActivityType.MEETING, ActivityType.EMAIL, ActivityType.TASK,
                ActivityType.CALL, ActivityType.MEETING, ActivityType.EMAIL, ActivityType.TASK,
                ActivityType.CALL, ActivityType.MEETING
        };

        for (int i = 0; i < 10; i++) {
            User assigned = (i % 2 == 0) ? sales1 : sales2;
            Customer cust = customerList.get(i % customerList.size());

            Activity act = Activity.builder()
                    .type(actTypes[i])
                    .subject("Quarterly Account Check-in - " + cust.getName())
                    .description("Reviewed current usage, answered questions, and discussed new features.")
                    .activityDate(LocalDateTime.now().minusDays(i))
                    .status(i % 3 == 0 ? ActivityStatus.COMPLETED : ActivityStatus.PENDING)
                    .customer(cust)
                    .assignedTo(assigned)
                    .createdBy(assigned)
                    .build();
            activityRepository.save(act);
        }

        log.info("Database seeding successfully completed with 4 users, 10 customers, 15 leads, 12 opportunities, 15 follow-ups, and 10 activities.");
    }
}
