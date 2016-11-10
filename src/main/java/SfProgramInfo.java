package src.main.java;

import java.util.Date;

public class SfProgramInfo {
    public String id;
    public String appType;
    public Date appDate; // For risk rating scale, this is the date taken.
    public String appStatus;
    public String confirmationNumber;
    public Date bda;
    public String reasonIncomplete;
    public String denialReason;
    public String docSubmitted;
    public String notes;
    public String incomeSource;
    public String monthlyIncome;
    public int householdSize;
    public String language;
    public String careGiverName;
    public String careGiverPhone;
    public String insuranceProvider;
    public String primaryDiagnosis;
    public String primaryCareProvider;
    public String timesHospital;
    public String timesFallen;
    public String readmitted;
    public Date lastModified;

    // Risk rating scale.
    public String housing;
    public String nutrition;
    public String primaryCare;
    public String medication;
    public String socialSupport;
    public String dailyLiving;
    public String ambulance;
    public String incomeEmployment;
    public String transportation;
}
