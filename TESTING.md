# AcxiomCRM System Verification & Acceptance Guide

This document outlines the end-to-end verification and testing procedures for the AcxiomCRM platform.

---

## 1. Environment & Default Credentials

| Role | Username | Password | Default Scope |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin@acxiomcrm.com` | `Admin@123` | Global (Full Access) |
| **Sales Manager** | `manager@acxiomcrm.com` | `Manager@123` | Management & Team Scope |
| **Sales Executive 1** | `sales1@acxiomcrm.com` | `Sales@123` | Assigned Records Only |
| **Sales Executive 2** | `sales2@acxiomcrm.com` | `Sales@123` | Assigned Records Only |

---

## 2. Verification Steps

### Step 1: Initial Bootstrap & Seed Data Inspection
- Start the application: `mvn spring-boot:run`
- Access `http://localhost:8080/login`
- Login as `admin@acxiomcrm.com` with `Admin@123`.
- Navigate to `/customers`, `/leads`, `/opportunities`, `/followups`.
- **Expected Outcome**: Pre-seeded dataset is visible (10 Customers, 15 Leads, 12 Opportunities, 15 Follow-ups, 10 Activities).

---

### Step 2: Authentication & Account Lockout
1. Navigate to `/login`.
2. Attempt login with `sales1@acxiomcrm.com` using incorrect password `WrongPass@1` five consecutive times.
3. On the 5th failed attempt, observe the message informing that the account is locked for 15 minutes.
4. Login as `admin@acxiomcrm.com`, navigate to `/users`.
5. Locate `sales1@acxiomcrm.com`, verify the status displays "Locked", and click "Unlock".
6. Navigate back to `/login` and sign in successfully with `sales1@acxiomcrm.com` / `Sales@123`.

---

### Step 3: User Registration & Password Complexity
1. Click "Register" on the login page (`/register`).
2. Test invalid passwords:
   - `short1!` (< 8 chars) -> Rejected
   - `nouppercase1!` -> Rejected
   - `NOLOWERCASE1!` -> Rejected
   - `NoDigitsSpecial!` -> Rejected
3. Enter valid password `Valid@2026Pass`, complete registration.
4. Verify user is registered with role `ROLE_SALES_EXECUTIVE` (role cannot be selected by registering user).

---

### Step 4: Record Scope & IDOR Protection
1. Log in as `sales1@acxiomcrm.com`.
2. Note the ID of a Customer owned by `sales1` (e.g. ID `1`).
3. Log out and log in as `sales2@acxiomcrm.com`.
4. Attempt to access `http://localhost:8080/customers/1` or edit `http://localhost:8080/customers/1/edit`.
5. **Expected Outcome**: HTTP 403 Access Denied or redirect to access-denied page.
6. Attempt REST API fetch: `GET /api/customers/1` with Bearer token of `sales2`.
7. **Expected Outcome**: HTTP 403 Forbidden with ProblemDetail JSON.

---

### Step 5: Customer Uniqueness & Validation
1. Log in as `sales1@acxiomcrm.com` and go to `/customers/new`.
2. Submit a customer with an already existing email:
   - **Expected**: Field validation error stating duplicate email.
3. Submit a customer with an invalid phone format (`12345`):
   - **Expected**: "Enter a valid phone number." (must match Indian mobile format `^[6-9]\d{9}$`).
4. Submit valid unique customer details.
5. **Expected Outcome**: Customer saved with sequential identifier (e.g. `CUST-0011`).

---

### Step 6: Lead Workflow State Engine
1. Navigate to `/leads/new` and create a lead in `NEW` status.
2. Attempt illegal transition directly from `NEW` to `QUALIFIED` or `CONVERTED`.
   - **Expected Outcome**: Transition rejected by workflow validator.
3. Transition `NEW` -> `CONTACTED`.
4. Transition `CONTACTED` -> `QUALIFIED`.
5. Notice that from `QUALIFIED`, the lead can be converted or marked `LOST`.

