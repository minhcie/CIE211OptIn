package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbSupplemental {
    private static final Logger log = Logger.getLogger(DbSupplemental.class.getName());

    public long id = 0;
    public long clientId;
    public int householdSize;
    public String monthlyIncome;
    public String incomeSource;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client_supplemental (clientId, numberInHousehold, ");
            sb.append("housingStatus, incomeAmount, incomeSource) ");
            sb.append("VALUES (?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setInt(2, this.householdSize);
            if (this.householdSize == 1) {
                ps.setString(3, "Lives Alone");
            }
            else if (this.householdSize > 1) {
                ps.setString(3, "Live With Others");
            }
            else {
                ps.setString(3, "");
            }
            ps.setString(4, SqlString.encode(this.monthlyIncome));
            ps.setString(5, SqlString.encode(this.incomeSource));

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client supplemental record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbSupplemental.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbSupplemental.insert(): " + e);
             e.printStackTrace();
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE client_supplemental SET numberInHousehold = ?, housingStatus = ?, ");
            sb.append("incomeAmount = ?, incomeSource = ?, modified = ? ");
            sb.append("WHERE id = ?");

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setInt(1, this.householdSize);
            if (this.householdSize == 1) {
                ps.setString(2, "Lives Alone");
            }
            else if (this.householdSize > 1) {
                ps.setString(2, "Live With Others");
            }
            else {
                ps.setString(2, "");
            }
            ps.setString(3, SqlString.encode(this.monthlyIncome));
            ps.setString(4, SqlString.encode(this.incomeSource));

            // last modified date time.
            Date now = new Date();
            java.sql.Date sqlDate = new java.sql.Date(now.getTime());
            ps.setDate(5, sqlDate);

            ps.setLong(6, this.id);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update client supplemental record!");
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbSupplemental.update(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbSupplemental.update(): " + e);
             e.printStackTrace();
        }
    }

    public static DbSupplemental findByClient(Connection conn, long clientId) {
        DbSupplemental result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, numberInHousehold, housingStatus, ");
            sb.append("incomeAmount, incomeSource ");
            sb.append("FROM client_supplemental ");
            sb.append("WHERE clientId = " + clientId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbSupplemental();
                result.id = rs.getLong("id");
                result.clientId = rs.getLong("clientId");
                result.householdSize = rs.getInt("numberInHousehold");
                result.monthlyIncome = rs.getString("incomeAmount");
                result.incomeSource = rs.getString("incomeSource");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbSupplemental.findByClient(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbSupplemental.findByClient(): " + e);
        }
        return result;
    }
}
