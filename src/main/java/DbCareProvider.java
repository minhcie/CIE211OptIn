package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbCareProvider {
    private static final Logger log = Logger.getLogger(DbCareProvider.class.getName());

    public long id;
    public long clientId;
    public long programId;
    public long providerRoleId = 1; // Case Manager, must match id from provider_role table.
    public String name;
    public String phone;
    public String email;
    public String agencyName = "211 San Diego";
    public int contactPriority = 1;
    public Date startDate;


    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO care_provider (clientId, programId, providerRoleId, ");
            sb.append("name, phone1, email, agencyName, contactPriority, careProvisionStartDate) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.programId);
            ps.setLong(3, this.providerRoleId);
            ps.setString(4, this.name);
            ps.setString(5, this.phone);
            ps.setString(6, this.email);
            ps.setString(7, this.agencyName);
            ps.setInt(8, this.contactPriority);

            if (this.startDate != null) {
                java.sql.Date sqlDate = new java.sql.Date(this.startDate.getTime());
                ps.setDate(9, sqlDate);
            }
            else {
                ps.setDate(9, null);
            }

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert care provider record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbCareProvider.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbCareProvider.insert(): " + e);
             e.printStackTrace();
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE care_provider SET clientId = ?, programId = ?, ");
            sb.append("providerRoleId = ?, name = ?, phone1 = ?, email = ?, ");
            sb.append("agencyName = ?, contactPriority = ?, careProvisionStartDate = ? ");
            sb.append("WHERE id = ?");

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.programId);
            ps.setLong(3, this.providerRoleId);
            ps.setString(4, this.name);
            ps.setString(5, this.phone);
            ps.setString(6, this.email);
            ps.setString(7, this.agencyName);
            ps.setInt(8, this.contactPriority);

            if (this.startDate != null) {
                java.sql.Date sqlDate = new java.sql.Date(this.startDate.getTime());
                ps.setDate(9, sqlDate);
            }
            else {
                ps.setDate(9, null);
            }
            ps.setLong(10, this.id);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update care provider record!");
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbCareProvider.update(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbCareProvider.update(): " + e);
             e.printStackTrace();
        }
    }

    public static DbCareProvider findByClientProviderName(Connection conn, long clientId,
                                                          String name) {
        DbCareProvider result = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, programId, providerRoleId, name, phone1, ");
            sb.append("email, agencyName, contactPriority, careProvisionStartDate ");
            sb.append("FROM care_provider ");
            sb.append("WHERE clientId = " + clientId);
            sb.append("  AND name = '" + SqlString.encode(name) + "' ");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbCareProvider();
                result.id = rs.getLong("id");
                result.clientId = rs.getLong("clientId");
                result.programId = rs.getLong("programId");
                result.providerRoleId = rs.getLong("providerRoleId");
                result.name = rs.getString("name");
                result.phone = rs.getString("phone1");
                result.email = rs.getString("email");
                result.agencyName = rs.getString("agencyName");
                result.contactPriority = rs.getInt("contactPriority");
                result.startDate = rs.getDate("careProvisionStartDate");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbCareProvider.findByClientProviderName(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbCareProvider.findByClientProviderName(): " + e);
        }
        return result;
    }
}
