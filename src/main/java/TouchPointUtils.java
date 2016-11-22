package src.main.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sun.jersey.api.client.ClientResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;

public class TouchPointUtils {
    private static final Logger log = Logger.getLogger(TouchPointUtils.class.getName());

    public enum SD211Programs {
        intake(791),
        calFreshEnrollment(793),
        calFreshDenial(794),
        calFreshApproval(795),
        comboEnrollment(796),
        comboDenial(797),
        comboApproval(798),
        mediCalEnrollment(799),
        mediCalDenial(800),
        mediCalApproval(801),
        healthGeneral(802),
        perinatal(803),
        projectCare(804),
        sharpReferrals(805),
        riskRatingScale(806);

        int value;
        SD211Programs(int v) {
            this.value = v;
        }
        public int getValue() {
            return this.value;
        }
    }

    private static int getProgramId(String appType, String appStatus) {
        int programId = 0;
        if (appType == null || appType.length() <= 0) {
            log.error("Trying to get 211 SD program ID without application type");
            return programId;
        }

        String status = "";
        String aType = appType.toLowerCase();
        switch (aType) {
            case "calfresh":
                if (appStatus == null || appStatus.length() <= 0) {
                    log.error("Trying to get CalFresh program ID without application status");
                    return programId;
                }

                status = appStatus.toLowerCase();
                switch (status) {
                    case "pending":
                        programId = SD211Programs.calFreshEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.calFreshDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.calFreshApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus +
                                  ", application type: CalFresh");
                        break;
                }
                break;
            case "combo":
                if (appStatus == null || appStatus.length() <= 0) {
                    log.error("Trying to get Combo program ID without application status");
                    return programId;
                }

                status = appStatus.toLowerCase();
                switch (status) {
                    case "pending":
                        programId = SD211Programs.comboEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.comboDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.comboApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus +
                                  ", application type: Combo");
                        break;
                }
                break;
            case "medi-cal":
                if (appStatus == null || appStatus.length() <= 0) {
                    log.error("Trying to get Medi-Cal program ID without application status");
                    return programId;
                }

