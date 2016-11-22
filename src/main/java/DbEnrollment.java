package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbEnrollment {
    private static final Logger log = Logger.getLogger(DbEnrollment.class.getName());

    public long id = 0;
    public long clientId;
    public long programId;
    public Date startDate;
    public Date endDate;
    public long dismissalReasonId;
    public String dismissalReasonOther;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO enrollment (clientId, programId, startDate, ");
            sb.append("endDate, dismissalReasonId, dismissalReasonOther) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.programId);
            java.sql.Date sqlDate = new java.sql.Date(this.startDate.getTime());
            ps.setDate(3, sqlDate);
            if (this.endDate != null) {
                sqlDate = new java.sql.Date(this.endDate.getTime());
                ps.setDate(4, sqlDate);
                ps.setLong(5, this.dismissalReasonId);
                ps.setString(6, SqlString.encode(this.dismissalReasonOther));
            }
            else {
                ps.setDate(4, null);
                ps.setLong(5, 0);
                ps.setString(6, null);
            }

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert enrollment record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEnrollment.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEnrollment.insert(): " + e);
             e.printStackTrace();
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE enrollment SET programId = ?, startDate = ?, ");
            sb.append("endDate = ?, dismissalReasonId = ?, dismissalReasonOther = ? ");
            sb.append("WHERE id = ?");

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setLong(1, this.programId);
            java.sql.Date sqlDate = new java.sql.Date(this.startDate.getTime());
            ps.setDate(2, sqlDate);
            sqlDate = new java.sql.Date(this.endDate.getTime());
            if (this.endDate != null) {
                sqlDate = new java.sql.Date(this.endDate.getTime());
                ps.setDate(3, sqlDate);
                ps.setLong(4, this.dismissalReasonId);
                ps.setString(5, SqlString.encode(this.dismissalReasonOther));
            }
            else {
                ps.setDate(3, null);
                ps.setLong(4, 0);
                ps.setString(5, null);
            }
            ps.setLong(6, this.id);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update enrollment record!");
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEnrollment.update(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEnrollment.update(): " + e);
             e.printStackTrace();
        }
    }

    public static DbEnrollment findByProgram(Connection conn, long clientId,
                                             long programId, Date programStartDate) {
        DbEnrollment result = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, programId, startDate, endDate, ");
            sb.append("dismissalReasonId, dismissalReasonOther ");
            sb.append("FROM enrollment ");
            sb.append("WHERE clientId = " + clientId);
            sb.append("  AND programId = " + programId);
            sb.append("  AND startDate = '" + sdf.format(programStartDate) + "'");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbEnrollment();
                result.id = rs.getLong("id");
                result.clientId = rs.getLong("clientId");
                result.programId = rs.getLong("programId");
                result.startDate = rs.getDate("startDate");
                result.endDate = rs.getDate("endDate");
                result.dismissalReasonId = rs.getLong("dismissalReasonId");
                result.dismissalReasonOther = rs.getString("dismissalReasonOther");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEnrollment.findByProgram(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEnrollment.findByProgram(): " + e);
        }
        return result;
    }

    public static DbEnrollment findOpenEnrollment(Connection conn, long clientId) {
        DbEnrollment result = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, programId, startDate, endDate, ");
            sb.append("dismissalReasonId, dismissalReasonOther ");
            sb.append("FROM enrollment ");
            sb.append("WHERE clientId = " + clientId);
            sb.append("  AND endDate IS NULL ");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbEnrollment();
                result.id = rs.getLong("id");
                result.clientId = rs.getLong("clientId");
                result.programId = rs.getLong("programId");
                result.startDate = rs.getDate("startDate");
                result.endDate = rs.getDate("endDate");
                result.dismissalReasonId = rs.getLong("dismissalReasonId");
                result.dismissalReasonOther = rs.getString("dismissalReasonOther");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEnrollment.findByProgram(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEnrollment.findByProgram(): " + e);
        }
        return result;
    }
}
