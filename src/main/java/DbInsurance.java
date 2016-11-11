package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbInsurance {
    private static final Logger log = Logger.getLogger(DbInsurance.class.getName());

    public long id = 0;
    public long clientId;
    public long coverageTypeId;
    public String name;
    public int rank = 0;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO insurance (clientId, coverageTypeId, name, ");
            sb.append("rank, created) ");
            sb.append("VALUES (?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.coverageTypeId);
            ps.setString(3, this.name);
            ps.setInt(4, this.rank);
            Date now = new Date();
            java.sql.Date sqlDate = new java.sql.Date(now.getTime());
            ps.setDate(5, sqlDate);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert insurance record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbInsurance.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbInsurance.insert(): " + e);
             e.printStackTrace();
        }
    }

    public static DbInsurance findByClientProvider(Connection conn, long id, String name) {
        DbInsurance result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, coverageTypeId, name, rank ");
            sb.append("FROM insurance ");
            sb.append("WHERE clientId = " + id + " ");
            sb.append("  AND name = '" + name + "'");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbInsurance();
                result.id = rs.getLong("id");
                result.clientId = rs.getLong("clientId");
                result.coverageTypeId = rs.getLong("coverageTypeId");
                result.name = rs.getString("name");
                result.rank = rs.getInt("rank");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbInsurance.findByClientProvider(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbInsurance.findByClientProvider(): " + e);
        }
        return result;
    }
}