---

### Step 7: Lead Conversion
1. Open a lead in `QUALIFIED` status.
2. Click "Convert Lead".
3. **Expected Outcome**:
   - Customer account is created (or existing matched by email).
   - Opportunity is automatically created in `QUALIFICATION` stage with amount equal to lead expected value.
   - Lead status updates to `CONVERTED`.
   - Action is recorded in Audit Log.

---

### Step 8: Opportunity Constraints & Calculated Fields
1. Navigate to `/opportunities/new`.
2. Test validations:
   - Amount `<= 0`: "Opportunity Amount must be greater than 0."
   - Probability `< 0` or `> 100`: "Probability must be between 0 and 100."
   - Past Expected Close Date for active deal: "Expected Close Date cannot be in the past."
3. Enter Amount `100000.00` and Probability `40`.
4. Save and inspect detail view:
   - **Expected Outcome**: Weighted value automatically calculates to `40000.00`. Status displays `OPEN`.
5. Update stage to `WON`:
   - Status updates to `WON`, probability sets to `100%`.

---

### Step 9: Pipeline Kanban & Stage Tracking
1. Navigate to `/opportunities/pipeline`.
2. View opportunities sorted into stage columns (Qualification, Proposal, Negotiation, Won, Lost).
3. Observe column totals and weighted revenue pipeline metrics.

---

### Step 10: Follow-up Scheduling & Overdue Batch Job
1. Navigate to `/followups/new`.
2. Select yesterday's date for a new follow-up:
   - **Expected**: "Follow-up date cannot be earlier than today."
3. Set a valid future date and submit.
4. In the list, click "Complete" on a scheduled follow-up and enter completion remarks.
5. Scheduled cron job verification:
   - Run unit test `FollowUpRulesTest.testMarkOverdueAsMissed` or trigger daily cron to verify overdue planned items transition to `MISSED`.

---

### Step 11: Activity Logging
1. Navigate to `/activities/new`.
2. Create an activity of type `CALL`, `MEETING`, `EMAIL`, or `TASK` associated with a Customer or Lead.
3. Confirm activity appears in list, customer details timeline, and user activity report.

---

### Step 12: Reporting Engine & CSV Exports
1. Log in as `manager@acxiomcrm.com` or `admin@acxiomcrm.com`.
2. Navigate to `/reports`.
3. Open each report:
   - Customer Report
   - Lead Status Report
   - Opportunity Pipeline Report
   - Follow-up Summary Report
   - Lead Conversion Report
   - User Activity Report
   - Audit Trail Report
4. Click "Export to CSV" on each report and verify that properly formatted CSV files download with expected headers and data.

---

### Step 13: REST API Authentication & Rate Limiting
1. Send `POST /api/auth/login` with valid JSON payload:
   ```json
   {
     "email": "sales1@acxiomcrm.com",
     "password": "Sales@123"
   }
   ```
   - **Expected Outcome**: HTTP 200 with JWT `accessToken`.
2. Use the token in header `Authorization: Bearer <token>` to request `GET /api/customers`.
   - **Expected Outcome**: HTTP 200 with scoped customer list.
3. Test rate limiting: Send repeated login attempts in rapid succession.
   - **Expected Outcome**: HTTP 429 Too Many Requests when rate limit threshold is exceeded.

---

### Step 14: Audit Trail Immutability & Coverage
1. Log in as `admin@acxiomcrm.com` and open `/audit`.
2. Verify system logs contain entries for:
   - `LOGIN_SUCCESS`, `LOGIN_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`
   - `CREATE`, `UPDATE`, `DELETE` operations
   - Lead conversions (`CONVERT_LEAD`)
   - Follow-up completions (`COMPLETE_FOLLOWUP`) and reschedules (`RESCHEDULE_FOLLOWUP`)
3. Verify audit log records are read-only and no update/delete UI or endpoints exist.