                status = appStatus.toLowerCase();
                switch (status) {
                    case "pending":
                        programId = SD211Programs.mediCalEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.mediCalDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.mediCalApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus +
                                  ", application type: Medi-Cal");
                        break;
                }
                break;
            case "health general":
                programId = SD211Programs.healthGeneral.getValue();
                break;
            case "perinatal":
                programId = SD211Programs.perinatal.getValue();
                break;
            case "project care":
                programId = SD211Programs.projectCare.getValue();
                break;
            case "sharp referrals":
                programId = SD211Programs.sharpReferrals.getValue();
                break;
            case "risk rating":
                programId = SD211Programs.riskRatingScale.getValue();
                break;
            default:
                log.error("Invalid application type: " + appType);
                break;
        }
        return programId;
    }

    public static void addConsentFile(EtoAuthentication auth, Long subjectId,
                                      DbClient data, String consentFileName) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(data.consentDate);
        Date dt = cal.getTime();
        int intakePrgmId = SD211Programs.intake.getValue();

        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(14)); // CIE Client Authorization TP.
        input.put("SubjectID", subjectId);
        input.put("ResponseCreatedDate", "/Date(" + dt.getTime() + ")/");
        input.put("ProgramID", new Integer(intakePrgmId));

        JSONArray respElements = new JSONArray();
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(137)); // Date authorization recorded.
        ele.put("ElementType", new Integer(9));
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        ele = new JSONObject();
        ele.put("ElementID", new Integer(165)); // Date authorization expired.
        ele.put("ElementType", new Integer(9));
        cal.add(Calendar.YEAR, 3);
        dt = cal.getTime();
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        // File attachments.
        Path path = Paths.get(consentFileName);
        byte[] fileData = Files.readAllBytes(path);
        JSONObject f = new JSONObject();
        f.put("Caption", null);
        JSONArray fileArr = new JSONArray();
        for (int k = 0; k < fileData.length; k++) {
            int n = fileData[k] & 0xff; // Since bytes are signed in Java.
            fileArr.add(n);
        }
        f.put("FileContent", fileArr);
        f.put("FileName", data.firstName + "_" + data.lastName + "_Authorization.pdf");

        ele = new JSONObject();
        ele.put("ElementID", new Integer(138)); // Authorization attachment.
        ele.put("ElementType", new Integer(29));
        ele.put("ResponseFileAttachment", f);
        ele.put("Value", null);
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            String resp = response.getEntity(String.class);
            log.info("Client Authorization - Response from server:");
            log.info(resp);
        }
    }

    public static void addIntakeProgram(Connection sqlConn, EtoAuthentication auth,
                                        DbClient data) throws Exception {
        JSONObject input = new JSONObject();

        int intakePrgmId = SD211Programs.intake.getValue();
        log.info("Adding new intake program ID: " + intakePrgmId);
        input.put("participantID", data.etoParticipantSiteId);
        input.put("programID", new Integer(intakePrgmId));
        long prgmStartDate = 0;
        Date now = new Date();
        if (data.consentDate != null) {
            prgmStartDate = data.consentDate.getTime();
        }
        else {
            prgmStartDate = now.getTime();
        }
        input.put("startDate", "/Date(" + prgmStartDate + ")/");
        input.put("projectedEndDate", null);

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                             auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long enrollmentId = EtoServiceUtil.parseResponse(response, "SaveParticipantEnrollmentResult", "ID");
            log.info("Client enrollment ID: " + enrollmentId);

            // Find matching CIE program using ETO's program id.
            DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, intakePrgmId);
            if (prgm == null) {
                log.error("CIE Program not found while trying to add new intake program");
                return;
            }

            // Save enrollment info.
            log.info("DbEnrollment.insert()");
            DbEnrollment enroll = new DbEnrollment();
            enroll.clientId = data.id;
            enroll.programId = prgm.id;
            if (data.consentDate != null) {
                enroll.startDate = data.consentDate;
            }
            else {
                enroll.startDate = now;
            }
            enroll.endDate = null;
            enroll.dismissalReasonId = 1; // Completed.
            enroll.insert(sqlConn);
        }
    }

    public static DbEnrollment addProgram(Connection sqlConn, EtoAuthentication auth,
                                          long clientId, long participantId,
                                          SfProgramInfo data, String appStatus) throws Exception {
        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add new enrollment");
            return null;
        }

        // Find matching CIE program using ETO's program id.
        DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (prgm == null) {
            log.error("CIE program not found while trying to add new enrollment");
            return null;
        }

        // Check to see if enrollment for this program already existed.
        DbEnrollment enroll = DbEnrollment.findByProgram(sqlConn, clientId, prgm.id, data.appDate);
        if (enroll != null) {
            // Check to see if client has been dismissed from existing enrollment.
            if (enroll.endDate != null) {
                log.info("Client has been dismissed from ETO program id: " + programId);
            }
            else {
                log.info("Client has been enrolled in ETO program id:" + programId);
            }
            return null;
        }

        // Add new program enrollment.
        log.info("Adding new program (Client ID: " + clientId + ", ETO program ID: " + programId);
        JSONObject input = new JSONObject();
        input.put("participantID", participantId);
        input.put("programID", new Integer(programId));
        long prgmStartDate = data.appDate.getTime();
        input.put("startDate", "/Date(" + prgmStartDate + ")/");

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                             auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
            return null;
        }

        // Parse response.
        Long enrollmentId = EtoServiceUtil.parseResponse(response, "SaveParticipantEnrollmentResult", "ID");
        log.info("Client enrollment ID: " + enrollmentId);

        // Save enrollment info.
        log.info("DbEnrollment.insert()");
        enroll = new DbEnrollment();
        enroll.clientId = clientId;
        enroll.programId = prgm.id;
        enroll.startDate = data.appDate;
        enroll.endDate = null;
        enroll.dismissalReasonId = 1; // Completed.
        enroll.insert(sqlConn);

        return enroll;
    }

    public static void updateProgram(Connection sqlConn, EtoAuthentication auth,
                                     long clientId, long participantId,
                                     SfProgramInfo data, String appStatus) throws Exception {
        JSONObject input = new JSONObject();

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to update");
            return;
        }

        // Find matching CIE program using ETO's program id.
        DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (prgm == null) {
            log.error("CIE Program not found while trying to update");
            return;
        }

        // Check to see if enrollment for this program already existed.  If not,
        // add new program.
        DbEnrollment enroll = DbEnrollment.findByProgram(sqlConn, clientId, prgm.id, data.appDate);
        if (enroll == null) { // New program enrollment.
            log.info("Program enrollment not found while trying to update program");

            // Add new program before updating.
            enroll = addProgram(sqlConn, auth, clientId, participantId, data, appStatus);
            if (enroll == null) {
                return;
            }
        }

        // Check to see if existing enrollment has been completed.
        if (enroll.endDate != null) {
            log.info("Program enrollment (id: " + programId + ") has been completed");
            return;
        }

        // Update program enrollment.
        log.info("Updating existing enrollment id: " + enroll.id);
        input.put("participantID", participantId);
        input.put("programID", new Integer(programId));
        long prgmEndDate = data.lastModified.getTime();
        input.put("endDate", "/Date(" + prgmEndDate + ")/");
        input.put("graduated", new Boolean(true));

        // Dismissal reason.
        String reason = "";
        if (appStatus.equalsIgnoreCase("pending") &&
            data.docSubmitted != null && data.docSubmitted.length() > 0) {
            String str = data.docSubmitted.toLowerCase();
            if (str.contains("complete")) {
                reason = "Submitted with complete documentation";
                input.put("reasonID", new Integer(426));
            }
            else if (str.contains("partial")) {
                reason = "Submitted with partial documentation";
                input.put("reasonID", new Integer(444));
            }
            else if (str.contains("without")) {
                reason = "Submitted without documentation";
                input.put("reasonID", new Integer(462));
            }
        }
        else if (appStatus.equalsIgnoreCase("denied") &&
                 data.denialReason != null && data.denialReason.length() > 0) {
            String str = data.denialReason.toLowerCase();
            if (str.contains("failure to provide")) {
                reason = "Failure to provide documents (FTP)";
                input.put("reasonID", new Integer(480));
            }
            else if (str.contains("missed interview")) {
                reason = "Missed county interview";
                input.put("reasonID", new Integer(498));
            }
            else if (str.contains("over income")) {
                reason = "Over income";
                input.put("reasonID", new Integer(516));
            }
            else if (str.contains("had calfresh")) {
                reason = "Already had CalFresh";
                input.put("reasonID", new Integer(534));
            }
            else if (str.contains("drug felony")) {
                reason = "Drug felony";
                input.put("reasonID", new Integer(552));
            }
            else if (str.contains("fleeing")) {
                reason = "Fleeing felon";
                input.put("reasonID", new Integer(570));
            }
            else if (str.contains("withdrawn")) {
                reason = "Withdrawn/Cancelled application";
                input.put("reasonID", new Integer(588));
            }
            else if (str.contains("ssi")) {
                reason = "Receiving SSI";
                input.put("reasonID", new Integer(606));
            }
            else if (str.contains("ineligible")) {
                reason = "Receiving another prgm that makes them ineligible";
                input.put("reasonID", new Integer(624));
            }
            else if (str.contains("multiple")) {
                reason = "Multiple households";
                input.put("reasonID", new Integer(642));
            }
            else if (str.contains("program violation")) {
                reason = "Intentional program violation (IPV)";
                input.put("reasonID", new Integer(660));
            }
            else if (str.contains("student")) {
                reason = "Student";
                input.put("reasonID", new Integer(678));
            }
            else if (str.contains("not a resident")) {
                reason = "Not a resident of county";
                input.put("reasonID", new Integer(696));
            }
            else if (str.contains("in progress")) {
                reason = "Application already in progress";
                input.put("reasonID", new Integer(714));
            }
            else if (str.contains("quit job")) {
                reason = "Voluntarily quit job";
                input.put("reasonID", new Integer(732));
            }
        }
        else if (appStatus.equalsIgnoreCase("approved")) {
            reason = "Completed, closed";
            input.put("reasonID", new Integer(426));
        }

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.deleteRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                               auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
            return;
        }

        // Update enrollment info.
        log.info("DbEnrollment.update()");
        enroll.endDate = data.lastModified;
        enroll.dismissalReasonOther = reason;
        enroll.dismissalReasonId = 1; // Completed.
        enroll.update(sqlConn);
    }


    public static void upsertHealthNavProgram(Connection sqlConn, EtoAuthentication auth,
                                              long clientId, long participantId,
                                              SfProgramInfo data) throws Exception {
        JSONObject input = new JSONObject();

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to upsert Health-Nav program");
            return;
        }

        // Find matching CIE program using ETO's program id.
        DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (prgm == null) {
            log.error("CIE Program not found while trying to upsert Health-Nav program");
            return;
        }

        // Check to see if enrollment for this program already existed.  If not,
        // add new program.
        DbEnrollment enroll = DbEnrollment.findByProgram(sqlConn, clientId, prgm.id, data.appDate);
        if (enroll == null) { // New program enrollment.
            log.info("Program enrollment not found while trying to upsert Health-Nav program");

            // Add new program before updating.
            enroll = addProgram(sqlConn, auth, clientId, participantId, data, data.appStatus);
            if (enroll == null) {
                return;
            }
        }

        // Check to see if existing enrollment has been closed.
        if (enroll.endDate != null) {
            log.info("Program enrollment (id: " + programId + ") has been closed");
            return;
        }

        // Update program enrollment only if application status is closed.
        if (data.appStatus != null && data.appStatus.length() > 0 &&
            data.appStatus.equalsIgnoreCase("closed")) {
            log.info("Updating existing enrollment id: " + enroll.id);
            input.put("participantID", participantId);
            input.put("programID", new Integer(programId));
            long prgmEndDate = data.lastModified.getTime();
            input.put("endDate", "/Date(" + prgmEndDate + ")/");
            input.put("graduated", new Boolean(true));

            // @debug.
            log.info(input.toString() + "\n");

            // Post request.
            ClientResponse response = EtoServiceUtil.deleteRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                                   auth, input.toString());
            if (response.getStatus() != 200) {
                log.error(response.toString());
                return;
            }

            // Update enrollment info.
            log.info("DbEnrollment.update()");
            enroll.endDate = data.lastModified;
            enroll.dismissalReasonOther = "Program name: " + data.appType +
                                          ", status: completed, closed";
            enroll.dismissalReasonId = 1; // Completed.
            enroll.update(sqlConn);
        }
    }

    public static void addCaseManager(Connection sqlConn, EtoAuthentication auth,
                                      long clientId, Long subjectId,
                                      SfProgramInfo data) throws Exception {
        log.info("Adding generic case manager, client id: " + clientId +
                 ", application: " + data.appType);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add case manager");
            return;
        }

        // Find generic case manager info.
        DbGenericCaseManager caseMgr = DbGenericCaseManager.findByProgram(sqlConn, programId);
        if (caseMgr == null) {
            log.error("Generic case manager for program id " + programId + " has not been configured");
            return;
        }

        // Check to see if generic case manager has been added for given program.
        String providerName = caseMgr.firstName + " " + caseMgr.lastName;
        DbCareProvider cp = DbCareProvider.findByClientProviderName(sqlConn, clientId,
                                                                    providerName);
        if (cp != null) {
            log.info("Generic case manager alredy existed for client id: " + clientId);
            return;
        }

        // Find ETO entity info to populate case manager TouchPoint.
        DbEtoEntity entity = DbEtoEntity.findByName(sqlConn, caseMgr.firstName, caseMgr.lastName);
        if (caseMgr == null) {
            log.error("ETO entity info has not been configured for case manager " +
                      caseMgr.firstName + " " + caseMgr.lastName);
            return;
        }

        // ETO Case Manager or Contact TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(17));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        // Case manager name.
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(168));
        ele.put("ElementType", new Integer(30));
        ele.put("Value", new Long(entity.entityId)); // ETO Entity ID.
        respElements.add(ele);

        // Case management end date.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(169));
        ele.put("ElementType", new Integer(9));
        ele.put("Value", null);
        respElements.add(ele);

        // Source system identifier.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(308));
        ele.put("ElementType", new Integer(5));
        ele.put("Value", "211 San Diego");
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long caseMgrRespId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                              "TouchPointResponseID");
            log.info("Case manager response ID: " + caseMgrRespId + "\n");
        }

        // Insert new CIE care provider record.
        log.info("Inserting CIE care provider, client id: " + clientId);

        // Find matching CIE program ID.
        DbProgram ciePrgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (ciePrgm == null) {
            log.error("CIE program info not found while trying to insert CIE care provider");
            return;
        }

        cp = new DbCareProvider();
        cp.clientId = clientId;
        cp.programId = ciePrgm.id;
        cp.name = providerName;
        cp.phone = caseMgr.phone;
        cp.email = caseMgr.email;
        cp.startDate = data.appDate;
        cp.insert(sqlConn);
    }

    public static void addSupplemental(Connection sqlConn, EtoAuthentication auth,
                                       long clientId, Long subjectId,
                                       SfProgramInfo data) throws Exception {
        log.info("Adding supplemental demographics, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add supplemental demographics");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO Supplemental Demographics TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(49));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Number in hoursehold / living status.
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(690));
        ele.put("ElementType", new Integer(6));
        if (data.householdSize <= 0) {
            ele.put("Value", null);
        }
        else {
            ele.put("Value", new Integer(data.householdSize));
        }
        respElements.add(ele);

        // Housing status.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(707));
        ele.put("ElementType", new Integer(5));
        if (data.householdSize == 1) {
            ele.put("Value", "Lives Alone");
        }
        else if (data.householdSize > 1) {
            ele.put("Value", "Lives With Others");
        }
        respElements.add(ele);

        // Monthly income.
        respElements.add(createTextRespElement(698, data.monthlyIncome));

        // Income source.
        respElements.add(createTextRespElement(699, data.incomeSource));

        // Primary language.
        respElements.add(createTextRespElement(688, data.language));

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long supDemoRespId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                              "TouchPointResponseID");
            log.info("Supplemental demographics response ID: " + supDemoRespId + "\n");
        }

        // Add/update CIE client supplemental demographics.
        log.info("Add/Update CIE client supplemental demographics");
        DbSupplemental sup = DbSupplemental.findByClient(sqlConn, clientId);
        if (sup != null) {
            sup.householdSize = data.householdSize;
            sup.monthlyIncome = data.monthlyIncome;
            sup.incomeSource = data.incomeSource;
            sup.language = data.language;
            sup.update(sqlConn);
        }
        else {
            sup = new DbSupplemental();
            sup.clientId = clientId;
            sup.householdSize = data.householdSize;
            sup.monthlyIncome = data.monthlyIncome;
            sup.incomeSource = data.incomeSource;
            sup.language = data.language;
            sup.insert(sqlConn);
        }
    }

    public static void addInsuranceProvider(Connection sqlConn, EtoAuthentication auth,
                                            long clientId, Long subjectId,
                                            SfProgramInfo data) throws Exception {
        // Check to see if the same insurance provider is existed.
        DbInsurance ins = DbInsurance.findByClientProvider(sqlConn, clientId,
                                                           data.insuranceProvider);
        if (ins != null) {
            log.info("Insurance provider " + data.insuranceProvider + " is already added for client id: " + clientId);
            return;
        }

        log.info("Adding insurance provider, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add insurance provider");
            return;
        }

        // ETO Insurance Provider TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(44));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Insurance name.
        respElements.add(createTextRespElement(651, data.insuranceProvider));

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long respId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                       "TouchPointResponseID");
            log.info("Insurance provider response ID: " + respId + "\n");
        }

        // Add CIE insurance provider.
        log.info("Add CIE insurance provider");
        ins = new DbInsurance();
        ins.clientId = clientId;
        ins.coverageTypeId = 7; // Unknown.
        ins.name = data.insuranceProvider;
        ins.rank = 1;
        ins.insert(sqlConn);
    }

    public static void addGeneralHealth(Connection sqlConn, EtoAuthentication auth,
                                        long clientId, Long subjectId,
                                        SfProgramInfo data) throws Exception {
        log.info("Adding general health, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add general health");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO General Health TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(43));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Question (element) response choices.
        JSONArray respElementChoices = new JSONArray();
        JSONObject choice = new JSONObject();

        // Medical diagnosis.
        respElements.add(createTextRespElement(624, data.primaryDiagnosis));

        // PCP/Medical home?
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(628));
        ele.put("ElementType", new Integer(4));
        if (data.primaryCareProvider != null &&
            data.primaryCareProvider.equalsIgnoreCase("yes")) {
            choice.put("TouchPointElementChoiceID", new Integer(1113));
        }
        else {
            choice.put("TouchPointElementChoiceID", new Integer(1114));
        }
        respElementChoices.add(choice);
        ele.put("ResponseElementChoices", respElementChoices);
        respElements.add(ele);

        // Hospitalization last 6 months.
        if (data.timesHospital != null && data.timesHospital.trim().length() > 0) {
            ele = new JSONObject();
            ele.put("ElementID", new Integer(635));
            ele.put("ElementType", new Integer(6));
            if (data.timesHospital.contains("1")) {
                ele.put("Value", new Integer(1));
            }
            else if (data.timesHospital.contains("2")) {
                ele.put("Value", new Integer(2));
            }
            else { // 3 or more.
                ele.put("Value", new Integer(3));
            }
            respElements.add(ele);
        }

        // Fallen last 6 months.
        if (data.timesFallen != null && data.timesFallen.trim().length() > 0) {
            ele = new JSONObject();
            ele.put("ElementID", new Integer(634));
            ele.put("ElementType", new Integer(6));
            if (data.timesFallen.contains("1")) {
                ele.put("Value", new Integer(1));
            }
            else if (data.timesFallen.contains("2")) {
                ele.put("Value", new Integer(2));
            }
            else { // 3 or more.
                ele.put("Value", new Integer(3));
            }
            respElements.add(ele);
        }

        // Readmitted to hospital.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(637));
        ele.put("ElementType", new Integer(4));
        choice = new JSONObject();
        if (data.readmitted != null && data.readmitted.trim().length() > 0) {
            choice.put("TouchPointElementChoiceID", new Integer(1117));
        }
        else {
            choice.put("TouchPointElementChoiceID", new Integer(1118));
        }
        respElementChoices = new JSONArray();
        respElementChoices.add(choice);
        ele.put("ResponseElementChoices", respElementChoices);
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long respId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                       "TouchPointResponseID");
            log.info("General health response ID: " + respId + "\n");
        }

        // Add CIE general health.
        log.info("Add CIE general health assessment");

        // Get the risk rating scale assessment.  Order IMPORTANT!
        List<DbQuestion> questions = DbQuestion.findByAssessment(sqlConn,
                                                                 "211 San Diego General Health Assessment");
        if (questions == null || questions.size() <= 0) {
            log.error("General health assessment has not been configured");
            return;
        }

        // PCP/Medical Home.
        DbQuestion q = questions.get(0);
        DbAnswer ans = new DbAnswer(clientId, q.id);
        if (data.primaryCareProvider != null &&
            data.primaryCareProvider.equalsIgnoreCase("yes")) {
            ans.answerYN = true;
        }
        else {
            ans.answerYN = false;
        }
        ans.insert(sqlConn);

        // Times hospitalization last 6 months.
        q = questions.get(1);
        ans = new DbAnswer(clientId, q.id);
        ans.answerText = data.timesHospital;
        ans.insert(sqlConn);

        // Times fallen last 6 months.
        q = questions.get(2);
        ans = new DbAnswer(clientId, q.id);
        ans.answerText = data.timesFallen;
        ans.insert(sqlConn);

        // Re-admitted to hospital.
        q = questions.get(3);
        ans = new DbAnswer(clientId, q.id);
        ans.answerText = data.readmitted;
        ans.insert(sqlConn);

        // Medical diagnosis
        q = questions.get(4);
        ans = new DbAnswer(clientId, q.id);
        ans.answerText = data.primaryDiagnosis;
        ans.insert(sqlConn);

        // Save client assessment for this section.
        DbClientAssessmentSection cas = new DbClientAssessmentSection();
        cas.clientId = clientId;
        cas.assessmentSectionId = q.assessmentSectionId;
        cas.dateTaken = data.appDate;
        cas.insert(sqlConn);
    }

    public static void addADLAssessment(Connection sqlConn, EtoAuthentication auth,
                                        long clientId, Long subjectId,
                                        SfProgramInfo data) throws Exception {
        log.info("Adding ADL assessment, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add general health");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO ADL/IADL Assessment TouchPoint.
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(33));
        input.put("SubjectID", new Long(subjectId));
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        int[] choices = new int[] {977, 978, 979, 980, 981}; // Transferring.
        respElements.add(createAdlRespElement(477, data.mobility, choices));

        choices = new int[] {982, 983, 984, 985, 986}; // Bathing.
        respElements.add(createAdlRespElement(478, data.safety, choices));

        choices = new int[] {997, 998, 999, 1000, 1001}; // Walking.
        respElements.add(createAdlRespElement(481, data.mobility, choices));

        choices = new int[] {1002, 1003, 1004, 1005, 1006}; // Light housework.
        respElements.add(createAdlRespElement(482, data.housework, choices));

        choices = new int[] {1007, 1008, 1009, 1010, 1011}; // Laundry.
        respElements.add(createAdlRespElement(483, data.housework, choices));

        choices = new int[] {1017, 1018, 1019, 1020, 1021}; // Meal prep, cleanup.
        respElements.add(createAdlRespElement(485, data.mealPrep, choices));

        choices = new int[] {1022, 1023, 1024, 1025, 1026}; // Transportation.
        respElements.add(createAdlRespElement(486, data.safety, choices));

        choices = new int[] {1032, 1033, 1034, 1035, 1036}; // Manage medications.
        respElements.add(createAdlRespElement(488, data.healthcareAccess, choices));

        choices = new int[] {1037, 1038, 1039, 1040, 1041}; // Manage money.
        respElements.add(createAdlRespElement(489, data.moneyMgmt, choices));

        choices = new int[] {1042, 1043, 1044, 1045, 1046}; // Stair climbing.
        respElements.add(createAdlRespElement(490, data.safety, choices));

        choices = new int[] {1057, 1058, 1059, 1060, 1061}; // Heavy housework.
        respElements.add(createAdlRespElement(493, data.homeRepair, choices));

        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(496)); // Date taken.
        ele.put("ElementType", new Integer(9));
        if (data.appDate != null) {
            ele.put("Value", sdf.format(data.appDate));
        }
        else {
            ele.put("Value", null);
        }
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long adlRespId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                          "TouchPointResponseID");
            log.info("ADL/IADL assessment response ID: " + adlRespId + "\n");
        }

        // Add/update CIE ADL assessment.
        log.info("Add/Update CIE ADL assessment\n");

        // Note that it is very important to configure the number of questions
        // in CIE database to match the number of questions in ETO TouchPoint.
        int totalScore = 0;
        List<DbQuestion> questions = DbQuestion.findByAssessment(sqlConn,
                                                                 "211 Health Navigation ADL Assessment");
        if (questions != null && questions.size() > 0) {
            DbQuestion q = questions.get(0); // Bathing.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.safety);

            q = questions.get(1); // Stair climbing.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.safety);

            q = questions.get(7); // Transferring.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.mobility);

            q = questions.get(8); // Walking.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.mobility);

            // Save client assessment for this section.
            DbClientAssessmentSection cas = new DbClientAssessmentSection();
            cas.clientId = clientId;
            cas.assessmentSectionId = q.assessmentSectionId;
            cas.dateTaken = data.appDate;
            cas.totalScore = totalScore;
            cas.insert(sqlConn);
        }
        else {
            log.info("211 Health Navigation ADL assessment has not been configured in CIE database");
        }

        // Add/update CIE ADL assessment.
        log.info("Add/Update CIE IADL assessment\n");

        // Note that it is very important to configure the number of questions
        // in CIE database to match the number of questions in ETO TouchPoint.
        totalScore = 0;
        questions = DbQuestion.findByAssessment(sqlConn, "211 Health Navigation IADL Assessment");
        if (questions != null && questions.size() > 0) {
            DbQuestion q = questions.get(0); // Heavy housework.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.homeRepair);

            q = questions.get(1); // Laundry.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.housework);

            q = questions.get(2); // Light house work.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.housework);

            q = questions.get(3); // Manage medications.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.healthcareAccess);

            q = questions.get(4); // Meal prep/cleanup.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.mealPrep);

            q = questions.get(6); // Manage money.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.moneyMgmt);

            q = questions.get(9); // Transportation.
            totalScore += saveAdlAnswer(sqlConn, clientId, q, data.safety);

            // Save client assessment for this section.
            DbClientAssessmentSection cas = new DbClientAssessmentSection();
            cas.clientId = clientId;
            cas.assessmentSectionId = q.assessmentSectionId;
            cas.dateTaken = data.appDate;
            cas.totalScore = totalScore;
            cas.insert(sqlConn);
        }
        else {
            log.info("211 Health Navigation IADL assessment has not been configured in CIE database");
        }
    }

    public static void addRiskRatingScale(Connection sqlConn, EtoAuthentication auth,
                                          long clientId, Long subjectId,
                                          SfProgramInfo data) throws Exception {
        log.info("Adding risk rating scale, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add risk rating scale");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO Risk Rating Scale TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(53));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Current living situation, housing.
        JSONObject ele = createRiskRatingRespElement(803, 1236, data.housing);
        if (ele != null) {
            respElements.add(ele);
        }

        // Nutrition, healthly food.
        ele = createRiskRatingRespElement(804, 1241, data.nutrition);
        if (ele != null) {
            respElements.add(ele);
        }

        // Primary care provider, medical home.
        ele = createRiskRatingRespElement(805, 1246, data.primaryCare);
        if (ele != null) {
            respElements.add(ele);
        }

        // Health, medication management.
        ele = createRiskRatingRespElement(806, 1251, data.medication);
        if (ele != null) {
            respElements.add(ele);
        }

        // Social support.
        ele = createRiskRatingRespElement(807, 1256, data.socialSupport);
        if (ele != null) {
            respElements.add(ele);
        }

        // Activities of daily living.
        ele = createRiskRatingRespElement(808, 1261, data.dailyLiving);
        if (ele != null) {
            respElements.add(ele);
        }

        // Ambulance, hospitalization.
        ele = createRiskRatingRespElement(809, 1266, data.ambulance);
        if (ele != null) {
            respElements.add(ele);
        }

        // Income source, employment.
        ele = createRiskRatingRespElement(810, 1271, data.incomeEmployment);
        if (ele != null) {
            respElements.add(ele);
        }

        // Transportation.
        ele = createRiskRatingRespElement(811, 1276, data.transportation);
        if (ele != null) {
            respElements.add(ele);
        }

        // Date taken.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(812));
        ele.put("ElementType", new Integer(9));
        if (data.appDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            ele.put("Value", sdf.format(data.appDate));
        }
        else {
            ele.put("Value", null);
        }
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long rrsId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                      "TouchPointResponseID");
            log.info("Risk rating scale response ID: " + rrsId + "\n");
        }

        // Add CIE risk rating scale.
        log.info("Add CIE risk rating scale assessment");

        // Get the risk rating scale assessment.  Order IMPORTANT!
        List<DbQuestion> questions = DbQuestion.findByAssessment(sqlConn,
                                                                 "211 San Diego Risk Rating Scale Assessment");
        if (questions == null || questions.size() <= 0) {
            log.error("Risk rating scale assessment has not been configured");
            return;
        }

        // Activities of daily living.
        DbQuestion q = questions.get(0);
        saveRRSAnswer(sqlConn, clientId, q, data.dailyLiving);

        // Ambulance, hospitalization.
        q = questions.get(1);
        saveRRSAnswer(sqlConn, clientId, q, data.ambulance);

        // Health, medication management.
        q = questions.get(2);
        saveRRSAnswer(sqlConn, clientId, q, data.medication);

        // Current living situation, housing.
        q = questions.get(3);
        saveRRSAnswer(sqlConn, clientId, q, data.housing);

        // Income source, employment.
        q = questions.get(4);
        saveRRSAnswer(sqlConn, clientId, q, data.incomeEmployment);

        // Nutrition, healthly food.
        q = questions.get(5);
        saveRRSAnswer(sqlConn, clientId, q, data.nutrition);

        // Primary care provider, medical home.
        q = questions.get(6);
        saveRRSAnswer(sqlConn, clientId, q, data.primaryCare);

        // Social support.
        q = questions.get(7);
        saveRRSAnswer(sqlConn, clientId, q, data.socialSupport);

        // Transportation.
        q = questions.get(8);
        saveRRSAnswer(sqlConn, clientId, q, data.transportation);

        // Save client assessment for this section.
        DbClientAssessmentSection cas = new DbClientAssessmentSection();
        cas.clientId = clientId;
        cas.assessmentSectionId = q.assessmentSectionId;
        cas.dateTaken = data.appDate;
        cas.insert(sqlConn);
    }

    private static JSONObject createTextRespElement(int elementId, String respText) {
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(elementId));
        ele.put("ElementType", new Integer(5)); // Free text answer.
        ele.put("Value", respText);
        return ele;
    }

    private static JSONObject createAdlRespElement(int elementId, String respText, int[] choices) {
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(elementId));
        ele.put("ElementType", new Integer(4));
        if (respText != null && respText.length() > 0) {
            JSONArray respElementChoices = setAdlResponse(respText, choices);
            ele.put("ResponseElementChoices", respElementChoices);
        }
        else {
            ele.put("ResponseElementChoices", null);
        }
        return ele;
    }

    private static JSONArray setAdlResponse(String funcLevel, int[] choices) {
        if (choices.length < 5) {
            log.error("Invalid ADL/IADL choices");
            return null;
        }

        JSONArray respElementChoices = new JSONArray();
        JSONObject choice = new JSONObject();

        if (funcLevel == null || funcLevel.length() <= 0) {
            choice.put("TouchPointElementChoiceID", null);
            respElementChoices.add(choice);
            return respElementChoices;
        }

        String str = funcLevel.toLowerCase();
        if (str.contains("independent")) {
            choice.put("TouchPointElementChoiceID", new Integer(choices[4]));
        }
        else if (str.contains("verbal")) {
            choice.put("TouchPointElementChoiceID", new Integer(choices[3]));
        }
        else if (str.contains("stand by")) {
            choice.put("TouchPointElementChoiceID", new Integer(choices[2]));
        }
        else if (str.contains("hands on")) {
            choice.put("TouchPointElementChoiceID", new Integer(choices[1]));
        }
        else if (str.contains("dependent")) {
            choice.put("TouchPointElementChoiceID", new Integer(choices[0]));
        }
        else {
            choice.put("TouchPointElementChoiceID", null);
        }
        respElementChoices.add(choice);
        return respElementChoices;
    }

    private static JSONObject createRiskRatingRespElement(int elementId,
                                                          int thrivingChoiceId,
                                                          String respText) {
        JSONObject ele = null;
        if (respText == null || respText.trim().length() <= 0) {
            return ele;
        }

        int riskRating = 0;
        String str = respText.toLowerCase();
        switch (str) {
            case "crisis":
                riskRating = 5;
                break;
            case "vulnerable":
                riskRating = 4;
                break;
            case "stable":
                riskRating = 3;
                break;
            case "safe":
                riskRating = 2;
                break;
            case "thriving":
                riskRating = 1;
                break;
            default:
                break;
        }

        if (riskRating > 0) {
            int choiceId = thrivingChoiceId - riskRating + 1;
            JSONObject choice = new JSONObject();
            choice.put("TouchPointElementChoiceID", new Integer(choiceId));
            JSONArray respElementChoices = new JSONArray();
            respElementChoices.add(choice);

            ele = new JSONObject();
            ele.put("ElementID", new Integer(elementId));
            ele.put("ElementType", new Integer(4)); // Pick list answer.
            ele.put("ResponseElementChoices", respElementChoices);
        }
        return ele;
    }

    private static void saveRRSAnswer(Connection sqlConn, long clientId,
                                      DbQuestion q, String scaleLevel) {
        // Make sure risk rating scale is not null.
        if (scaleLevel == null || scaleLevel.trim().length() <= 0) {
            return;
        }

        DbAnswer ans = new DbAnswer(clientId, q.id);
        if (scaleLevel.equalsIgnoreCase("crisis")) {
            ans.optionChoiceId = 30; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (scaleLevel.equalsIgnoreCase("vulnerable")) {
            ans.optionChoiceId = 31; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (scaleLevel.equalsIgnoreCase("stable")) {
            ans.optionChoiceId = 32; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (scaleLevel.equalsIgnoreCase("safe")) {
            ans.optionChoiceId = 33; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (scaleLevel.equalsIgnoreCase("thriving")) {
            ans.optionChoiceId = 34; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (scaleLevel.equalsIgnoreCase("critical")) {
            ans.optionChoiceId = 35; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
    }

    private static int saveAdlAnswer(Connection sqlConn, long clientId,
                                     DbQuestion q, String funcLevel) {
        int score = 0;

        // Make sure functioning scale is not null.
        if (funcLevel == null || funcLevel.trim().length() <= 0) {
            return score;
        }

        String func = funcLevel.toLowerCase();
        DbAnswer ans = new DbAnswer(clientId, q.id);
        if (func.contains("independent")) {
            ans.optionChoiceId = 3; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (func.contains("verbal")) {
            score = 1;
            ans.optionChoiceId = 4; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (func.contains("stand by")) {
            score = 1;
            ans.optionChoiceId = 5; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (func.contains("hands on")) {
            score = 1;
            ans.optionChoiceId = 6; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        else if (func.contains("dependent")) {
            score = 1;
            ans.optionChoiceId = 7; // Must match id in option_choice table.
            ans.insert(sqlConn);
        }
        return score;
    }
}
