package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbClientRace {
    private static final Logger log = Logger.getLogger(DbClientRace.class.getName());

    public long id = 0;
    public long clientId;
    public long raceId;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client_race (clientId, raceId) ");
            sb.append("VALUES (?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.raceId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client race record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbClientRace.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbClientRace.insert(): " + e);
            e.printStackTrace();
        }
    }

    public static DbClientRace findByClientRace(Connection conn, long clientId, long raceId) {
        DbClientRace client = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, raceId ");
            sb.append("FROM client_race ");
            sb.append("WHERE clientId = " + clientId + " AND raceId = " + raceId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                client = new DbClientRace();
                client.id = rs.getLong("id");
                client.clientId = rs.getLong("clientId");
                client.raceId = rs.getLong("raceId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbClientRace.findByClientRace(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbClientRace.findByClientRace(): " + e);

        }
        return client;
    }
}
